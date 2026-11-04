<div align="center">

# 电视直播

面向 Android TV / 电视盒子的直播应用，遥控器操作优先。

[下载安装包](https://github.com/wnhhh/livetv-update/releases/latest) ·
[历史版本](https://github.com/wnhhh/livetv-update/releases) ·
[自行编译](#自行编译)

</div>

## 这是什么

基于 [天光云影 mytv-android](https://github.com/yaoxieyoulei/mytv-android)（MIT 协议）二次开发的电视直播应用，
围绕遥控器操作、启动速度和家庭电视的实际使用场景做了较多改动。

| 项 | 值 |
|---|---|
| 应用名 | 电视直播 |
| 包名 | `com.livetv.player` |
| 当前版本 | 3.3.20（versionCode 13） |
| 最低系统 | Android 5.0（minSdk 21） |
| 编译目标 | targetSdk 36 |

## 下载与安装

在 [Releases](https://github.com/wnhhh/livetv-update/releases) 页面下载最新 apk，直接覆盖安装即可。
签名固定不变，升级不需要卸载（卸载会丢掉收藏配置）。

### 装好后要开两个权限

电视系统（尤其小米、创维等定制 ROM）不会在启动时弹权限请求，需要手动开启：

| 权限 | 是否必须 | 说明 |
|---|---|---|
| 应用内安装其他应用 | **必须** | 不开的话在线更新下载完成也装不上 |
| 读取外部存储 / 所有文件访问 | 可选 | 只有使用本地 txt/m3u 直播源文件时才需要，网络源不需要 |

开法有两种：

1. 应用内：`设置 → 权限`，两项点进去会跳到对应的系统页面
2. 系统设置：`电视设置 → 应用（或权限管理）→ 电视直播`

部分机型没有「安装未知应用」这个设置页，此时安装时会由系统弹窗询问是否允许，一样能装。

## 遥控器操作

| 按键 | 作用 |
|---|---|
| `OK` | 打开频道列表（左侧两级：分组 + 频道） |
| `↑` `↓` | 上一个 / 下一个频道 |
| `←` `→` | 切换播放线路 |
| 长按 `OK` | 收藏 / 取消收藏当前频道（收藏后右侧显示星标） |
| 长按 `←` | 节目单 |
| 长按 `→` | 线路列表 |
| 长按 `↓` | 播放控制 |
| 菜单键 / 长按 `OK`（直播画面） | 底部操作条 |
| 数字键 | 直接跳台 |
| `返回` | 连按两次退出 |

频道列表左列最上方是「收藏」分组，进入时会自动定位到正在播放的频道所属分组。

底部操作条分两行：第一行是频道列表、收藏、节目单、播放控制、设置；第二行是线路、显示模式、播放内核、音视频轨道、刷新订阅源、清缓存、主界面、返回。

## 与上游的主要差异

**启动与加载**
- 启动直接进入直播页，不再经过独立的加载页；源未就绪时用底部浮层提示
- 直播源缓存 6 小时，拉取失败回退本地缓存；网络重试次数从 10 次降到 3 次，避免启动卡死
- OkHttpClient 提为单例，减少重复创建连接池

**频道浏览**
- 频道列表改为两级侧边栏：左列分组、右列频道，右侧留出画面
- 分组自动整理：泛分组（地方、国际等）按频道名识别省份 / 国家再拆分，支持订阅源用 `地方/广东` 显式指定
- 「收藏」作为第一个分组常驻左列
- 节目单信息预计算并缓存，列表滚动不重复计算，每分钟刷新

**在线更新**
- 设置页新增「检查更新」，进入更新页即自动检查，显示当前版本 / 最新版本 / 更新通道
- 三个更新通道（稳定版 / 测试版 / 开发版）各自对应仓库里一份清单
- 下载完成直接调起系统安装器，不再要求先授予安装权限
- 安装失败时逐级尝试不同的系统设置页（带包名的安装未知应用 → 应用详情 → 应用管理 → 安全设置）

**权限**
- 补齐 `MANAGE_EXTERNAL_STORAGE`、`READ_EXTERNAL_STORAGE`、`WRITE_EXTERNAL_STORAGE` 声明
- 首次启动检测缺失权限并提示一次
- 从系统设置返回后自动刷新权限状态

## 自行编译

### 环境

- JDK 21
- Android SDK（compileSdk 36，需装对应 build-tools）
- Gradle 用仓库自带的 wrapper，不必单独安装

### 步骤

```bash
git clone https://github.com/wnhhh/livetv-update.git
cd livetv-update
```

在项目根目录创建 `key.properties`（**该文件被 git 忽略，不会进仓库**）：

```properties
storeFile=../keystore.jks
storePassword=你的口令
keyAlias=你的别名
keyPassword=你的口令
```

缺少这个文件时会回退用 debug 签名，编出来的包无法覆盖安装正式版。

```bash
./gradlew :tv:assembleOriginalRelease
```

产物：`tv/build/outputs/apk/original/release/`

### 两个 flavor

| flavor | applicationId | 用途 |
|---|---|---|
| `original` | `com.livetv.player` | 正式使用 |
| `disguised` | `com.chinablue.tv` | 备用包名，同一份代码换壳 |

## 发布新版本

发布脚本会做四件事：创建 GitHub Release、上传 apk 作为附件、更新三份更新清单、推送清单。

```bash
# 1. 改版本号（两个都要改，版本比较只认 versionName）
#    tv/build.gradle.kts
#    versionCode = 14
#    versionName = "3.3.21"

# 2. 打包
./gradlew :tv:assembleOriginalRelease
cp tv/build/outputs/apk/original/release/*3.3.21*.apk dist/livetv-3.3.21.apk

# 3. 发布（建 Release + 传包 + 改清单 + push）
bash tools/release.sh 3.3.21
```

脚本读取令牌的优先级：`~/.github_token` → 本机 Git 凭据管理器里已保存的 GitHub 凭据。
前者需要 `Contents: Read and write` 权限；后者只要有 `repo` 范围即可。

**apk 不进 Git 仓库**，只作为 Release 附件存在，仓库里只有源码和几 KB 的清单。

## 在线更新是怎么工作的

```
应用 → gh-proxy → raw.githubusercontent.com/.../tv-stable.json（清单，几 KB）
                       ↓ 里面的 browser_download_url
                  github.com/.../releases/download/vX.Y.Z/livetv-X.Y.Z.apk
```

- 应用先取清单，用清单里的 `tag_name` 与自身 `versionName` 比较，低于才提示更新
- 版本比较只看 `versionName`（`versionCode` 不参与），三段点分数字逐段比较，带 `-` 后缀视为预发布版
- 下载地址会统一拼上 `gh-proxy.com` 前缀，所以清单里写 GitHub 原始地址即可，不需要自己加代理

三份清单对应三个通道：

| 通道 | 清单文件 | 约定 |
|---|---|---|
| 稳定版（默认） | `tv-stable.json` | 正式发布 |
| 测试版 | `tv-beta.json` | 可写 `v3.3.21-beta` 这类预发布版本号 |
| 开发版 | `tv-dev.json` | 可写构建号 |

目前三份内容相同，都跟随稳定版。

## 目录结构

```
tv/                  电视端主程序（Compose for TV 界面、播放器、侧边栏、设置页）
mobile/              手机端外壳
core/data/           数据层：直播源解析、EPG、更新检查、网络
core/util/           工具：下载、APK 安装、版本比较
core/designsystem/   主题与通用组件
allinone/            外部二进制启动壳
ijkplayer-java/      IJK 播放内核
tools/release.sh     发版脚本
tv-stable.json       稳定版更新清单
```

## 声明

本项目基于 [yaoxieyoulei/mytv-android](https://github.com/yaoxieyoulei/mytv-android) 修改，
遵循 [MIT 协议](./LICENSE)，原项目版权归原作者所有，本仓库的改动同样以 MIT 协议发布。

仅用于个人学习与自用，不提供任何破解内容，直播源均来自网络公开地址，请在合法范围内使用。

## 致谢

- [yaoxieyoulei/mytv-android](https://github.com/yaoxieyoulei/mytv-android)：上游项目
- [fanmingming/live](https://github.com/fanmingming/live)：EPG 与频道图标
