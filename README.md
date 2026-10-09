# ProfileLimitUnlock

> 作者：林小冉 ｜ 测试设备：Redmi K70 ｜ 系统：澎湃OS 3.x（Android 16）

一个 LSPosed 模块：解除 Android（MIUI / HyperOS）**每个用户最多 2 个 managed profile（工作资料/应用分身）** 的限制。

## 功能

- **分身数量上限可随意修改**：默认 99，范围 1~999，在桌面 App 里改，**即时生效、无需重启**
- 同时放开三处限制：
  - `config_multiuserMaximumUsers`（用户总数）
  - `config_multiuserMaxRunningUsers`（可同时运行数）
  - MANAGED 分身"每父用户上限"（报错 "Cannot add more profiles of type MANAGED" 的真正拦截点）
- 桌面图标可显示/隐藏（默认隐藏），从 LSPosed 管理器或系统设置进入

## 使用

1. 确保已 root 并装好 [LSPosed](https://github.com/JingMatrix/LSPosed)（需 Zygisk 环境）；
2. 从 [Releases](../../releases) 下载 `ProfileLimitUnlock.apk` 并安装；
3. LSPosed 管理器 → 模块 → 启用 **ProfileLimitUnlock**，作用域勾选 **系统框架（android）**；
4. 重启手机（软重启即可）；
5. 桌面打开 **ProfileLimitUnlock** 应用（或 LSPosed 管理器 → 模块 → 点击模块名进入），随意填写/拖动分身上限。

## 构建

需要 Android SDK（aapt2、d8、apksigner、zipalign，任一 34+ build-tools 版本）。

```bash
export ANDROID_SDK=~/Library/Android/sdk  # macOS
# export ANDROID_SDK=~/Android/Sdk       # Linux
export BUILD_TOOLS=$ANDROID_SDK/build-tools/34.0.0
./build.sh
```

产物：`ProfileLimitUnlock.apk`（debug 签名）。

## License

[MIT](LICENSE)
