#!/usr/bin/env bash
# 发布新版本：把 apk 上传到 GitHub Releases，并更新仓库里的三份更新清单
#
# 用法:
#   bash tools/release.sh 3.3.21                    # 使用 dist/livetv-3.3.21.apk
#   bash tools/release.sh 3.3.21 path/to/app.apk    # 指定 apk 路径
#
# 令牌：从 ~/.github_token 读取，只对 livetv-update 需要 Contents 读写权限
# 前提：tv/build.gradle.kts 的 versionName 已是同一版本号，且已 assembleOriginalRelease
set -euo pipefail

REPO="wnhhh/livetv-update"
VER="${1:?用法: bash tools/release.sh <版本号> [apk路径]}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APK_SRC="${2:-$ROOT/dist/livetv-$VER.apk}"
APK_NAME="livetv-$VER.apk"
TAG="v$VER"
TOKEN_FILE="$HOME/.github_token"

[ -f "$APK_SRC" ] || { echo "找不到安装包：$APK_SRC"; exit 1; }

# 令牌来源：优先 ~/.github_token，其次复用本机 Git 凭据管理器里已登录的凭据
if [ -f "$TOKEN_FILE" ]; then
  TOKEN="$(tr -d ' \r\n' < "$TOKEN_FILE")"
else
  TOKEN="$(printf 'protocol=https\nhost=github.com\n\n' | git credential fill 2>/dev/null | sed -n 's/^password=//p')"
fi
[ -n "$TOKEN" ] || { echo "取不到 GitHub 令牌：请配置 ~/.github_token 或先 push 一次让 Git 记住凭据"; exit 1; }

cd "$ROOT"

api() { curl -s -H "Authorization: Bearer $TOKEN" -H "Accept: application/vnd.github+json" "$@"; }

# 1. 建 release（已存在则复用）
RELEASE_ID="$(api "https://api.github.com/repos/$REPO/releases/tags/$TAG" \
  | python -c 'import json,sys; d=json.load(sys.stdin); print(d.get("id",""))' 2>/dev/null || true)"

if [ -z "$RELEASE_ID" ]; then
  echo "创建 release $TAG ..."
  BODY="$(python -c "import json; print(json.dumps({'tag_name': '$TAG', 'name': '$TAG', 'body': '版本 $VER'}, ensure_ascii=False))")"
  RESP="$(api -X POST "https://api.github.com/repos/$REPO/releases" -d "$BODY")"
  RELEASE_ID="$(echo "$RESP" | python -c 'import json,sys; d=json.load(sys.stdin); print(d["id"] if d.get("id") else sys.exit("创建 release 失败: " + json.dumps(d, ensure_ascii=False)[:300]))')"
else
  echo "release $TAG 已存在，复用 id=$RELEASE_ID"
fi

# 2. 上传附件（同名附件先删，避免重复）
EXISTING_ASSET="$(api "https://api.github.com/repos/$REPO/releases/$RELEASE_ID/assets" \
  | python -c "import json,sys; print(next((a['id'] for a in json.load(sys.stdin) if a['name']=='$APK_NAME'), ''))")"

if [ -n "$EXISTING_ASSET" ]; then
  echo "删除同名旧附件 ..."
  api -X DELETE "https://api.github.com/repos/$REPO/releases/assets/$EXISTING_ASSET" >/dev/null
fi

echo "上传 $APK_NAME ($(du -h "$APK_SRC" | cut -f1)) ..."
api -X POST \
  -H "Content-Type: application/vnd.android.package-archive" \
  --data-binary "@$APK_SRC" \
  "https://uploads.github.com/repos/$REPO/releases/$RELEASE_ID/assets?name=$APK_NAME" \
  | python -c 'import json,sys; d=json.load(sys.stdin); print("browser_download_url:", d["browser_download_url"] if d.get("browser_download_url") else sys.exit("上传失败: " + json.dumps(d, ensure_ascii=False)[:300]))'

# 3. 更新三份清单，指向 release 附件
URL="https://github.com/$REPO/releases/download/$TAG/$APK_NAME"

python - "$VER" "$URL" <<'PY'
import json
import sys

ver, url = sys.argv[1], sys.argv[2]

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

# 4. 只提交清单，apk 不进仓库
git add tv-stable.json tv-beta.json tv-dev.json
git commit -q -m "release: v$VER"
git push origin main

echo "发布完成：$TAG"
echo "下载地址：$URL"
