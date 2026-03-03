#!/usr/bin/env bash
set -euo pipefail

# Build SEA (Single Executable Application) for playwright-kotlin server
# Usage: bash build-sea.sh [linux-x64|win-x64|macos-x64|all]
#
# Prerequisites:
#   - npm run build        (compile TS → dist/)
#   - npm run build:bundle (esbuild → bundle/server-bundle.js)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Convert MSYS2/Git Bash paths to Windows paths for Node.js compatibility
to_native_path() {
  if command -v cygpath &>/dev/null; then
    cygpath -m "$1"
  else
    echo "$1"
  fi
}

NODE_VERSION="v22.12.0"
BUILD_DIR="$SCRIPT_DIR/build"
BUNDLE_FILE="$SCRIPT_DIR/bundle/server-bundle.js"

if [ ! -f "$BUNDLE_FILE" ]; then
  echo "Error: $BUNDLE_FILE not found. Run 'npm run build:bundle' first."
  exit 1
fi

mkdir -p "$BUILD_DIR"

# Generate SEA config
generate_sea_config() {
  local native_bundle
  native_bundle="$(to_native_path "$BUNDLE_FILE")"
  local native_output
  native_output="$(to_native_path "$BUILD_DIR/sea-prep.blob")"
  cat > "$BUILD_DIR/sea-config.json" <<EOF
{
  "main": "$native_bundle",
  "output": "$native_output",
  "disableExperimentalSEAWarning": true
}
EOF
}

# Download Node.js binary for the target platform
download_node() {
  local platform="$1"  # linux-x64 or win-x64
  local node_dir="$BUILD_DIR/node-$platform"

  if [ -d "$node_dir" ]; then
    echo "Node.js for $platform already downloaded, skipping..."
    return
  fi

  mkdir -p "$node_dir"

  case "$platform" in
    linux-x64)
      local url="https://nodejs.org/dist/${NODE_VERSION}/node-${NODE_VERSION}-linux-x64.tar.xz"
      echo "Downloading Node.js for linux-x64..."
      curl -sL "$url" | tar -xJ --strip-components=1 -C "$node_dir"
      ;;
    win-x64)
      local url="https://nodejs.org/dist/${NODE_VERSION}/node-${NODE_VERSION}-win-x64.zip"
      local zip_file="$BUILD_DIR/node-win-x64.zip"
      echo "Downloading Node.js for win-x64..."
      curl -sL -o "$zip_file" "$url"
      # Extract node.exe from the zip
      unzip -q -o "$zip_file" "node-${NODE_VERSION}-win-x64/node.exe" -d "$BUILD_DIR/tmp-win"
      mv "$BUILD_DIR/tmp-win/node-${NODE_VERSION}-win-x64/node.exe" "$node_dir/node.exe"
      rm -rf "$BUILD_DIR/tmp-win" "$zip_file"
      ;;
    macos-x64)
      local url="https://nodejs.org/dist/${NODE_VERSION}/node-${NODE_VERSION}-darwin-x64.tar.gz"
      echo "Downloading Node.js for macos-x64..."
      curl -sL "$url" | tar -xz --strip-components=1 -C "$node_dir"
      ;;
    *)
      echo "Unsupported platform: $platform"
      exit 1
      ;;
  esac
}

# Build SEA for a specific platform
build_sea() {
  local platform="$1"
  local node_dir="$BUILD_DIR/node-$platform"

  echo "=== Building SEA for $platform ==="

  # Generate SEA blob using the host Node.js
  generate_sea_config
  echo "Generating SEA blob..."
  node --experimental-sea-config "$(to_native_path "$BUILD_DIR/sea-config.json")"

  case "$platform" in
    linux-x64)
      local node_bin="$node_dir/bin/node"
      local output="$BUILD_DIR/playwright-server-linux-x64"

      cp "$node_bin" "$output"
      # Remove the signature (if any) before injecting
      echo "Injecting SEA blob into Node.js binary..."
      npx postject "$output" NODE_SEA_BLOB "$BUILD_DIR/sea-prep.blob" \
        --sentinel-fuse NODE_SEA_FUSE_fce680ab2cc467b6e072b8b5df1996b2
      chmod +x "$output"
      echo "Built: $output"
      ;;
    win-x64)
      local node_bin="$node_dir/node.exe"
      local output="$BUILD_DIR/playwright-server-win-x64.exe"

      cp "$node_bin" "$output"
      echo "Injecting SEA blob into Node.js binary..."
      npx postject "$output" NODE_SEA_BLOB "$BUILD_DIR/sea-prep.blob" \
        --sentinel-fuse NODE_SEA_FUSE_fce680ab2cc467b6e072b8b5df1996b2
      echo "Built: $output"
      ;;
    macos-x64)
      local node_bin="$node_dir/bin/node"
      local output="$BUILD_DIR/playwright-server-macos-x64"

      cp "$node_bin" "$output"
      # Remove the existing signature before injecting (macOS only)
      if command -v codesign &>/dev/null; then
        codesign --remove-signature "$output"
      fi
      echo "Injecting SEA blob into Node.js binary..."
      npx postject "$output" NODE_SEA_BLOB "$BUILD_DIR/sea-prep.blob" \
        --sentinel-fuse NODE_SEA_FUSE_fce680ab2cc467b6e072b8b5df1996b2 \
        --macho-segment-name NODE_SEA
      chmod +x "$output"
      echo "Built: $output"
      ;;
  esac

  # Cleanup blob
  rm -f "$BUILD_DIR/sea-prep.blob" "$BUILD_DIR/sea-config.json"
}

# Main
TARGET="${1:-all}"

case "$TARGET" in
  linux-x64)
    download_node linux-x64
    build_sea linux-x64
    ;;
  win-x64)
    download_node win-x64
    build_sea win-x64
    ;;
  macos-x64)
    download_node macos-x64
    build_sea macos-x64
    ;;
  all)
    download_node linux-x64
    download_node win-x64
    download_node macos-x64
    build_sea linux-x64
    build_sea win-x64
    build_sea macos-x64
    ;;
  *)
    echo "Usage: $0 [linux-x64|win-x64|macos-x64|all]"
    exit 1
    ;;
esac

echo ""
echo "=== Build complete ==="
ls -lh "$BUILD_DIR"/playwright-server-* 2>/dev/null || echo "No binaries found."
