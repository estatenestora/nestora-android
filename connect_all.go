package main

import (
	"bytes"
	"fmt"
	"net"
	"os/exec"
	"regexp"
	"strings"
	"sync"
	"time"
)

type DeviceInfo struct {
	ID          string
	IsIP        bool
	IsMDNS      bool
	IsEmulator  bool
	IsOffline   bool
	Model       string
	Product     string
	Device      string
	TransportID string
}

type MDNSEntry struct {
	Name string
	IP   string
	Port string
}

func main() {
	fmt.Println("🚀 Starting ADB Wireless Debugging Auto-Connect & Clean...")

	// Step 1: Disconnect all offline/stale handles (both IP and mDNS that are offline)
	fmt.Println("🧹 Cleaning up stale and offline ADB connections...")
	cleanupAllStale()

	// Step 2: Try connecting via mDNS services (primary - these are the real WD ports)
	fmt.Println("🔎 Checking mDNS advertised Wireless Debugging services...")
	mdnsConnected := connectViaMDNS()

	// Step 3: Get local interface IPs
	localIPs := getLocalIPs()

	// Step 4: Parse ARP table to find candidate IPs not already connected via mDNS
	fmt.Println("🔍 Scanning ARP table for remaining unconnected devices...")
	alreadyConnected := getConnectedIPs()
	candidateIPs := getCandidateIPs(localIPs, alreadyConnected)

	if len(candidateIPs) > 0 {
		fmt.Printf("📱 Scanning %d IP(s) for Wireless Debugging ports: %v\n", len(candidateIPs), candidateIPs)

		var wg sync.WaitGroup
		sem := make(chan struct{}, 3) // limit to 3 concurrent IP scans to prevent thread exhaustion

		for _, ip := range candidateIPs {
			wg.Add(1)
			sem <- struct{}{} // acquire token
			go func(targetIP string) {
				defer wg.Done()
				defer func() { <-sem }() // release token
				scanAndConnectIP(targetIP)
			}(ip)
		}
		wg.Wait()
	} else if !mdnsConnected {
		fmt.Println("⚠️  No unconnected candidate device IPs found.")
	}

	// Step 5: Deduplicate — one physical device = one ADB handle
	fmt.Println("\n🔄 Deduplicating device handles...")
	deduplicateDevices()

	// Step 6: Show final clean device list
	fmt.Println("\n✅ Done. Current ADB devices:")
	showDevices()
}

// cleanupAllStale disconnects ALL offline handles (IP and mDNS) and stale IP connections
func cleanupAllStale() {
	out, _ := execCmdOutput("adb", "devices", "-l")
	lines := strings.Split(out, "\n")

	for _, line := range lines {
		line = strings.TrimSpace(line)
		if line == "" || strings.HasPrefix(line, "List of") {
			continue
		}
		fields := strings.Fields(line)
		if len(fields) < 2 {
			continue
		}
		id := fields[0]
		status := fields[1]

		isEmulator := strings.HasPrefix(id, "emulator-")
		if isEmulator {
			continue // never disconnect emulators
		}

		// Disconnect if offline OR if it's a plain IP handle (will reconnect fresh)
		if status == "offline" || status == "unauthorized" {
			fmt.Printf("   🗑️  Disconnecting %s handle: %s\n", status, id)
			execCmd("adb", "disconnect", id)
		} else if strings.Contains(id, ":") && !strings.Contains(id, "_tcp") {
			// It's an IP:port handle that's currently device — we'll reconnect cleanly
			fmt.Printf("   🔄  Disconnecting active IP handle for fresh reconnect: %s\n", id)
			execCmd("adb", "disconnect", id)
		}
	}
}

// connectViaMDNS reads mDNS services and connects to advertised WD endpoints
func connectViaMDNS() bool {
	out, err := execCmdOutput("adb", "mdns", "services")
	if err != nil || strings.TrimSpace(out) == "" {
		return false
	}

	entries := parseMDNSServices(out)
	if len(entries) == 0 {
		fmt.Println("   ℹ️  No mDNS services found.")
		return false
	}

	anyConnected := false
	for _, entry := range entries {
		// Skip emulator internal IPs (10.0.2.x, 127.x.x.x)
		if strings.HasPrefix(entry.IP, "10.0.2.") || strings.HasPrefix(entry.IP, "127.") {
			fmt.Printf("   ⏭️  Skipping emulator mDNS entry: %s at %s:%s\n", entry.Name, entry.IP, entry.Port)
			continue
		}
		target := fmt.Sprintf("%s:%s", entry.IP, entry.Port)
		fmt.Printf("📡 mDNS found: %s at %s. Connecting ADB...\n", entry.Name, target)
		out, success := runADBConnect(target)
		if success {
			fmt.Printf("✅ Connected to %s!\nOutput: %s\n", target, strings.TrimSpace(out))
			anyConnected = true
		} else {
			outLower := strings.ToLower(out)
			if strings.Contains(outLower, "failed to connect") {
				fmt.Printf("🔐 %s:%s is TLS-only — device needs (re-)pairing.\n", entry.IP, entry.Port)
				fmt.Printf("   On phone: Settings → Developer Options → Wireless Debugging → Pair device with pairing code\n")
				fmt.Printf("   Then run: adb pair %s:<PAIRING_PORT>  (enter 6-digit code when prompted)\n", entry.IP)
			} else {
				fmt.Printf("⚠️  Connect failed on %s: %s\n", target, strings.TrimSpace(out))
			}
		}
	}
	return anyConnected
}

func parseMDNSServices(out string) []MDNSEntry {
	var entries []MDNSEntry
	lines := strings.Split(out, "\n")
	for _, line := range lines {
		line = strings.TrimSpace(line)
		if line == "" || strings.HasPrefix(line, "List of") {
			continue
		}
		// Format: <name>\t<service type>\t<ip>:<port>
		parts := strings.Fields(line)
		if len(parts) < 3 {
			continue
		}
		addr := parts[len(parts)-1] // last field is ip:port
		colonIdx := strings.LastIndex(addr, ":")
		if colonIdx < 0 {
			continue
		}
		ip := addr[:colonIdx]
		port := addr[colonIdx+1:]
		entries = append(entries, MDNSEntry{
			Name: parts[0],
			IP:   ip,
			Port: port,
		})
	}
	return entries
}

// getConnectedIPs returns a set of IPs that are already active in adb devices
func getConnectedIPs() map[string]bool {
	connected := make(map[string]bool)
	out, _ := execCmdOutput("adb", "devices", "-l")
	lines := strings.Split(out, "\n")
	ipPortRegex := regexp.MustCompile(`^(\d+\.\d+\.\d+\.\d+):\d+`)
	for _, line := range lines {
		line = strings.TrimSpace(line)
		m := ipPortRegex.FindStringSubmatch(line)
		if m != nil && !strings.Contains(line, "offline") {
			connected[m[1]] = true
		}
	}
	return connected
}

func getLocalIPs() map[string]bool {
	localIPs := make(map[string]bool)
	addrs, err := net.InterfaceAddrs()
	if err != nil {
		return localIPs
	}
	for _, addr := range addrs {
		if ipNet, ok := addr.(*net.IPNet); ok && !ipNet.IP.IsLoopback() {
			if ipNet.IP.To4() != nil {
				localIPs[ipNet.IP.String()] = true
			}
		}
	}
	return localIPs
}

func getCandidateIPs(localIPs map[string]bool, alreadyConnected map[string]bool) []string {
	out, err := execCmdOutput("arp", "-a")
	if err != nil {
		return nil
	}

	ipRegex := regexp.MustCompile(`\b(?:192\.168|10\.\d+|172\.(?:1[6-9]|2\d|3[01]))\.\d+\.\d+\b`)
	matches := ipRegex.FindAllString(out, -1)

	uniqueIPs := make(map[string]bool)
	var candidateIPs []string

	for _, ipStr := range matches {
		ip := net.ParseIP(ipStr)
		if ip == nil {
			continue
		}
		ip4 := ip.To4()
		if ip4 == nil {
			continue
		}

		// Skip broadcast, host, already connected, local interface IPs
		if ip4[3] == 255 || ip4[3] == 0 {
			continue
		}
		if localIPs[ipStr] || alreadyConnected[ipStr] {
			continue
		}
		if ip4[0] == 224 || ip4[0] == 239 {
			continue
		}

		if !uniqueIPs[ipStr] {
			uniqueIPs[ipStr] = true
			candidateIPs = append(candidateIPs, ipStr)
		}
	}
	return candidateIPs
}

func scanAndConnectIP(ip string) {
	fmt.Printf("⚡ Scanning ports (30000-47000) for IP %s...\n", ip)

	const minPort = 30000
	const maxPort = 47000
	const numWorkers = 300

	portsChan := make(chan int, 1000)
	resultsChan := make(chan int, 100)
	var workerWg sync.WaitGroup

	for i := 0; i < numWorkers; i++ {
		workerWg.Add(1)
		go func() {
			defer workerWg.Done()
			for port := range portsChan {
				target := fmt.Sprintf("%s:%d", ip, port)
				conn, err := net.DialTimeout("tcp", target, 50*time.Millisecond)
				if err == nil {
					conn.Close()
					resultsChan <- port
				}
			}
		}()
	}

	go func() {
		for port := minPort; port <= maxPort; port++ {
			portsChan <- port
		}
		close(portsChan)
	}()

	go func() {
		workerWg.Wait()
		close(resultsChan)
	}()

	var openPorts []int
	for p := range resultsChan {
		openPorts = append(openPorts, p)
	}

	if len(openPorts) == 0 {
		return
	}

	for _, port := range openPorts {
		target := fmt.Sprintf("%s:%d", ip, port)
		fmt.Printf("🎉 Found open TCP port %d on %s. Connecting ADB...\n", port, ip)
		out, success := runADBConnect(target)
		if success {
			fmt.Printf("✅ Connected to %s!\nOutput: %s\n", target, strings.TrimSpace(out))
		} else {
			fmt.Printf("❌ Failed to ADB connect %s (TLS-only or not WD port)\nOutput: %s\n", target, strings.TrimSpace(out))
		}
	}
}

func runADBConnect(target string) (string, bool) {
	out, _ := execCmdOutput("adb", "connect", target)
	outLower := strings.ToLower(out)
	if (strings.Contains(outLower, "connected to") || strings.Contains(outLower, "already connected")) &&
		!strings.Contains(outLower, "failed to connect") &&
		!strings.Contains(outLower, "cannot connect") &&
		!strings.Contains(outLower, "refused") {
		return out, true
	}
	return out, false
}

func deduplicateDevices() {
	out, err := execCmdOutput("adb", "devices", "-l")
	if err != nil {
		return
	}

	lines := strings.Split(out, "\n")
	var devices []DeviceInfo

	for _, line := range lines {
		line = strings.TrimSpace(line)
		if line == "" || strings.HasPrefix(line, "List of devices attached") {
			continue
		}
		fields := strings.Fields(line)
		if len(fields) < 2 {
			continue
		}

		id := fields[0]
		status := fields[1]
		dev := DeviceInfo{
			ID:         id,
			IsIP:       strings.Contains(id, ":") && !strings.HasPrefix(id, "emulator-") && !strings.Contains(id, "_tcp"),
			IsMDNS:     strings.Contains(id, "_adb-tls-connect._tcp"),
			IsEmulator: strings.HasPrefix(id, "emulator-"),
			IsOffline:  status == "offline" || status == "unauthorized",
		}
		for _, f := range fields[2:] {
			switch {
			case strings.HasPrefix(f, "model:"):
				dev.Model = strings.TrimPrefix(f, "model:")
			case strings.HasPrefix(f, "product:"):
				dev.Product = strings.TrimPrefix(f, "product:")
			case strings.HasPrefix(f, "device:"):
				dev.Device = strings.TrimPrefix(f, "device:")
			case strings.HasPrefix(f, "transport_id:"):
				dev.TransportID = strings.TrimPrefix(f, "transport_id:")
			}
		}
		if !dev.IsEmulator {
			devices = append(devices, dev)
		}
	}

	// First pass: remove any offline/unauthorized handles
	for _, d := range devices {
		if d.IsOffline {
			fmt.Printf("   🗑️  Removing offline handle: %s\n", d.ID)
			execCmd("adb", "disconnect", d.ID)
		}
	}

	// Second pass: group active devices by physical identity key
	deviceMap := make(map[string][]DeviceInfo)
	for _, d := range devices {
		if d.IsOffline || (d.Model == "" && d.Device == "") {
			continue
		}
		key := fmt.Sprintf("%s|%s|%s", d.Model, d.Product, d.Device)
		deviceMap[key] = append(deviceMap[key], d)
	}

	for key, group := range deviceMap {
		if len(group) <= 1 {
			continue
		}
		fmt.Printf("ℹ️  Deduplicating %d handles for device (%s):\n", len(group), key)

		// Prefer mDNS handle if present; otherwise keep first IP, drop the rest
		hasMDNS := false
		for _, d := range group {
			if d.IsMDNS {
				hasMDNS = true
				break
			}
		}

		if hasMDNS {
			for _, d := range group {
				if d.IsIP {
					fmt.Printf("   -> Removing redundant IP handle: %s\n", d.ID)
					execCmd("adb", "disconnect", d.ID)
				}
			}
		} else {
			for i := 1; i < len(group); i++ {
				if group[i].IsIP {
					fmt.Printf("   -> Removing duplicate IP handle: %s\n", group[i].ID)
					execCmd("adb", "disconnect", group[i].ID)
				}
			}
		}
	}
}

func showDevices() {
	out, err := execCmdOutput("adb", "devices", "-l")
	if err == nil {
		fmt.Println(strings.TrimSpace(out))
	}
}

func execCmd(name string, args ...string) {
	cmd := exec.Command(name, args...)
	cmd.Run()
}

func execCmdOutput(name string, args ...string) (string, error) {
	cmd := exec.Command(name, args...)
	var outBuf, errBuf bytes.Buffer
	cmd.Stdout = &outBuf
	cmd.Stderr = &errBuf
	err := cmd.Run()
	return outBuf.String() + " " + errBuf.String(), err
}
