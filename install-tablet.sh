#!/usr/bin/env bash
# ==============================================================================
# Onnkiosk Tablet Setup & Installation Helper
# ==============================================================================
# Automatically prepares connected Android tablets (e.g. Walmart Onn 8" / ceres-b3),
# configures ambient kiosk settings (stay awake, landscape lock, update permissions),
# downloads or builds the latest Onnkiosk APK, installs it, and launches the app.
# ==============================================================================

set -euo pipefail

BOLD='\033[1m'
GREEN='\033[0;32m'
GOLD='\033[0;33m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m'

PACKAGE_NAME="com.nernal.frame.oauth"
ACTIVITY_NAME="com.nernal.frame.MainActivity"
GITHUB_REPO="derallo/onnkiosk"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

show_banner() {
    cat << "EOF"
  ___              _    _           _    
 / _ \ _ __  _ __ | | _(_) ___  ___| | __
| | | | '_ \| '_ \| |/ / |/ _ \/ __| |/ /
| |_| | | | | | | |   <| | (_) \__ \   < 
 \___/|_| |_|_| |_|_|\_\_|\___/|___/_|\_\
  Ambient Smart Display Tablet Installer
EOF
    echo -e "${GOLD}================================================================${NC}"
}

show_help() {
    show_banner
    echo -e "${BOLD}USAGE:${NC}"
    echo "  ./install-tablet.sh [options]"
    echo ""
    echo -e "${BOLD}OPTIONS:${NC}"
    echo "  --latest           Fetch and install the latest published GitHub release APK (default)"
    echo "  --local [PATH]     Install a local APK (or app/build/outputs/apk/release/app-release.apk)"
    echo "  --build            Build release APK locally using Gradle, then install"
    echo "  --setup-only       Apply kiosk settings (stay awake, permissions) without reinstalling"
    echo "  --launch           Only launch the app on the tablet"
    echo "  --device [ID]      Target a specific ADB device ID if multiple are connected"
    echo "  -h, --help         Show this help screen with step-by-step guidance"
    echo ""
    echo -e "${BOLD}FIRST-TIME TABLET PREPARATION STEPS:${NC}"
    echo -e "  1. On your tablet, open ${CYAN}Settings${NC} -> ${CYAN}About tablet${NC}."
    echo -e "  2. Scroll to ${CYAN}Build number${NC} and tap it ${BOLD}7 times${NC} until it says \"You are now a developer!\"."
    echo -e "  3. Go to ${CYAN}Settings${NC} -> ${CYAN}System${NC} -> ${CYAN}Developer options${NC}."
    echo -e "  4. Turn ${BOLD}ON${NC} ${CYAN}USB debugging${NC}."
    echo -e "  5. Connect the tablet to this machine via USB-C cable."
    echo -e "  6. When the popup appears on the tablet, check ${BOLD}\"Always allow from this computer\"${NC} and tap ${BOLD}OK${NC}."
    echo ""
}

# Parse command line flags
MODE="latest"
LOCAL_APK=""
TARGET_DEVICE=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --latest)
            MODE="latest"
            shift
            ;;
        --local)
            MODE="local"
            if [[ $# -gt 1 && ! "$2" =~ ^-- ]]; then
                LOCAL_APK="$2"
                shift 2
            else
                shift
            fi
            ;;
        --build)
            MODE="build"
            shift
            ;;
        --setup-only)
            MODE="setup-only"
            shift
            ;;
        --launch)
            MODE="launch"
            shift
            ;;
        --device)
            TARGET_DEVICE="$2"
            shift 2
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            show_help
            exit 1
            ;;
    esac
done

show_banner

# Step 1: Check ADB installation
if ! command -v adb >/dev/null 2>&1; then
    echo -e "${RED}[!] ADB is not installed or not in your PATH.${NC}"
    echo "    Install with: sudo apt install adb (or install Android SDK Platform-Tools)"
    exit 1
fi

# Step 2: Check connected ADB devices
echo -e "${CYAN}[*] Checking connected Android devices...${NC}"
DEVICE_LIST=$(adb devices | grep -v "List of devices" | grep "device$" || true)

if [ -z "$DEVICE_LIST" ]; then
    echo -e "${RED}[!] No authorized Android devices found.${NC}"
    echo ""
    echo -e "${BOLD}TROUBLESHOOTING CHECKLIST:${NC}"
    echo "  1. Is the USB cable securely plugged into both the tablet and computer?"
    echo "  2. Did you enable USB Debugging in Settings -> System -> Developer options?"
    echo "  3. Did you unlock the tablet screen and tap 'Always allow from this computer'?"
    echo ""
    echo "Run 'adb devices' to verify detection, then rerun this script."
    exit 1
fi

# Select target device
if [ -n "$TARGET_DEVICE" ]; then
    ADB_CMD="adb -s $TARGET_DEVICE"
else
    DEVICE_COUNT=$(echo "$DEVICE_LIST" | wc -l)
    if [ "$DEVICE_COUNT" -gt 1 ]; then
        FIRST_DEV=$(echo "$DEVICE_LIST" | head -n 1 | awk '{print $1}')
        echo -e "${GOLD}[i] Multiple devices detected. Defaulting to first device: $FIRST_DEV${NC}"
        ADB_CMD="adb -s $FIRST_DEV"
    else
        FIRST_DEV=$(echo "$DEVICE_LIST" | awk '{print $1}')
        ADB_CMD="adb -s $FIRST_DEV"
    fi
fi

DEV_MODEL=$($ADB_CMD shell getprop ro.product.model 2>/dev/null || echo "Android Tablet")
DEV_NAME=$($ADB_CMD shell getprop ro.product.name 2>/dev/null || echo "Generic")
DEV_API=$($ADB_CMD shell getprop ro.build.version.sdk 2>/dev/null || echo "Unknown")

echo -e "${GREEN}[✓] Connected to:${NC} ${BOLD}$DEV_MODEL ($DEV_NAME)${NC} running Android API $DEV_API"
echo ""

# Function to apply kiosk device settings
apply_kiosk_settings() {
    echo -e "${CYAN}[*] Applying ambient countertop kiosk optimizations...${NC}"
    
    # Stay awake while plugged into charging power (1=AC, 2=USB, 4=Wireless -> 7=All)
    echo "    - Configuring 'Stay awake while charging' (7)..."
    $ADB_CMD shell settings put global stay_on_while_plugged_in 7
    
    # Auto-rotate / Landscape lock check
    echo "    - Ensuring landscape screen orientation..."
    $ADB_CMD shell settings put system accelerometer_rotation 1

    # Grant special permission: Install Unknown Packages (enables built-in GitHub Releases self-updater)
    echo "    - Granting self-update permission (REQUEST_INSTALL_PACKAGES)..."
    $ADB_CMD shell appops set "$PACKAGE_NAME" REQUEST_INSTALL_PACKAGES allow 2>/dev/null || true

    # Grant notifications permission for event alerts
    echo "    - Granting notification permissions for event alerts..."
    $ADB_CMD shell pm grant "$PACKAGE_NAME" android.permission.POST_NOTIFICATIONS 2>/dev/null || true

    echo -e "${GREEN}[✓] Kiosk hardware settings applied.${NC}"
}

if [ "$MODE" = "setup-only" ]; then
    apply_kiosk_settings
    echo -e "${GREEN}${BOLD}Setup complete!${NC}"
    exit 0
fi

if [ "$MODE" = "launch" ]; then
    echo -e "${CYAN}[*] Launching $PACKAGE_NAME on tablet...${NC}"
    $ADB_CMD shell am start -n "$PACKAGE_NAME/$ACTIVITY_NAME"
    echo -e "${GREEN}[✓] Onnkiosk launched!${NC}"
    exit 0
fi

# Step 3: Determine APK to install
APK_PATH=""

if [ "$MODE" = "build" ]; then
    echo -e "${CYAN}[*] Building release APK using Gradle...${NC}"
    if [ ! -f "$SCRIPT_DIR/gradlew" ]; then
        echo -e "${RED}[!] gradlew not found in $SCRIPT_DIR.${NC}"
        exit 1
    fi
    (cd "$SCRIPT_DIR" && ./gradlew assembleRelease)
    APK_PATH="$SCRIPT_DIR/app/build/outputs/apk/release/app-release.apk"
elif [ "$MODE" = "local" ]; then
    if [ -n "$LOCAL_APK" ]; then
        APK_PATH="$LOCAL_APK"
    else
        DEFAULT_LOCAL="$SCRIPT_DIR/app/build/outputs/apk/release/app-release.apk"
        if [ -f "$DEFAULT_LOCAL" ]; then
            APK_PATH="$DEFAULT_LOCAL"
        else
            echo -e "${RED}[!] No local APK specified and $DEFAULT_LOCAL not found.${NC}"
            echo "    Run with --latest to download from GitHub or --build to compile."
            exit 1
        fi
    fi
elif [ "$MODE" = "latest" ]; then
    echo -e "${CYAN}[*] Fetching latest release from GitHub ($GITHUB_REPO)...${NC}"
    DOWNLOAD_URL=$(python3 -c "
import urllib.request, json
try:
    req = urllib.request.Request('https://api.github.com/repos/$GITHUB_REPO/releases/latest', headers={'User-Agent': 'Onnkiosk-Installer'})
    data = json.loads(urllib.request.urlopen(req, timeout=10).read())
    assets = data.get('assets', [])
    apks = [a['browser_download_url'] for a in assets if a['name'].endswith('.apk')]
    if apks:
        print(apks[0])
except Exception as e:
    pass
")

    if [ -z "$DOWNLOAD_URL" ]; then
        # Fallback to direct latest release asset URL pattern
        DOWNLOAD_URL="https://github.com/$GITHUB_REPO/releases/latest/download/app-release.apk"
    fi

    CACHE_DIR="/tmp/onnkiosk_install"
    mkdir -p "$CACHE_DIR"
    APK_PATH="$CACHE_DIR/app-release.apk"

    echo -e "${CYAN}[*] Downloading:${NC} $DOWNLOAD_URL"
    curl -fL -o "$APK_PATH" "$DOWNLOAD_URL"
fi

if [ ! -f "$APK_PATH" ]; then
    echo -e "${RED}[!] APK file not found at: $APK_PATH${NC}"
    exit 1
fi

echo -e "${GREEN}[✓] APK ready:${NC} $APK_PATH ($(du -h "$APK_PATH" | cut -f1))"
echo ""

# Step 4: Install APK to tablet
echo -e "${CYAN}[*] Installing Onnkiosk to tablet ($ADB_CMD install -r -d)...${NC}"
$ADB_CMD install -r -d "$APK_PATH"
echo -e "${GREEN}[✓] APK installed successfully!${NC}"
echo ""

# Step 5: Apply Kiosk Settings & Permissions
apply_kiosk_settings
echo ""

# Step 6: Launch Application
echo -e "${CYAN}[*] Starting Onnkiosk on tablet...${NC}"
$ADB_CMD shell am start -n "$PACKAGE_NAME/$ACTIVITY_NAME"
echo -e "${GREEN}[✓] Onnkiosk is running!${NC}"
echo ""

echo -e "${GOLD}================================================================${NC}"
echo -e "${BOLD}${GREEN}INSTALLATION & TABLET CONFIGURATION COMPLETE!${NC}"
echo -e "${GOLD}================================================================${NC}"
echo "Next steps on your tablet:"
echo "  1. Tap the Account (person) or Settings (gear) icon in the top-right."
echo "  2. Tap 'Sign in with Google' to sync your Calendar, Tasks, and Photos."
echo "  3. Set your home location for real-time weather & live Doppler radar."
echo ""
