$tag = git describe --tags --exact-match 2>$null
if ($tag -match '^\d+\.\d+\.\d+$') { $tag } else { "0.11.0-dev+$(git rev-parse --short=12 HEAD)" }
