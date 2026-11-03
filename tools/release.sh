#!/usr/bin/env bash
# 发布新版本：替换仓库根目录的 apk 与三份清单，然后推送
#
# 用法:
#   bash tools/release.sh 3.3.21                    # 使用 dist/livetv-3.3.21.apk
#   bash tools/release.sh 3.3.21 path/to/app.apk    # 指定 apk 路径
#
# 前提：tv/build.gradle.kts 里的 versionName 已改成同一版本号，且已 assembleOriginalRelease
set -euo pipefail

VER="${1:?用法: bash tools/release.sh <版本号> [apk路径]}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APK_SRC="${2:-$ROOT/dist/livetv-$VER.apk}"
APK_NAME="livetv-$VER.apk"

[ -f "$APK_SRC" ] || { echo "找不到安装包：$APK_SRC"; exit 1; }

cd "$ROOT"

# 1. 只保留最新一个发布包，旧包从仓库移除
for old in livetv-*.apk; do
  [ -e "$old" ] || continue
  if [ "$old" != "$APK_NAME" ]; then
    git rm -q --cached "$old" 2>/dev/null || true
    rm -f "$old"
    echo "移除旧包：$old"
  fi
done

# 2. 放入新包
cp "$APK_SRC" "$APK_NAME"

# 3. 更新三份清单
python - "$VER" "$APK_NAME" <<'PY'
import json
import sys

ver, apk = sys.argv[1], sys.argv[2]
url = f"https://raw.githubusercontent.com/wnhhh/livetv-update/main/{apk}"

files = {
    "tv-stable.json": f"{ver} 更新内容\n1. （请补充本版改动说明）",
    "tv-beta.json": f"测试版当前与稳定版相同（{ver}）。",
    "tv-dev.json": f"开发版当前与稳定版相同（{ver}）。",
}

for name, body in files.items():
    with open(name, "w", encoding="utf-8") as f:
        json.dump(
            {"tag_name": f"v{ver}", "assets": [{"browser_download_url": url}], "body": body},
            f, ensure_ascii=False, indent=2,
        )
        f.write("\n")
        print(f"已更新 {name}")
PY

# 4. 提交并推送（源码与发布包在同一仓库，普通 push 即可）
git add -A
git commit -q -m "release: v$VER"
git push origin main

echo "发布完成：v$VER"
