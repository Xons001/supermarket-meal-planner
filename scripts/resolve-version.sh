#!/usr/bin/env bash
set -euo pipefail
tag="$(git describe --tags --exact-match 2>/dev/null || true)"
if [[ "$tag" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then printf '%s\n' "$tag"; else printf '0.11.0-dev+%s\n' "$(git rev-parse --short=12 HEAD)"; fi
