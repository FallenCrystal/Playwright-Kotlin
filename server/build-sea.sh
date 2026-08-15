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

# Return the Node.js executable inside a downloaded distribution.
node_binary_path() {
  local platform="$1"
  local node_dir="$2"

  if [ "$platform" = "win-x64" ]; then
    echo "$node_dir/node.exe"
  else
    echo "$node_dir/bin/node"
  fi
}

# Determine which same-version Node.js distribution can generate the SEA blob
# on the current host. SEA blobs are version-specific, even when code cache and
# snapshots are disabled.
detect_host_platform() {
  local machine
  machine="$(uname -m)"
  case "$machine" in
    x86_64|amd64) ;;
    *) return 1 ;;
  esac

  case "$(uname -s)" in
    Linux*) echo "linux-x64" ;;
    Darwin*) echo "macos-x64" ;;
    MINGW*|MSYS*|CYGWIN*) echo "win-x64" ;;
    *) return 1 ;;
  esac
}

# Generate SEA config. Each target gets its own files so Gradle parallel builds
# cannot overwrite another platform's blob or configuration.
generate_sea_config() {
  local platform="$1"
  local native_bundle
  native_bundle="$(to_native_path "$BUNDLE_FILE")"
  local native_output
  native_output="$(to_native_path "$BUILD_DIR/sea-prep-$platform.blob")"
  cat > "$BUILD_DIR/sea-config-$platform.json" <<EOF
{
  "main": "$native_bundle",
  "output": "$native_output",
  "disableExperimentalSEAWarning": true
}
EOF
}

# Download Node.js binary for the target platform
download_node() {
  local platform="$1"
  local node_dir="$BUILD_DIR/node-$platform"
  local node_bin
  node_bin="$(node_binary_path "$platform" "$node_dir")"

  if [ -f "$node_bin" ]; then
    echo "Node.js for $platform already downloaded, skipping..."
    return
  fi

  # mkdir is atomic and works in Git Bash as well as Unix shells. It prevents
  # parallel platform tasks from racing while preparing a shared host runtime.
  local lock_dir="$BUILD_DIR/.node-$platform.lock"
  while ! mkdir "$lock_dir" 2>/dev/null; do
    if [ -f "$node_bin" ]; then
      return
    fi
    if [ -f "$lock_dir/pid" ]; then
      local owner_pid
      owner_pid="$(cat "$lock_dir/pid" 2>/dev/null || true)"
      if [ -n "$owner_pid" ] && ! kill -0 "$owner_pid" 2>/dev/null; then
        rm -rf "$lock_dir"
        continue
      fi
    fi
    sleep 0.1
  done
  printf '%s' "$$" > "$lock_dir/pid"

  (
    local tmp_dir=""
    cleanup_download() {
      [ -z "$tmp_dir" ] || rm -rf "$tmp_dir"
      rm -rf "$lock_dir"
    }
    trap cleanup_download EXIT

    # Another process may have completed the download before we got the lock.
    if [ -f "$node_bin" ]; then
      exit 0
    fi

    tmp_dir="$(mktemp -d "$BUILD_DIR/.node-$platform.XXXXXX")"
    case "$platform" in
      linux-x64)
        local url="https://nodejs.org/dist/${NODE_VERSION}/node-${NODE_VERSION}-linux-x64.tar.xz"
        echo "Downloading Node.js for linux-x64..."
        curl --fail --show-error --location --silent "$url" \
          | tar -xJ --strip-components=1 -C "$tmp_dir"
        ;;
      win-x64)
        local url="https://nodejs.org/dist/${NODE_VERSION}/node-${NODE_VERSION}-win-x64.zip"
        local zip_file="$tmp_dir/node.zip"
        echo "Downloading Node.js for win-x64..."
        curl --fail --show-error --location --silent --output "$zip_file" "$url"
        unzip -q "$zip_file" "node-${NODE_VERSION}-win-x64/node.exe" -d "$tmp_dir/unpacked"
        mv "$tmp_dir/unpacked/node-${NODE_VERSION}-win-x64/node.exe" "$tmp_dir/node.exe"
        rm -rf "$tmp_dir/unpacked" "$zip_file"
        ;;
      macos-x64)
        local url="https://nodejs.org/dist/${NODE_VERSION}/node-${NODE_VERSION}-darwin-x64.tar.gz"
        echo "Downloading Node.js for macos-x64..."
        curl --fail --show-error --location --silent "$url" \
          | tar -xz --strip-components=1 -C "$tmp_dir"
        ;;
      *)
        echo "Unsupported platform: $platform" >&2
        exit 1
        ;;
    esac

    local downloaded_node
    downloaded_node="$(node_binary_path "$platform" "$tmp_dir")"
    if [ ! -f "$downloaded_node" ]; then
      echo "Downloaded Node.js archive for $platform is incomplete" >&2
      exit 1
    fi

    rm -rf "$node_dir"
    mv "$tmp_dir" "$node_dir"
    tmp_dir=""
  )
}

BLOB_GENERATOR=""

select_blob_generator() {
  if command -v node >/dev/null 2>&1 && [ "$(node --version)" = "$NODE_VERSION" ]; then
    BLOB_GENERATOR="$(command -v node)"
    return
  fi

  local host_platform
  if ! host_platform="$(detect_host_platform)"; then
    echo "Node.js $NODE_VERSION is required to generate SEA blobs on this host" >&2
    exit 1
  fi

  download_node "$host_platform"
  BLOB_GENERATOR="$(node_binary_path "$host_platform" "$BUILD_DIR/node-$host_platform")"
  if [ "$("$BLOB_GENERATOR" --version)" != "$NODE_VERSION" ]; then
    echo "Downloaded Node.js generator has an unexpected version: $BLOB_GENERATOR" >&2
    exit 1
  fi
}

# Build SEA for a specific platform
build_sea() {
  local platform="$1"
  local node_dir="$BUILD_DIR/node-$platform"
  local blob_file="$BUILD_DIR/sea-prep-$platform.blob"
  local config_file="$BUILD_DIR/sea-config-$platform.json"

  echo "=== Building SEA for $platform ==="

  # Generate the blob with exactly the same Node.js version as the target
  # executable. Mixing versions creates binaries that build successfully but
  # abort in node::sea::LoadSingleExecutableApplication at runtime.
  select_blob_generator
  generate_sea_config "$platform"
  echo "Generating SEA blob..."
  "$BLOB_GENERATOR" --experimental-sea-config "$(to_native_path "$config_file")"

  case "$platform" in
    linux-x64)
      local node_bin="$node_dir/bin/node"
      local output="$BUILD_DIR/playwright-server-linux-x64"

      cp "$node_bin" "$output"
      # Remove the signature (if any) before injecting
      echo "Injecting SEA blob into Node.js binary..."
      npx --no-install postject "$output" NODE_SEA_BLOB "$blob_file" \
        --sentinel-fuse NODE_SEA_FUSE_fce680ab2cc467b6e072b8b5df1996b2
      chmod +x "$output"
      echo "Built: $output"
      ;;
    win-x64)
      local node_bin="$node_dir/node.exe"
      local output="$BUILD_DIR/playwright-server-win-x64.exe"

      cp "$node_bin" "$output"
      echo "Injecting SEA blob into Node.js binary..."
      npx --no-install postject "$output" NODE_SEA_BLOB "$blob_file" \
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
      npx --no-install postject "$output" NODE_SEA_BLOB "$blob_file" \
        --sentinel-fuse NODE_SEA_FUSE_fce680ab2cc467b6e072b8b5df1996b2 \
        --macho-segment-name NODE_SEA
      chmod +x "$output"
      echo "Built: $output"
      ;;
  esac

  # Cleanup blob
  rm -f "$blob_file" "$config_file"
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
