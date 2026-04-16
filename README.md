# LotteryCalculator - 智能彩票计算器

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-1.9.0-purple?style=for-the-badge&logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/Android-API%2024%2B-green?style=for-the-badge&logo=android" alt="Android API">
  <img src="https://img.shields.io/badge/Retrofit-2.9.0-blue?style=for-the-badge&logo=square" alt="Retrofit">
  <img src="https://img.shields.io/badge/MVVM-Architecture-orange?style=for-the-badge&logo=android" alt="MVVM">
</p>

## 📱 项目简介

LotteryCalculator 是一款功能强大的Android彩票计算器应用，专门为中国彩票爱好者设计。应用集成了实时彩票数据获取、个人彩票管理、智能中奖计算等功能，支持**大乐透**和**双色球**两种主流彩票类型。

## ✨ 核心特性

### 🎯 开奖信息
- **实时开奖信息**：获取最新彩票开奖结果，包含期号、开奖号码、开奖时间
- **历史数据查询**：查看历史开奖记录，最多支持30期历史数据
- **智能期号导航**：左右按钮轻松切换期号，支持期号边界检测
- **数据可视化**：直观展示前区号码和后区号码

### 🧮 彩票算奖
- **个人彩票管理**：添加、编辑、删除个人购买的彩票
- **智能中奖计算**：根据开奖结果自动计算彩票中奖等级和奖金
- **批量计算支持**：一键计算所有彩票的中奖结果
- **组合票计算**：支持组合票的自动拆解和计算
- **结果可视化**：清晰展示中奖号码匹配情况和奖金信息

### 🔧 高级功能
- **数据缓存优化**：智能缓存机制减少API请求，提升响应速度
- **离线数据支持**：缓存历史数据，网络异常时仍可查看
- **用户友好界面**：Material Design设计，操作流畅直观
- **性能优化**：延迟加载、防抖处理、内存管理优化

## 🏗️ 技术架构

### 技术栈
- **编程语言**：Kotlin 100%
- **架构模式**：MVVM (Model-View-ViewModel)
- **UI框架**：Android View Binding + Fragments
- **网络层**：Retrofit 2.9.0 + Gson
- **本地存储**：Room Database + DataStore Preferences
- **异步处理**：Kotlin Coroutines + Flow
- **依赖注入**：手动依赖注入
- **构建工具**：Gradle Kotlin DSL

### 项目结构
```
app/src/main/java/com/example/lotterycalculator/
├── data/                          # 数据层
│   ├── model/                    # 数据模型类
│   │   ├── DrawResult.kt         # 开奖结果模型
│   │   ├── LotteryType.kt        # 彩票类型枚举
│   │   ├── UserTicket.kt         # 用户彩票模型
│   │   ├── LotteryResult.kt      # 中奖结果模型
│   │   └── PeriodBoundary.kt     # 期号边界模型
│   └── repository/               # 数据仓库层
│       └── LotteryRepository.kt  # 彩票数据仓库
├── network/                      # 网络层
│   └── LotteryApiService.kt      # API服务接口
├── viewmodel/                    # ViewModel层
│   ├── CalculationViewModel.kt   # 彩票算奖ViewModel
│   └── DisplayViewModel.kt       # 开奖信息ViewModel
└── ui/                           # 界面层
    ├── calculation/              # 彩票算奖界面
    │   ├── CalculationFragment.kt           # 彩票算奖主界面
    │   ├── TicketAdapter.kt                 # 彩票列表适配器
    │   ├── AddTicketDialogFragment.kt       # 添加彩票对话框
    │   └── CalculationResultsDialogFragment.kt  # 计算结果对话框
    ├── display/                  # 开奖信息界面
    │   ├── DisplayFragment.kt               # 开奖信息主界面
    │   └── HistoryDisplayFragment.kt        # 历史数据显示界面
    └── MainActivity.kt           # 主活动容器
```

### 设计模式
- **Repository模式**：统一数据访问接口
- **观察者模式**：LiveData实现UI数据绑定
- **单例模式**：API服务管理
- **适配器模式**：RecyclerView适配器
- **工厂模式**：数据模型创建

## 🚀 快速开始

### 环境要求
- Android Studio Flamingo 或更高版本
- Android SDK 34+
- Java 8 或 Kotlin 1.9.0+
- Gradle 8.0+

### 安装步骤
1. **克隆仓库**
   ```bash
   git clone https://github.com/LiOne-W/LotteryCalculator.git
   cd LotteryCalculator
   ```

2. **配置API密钥**
   - 前往 [MXNZP官网](https://www.mxnzp.com/) 注册账号
   - 获取 `app_id` 和 `app_secret`
   - 复制 `gradle.properties.template` 文件为 `gradle.properties`
   - 在 `gradle.properties` 中替换 `YOUR_APP_ID_HERE` 和 `YOUR_APP_SECRET_HERE` 为您的实际密钥

3. **构建应用**
   ```bash
   ./gradlew assembleDebug
   ```
   或通过Android Studio直接运行

4. **安装APK**
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

### 配置说明
#### API密钥配置
本项目需要MXNZP彩票API密钥才能正常运行。请按以下步骤获取和配置密钥：

1. **获取API密钥**
   - 访问 [MXNZP官网](https://www.mxnzp.com/) 注册账号
   - 登录后进入"我的应用"或"API管理"页面
   - 创建新应用，获取 `app_id` 和 `app_secret`

2. **配置密钥**
   项目使用外部配置文件管理API密钥，确保密钥安全不泄露到代码仓库：

   **方法一：使用gradle.properties文件（推荐）**
   1. 复制项目根目录下的 `gradle.properties.template` 文件，重命名为 `gradle.properties`
   2. 编辑 `gradle.properties` 文件，找到以下配置项：
      ```
      MXNZP_API_APP_ID=YOUR_APP_ID_HERE
      MXNZP_API_APP_SECRET=YOUR_APP_SECRET_HERE
      ```
   3. 将 `YOUR_APP_ID_HERE` 和 `YOUR_APP_SECRET_HERE` 替换为您从MXNZP获取的实际密钥

   **方法二：使用环境变量（高级用户）**
   您也可以通过设置环境变量的方式配置API密钥：
   - 设置 `MXNZP_API_APP_ID` 环境变量为您的app_id
   - 设置 `MXNZP_API_APP_SECRET` 环境变量为您的app_secret

   **验证配置**
   配置完成后，重新构建应用，API密钥将自动注入到BuildConfig中，应用代码通过 `BuildConfig.MXNZP_APP_ID` 和 `BuildConfig.MXNZP_APP_SECRET` 读取密钥。

3. **注意事项**
   - 请妥善保管您的API密钥，不要公开分享
   - API调用可能有频率限制，请合理使用
   - 如遇API访问问题，请检查密钥是否正确或联系MXNZP技术支持

#### 调试配置
- 启用网络日志：`OkHttp logging-interceptor`
- 数据缓存调试：查看Logcat标签 `LotteryRepository` 和 `CalculationViewModel`

## 📖 使用指南

### 开奖信息使用
1. **查看最新开奖**：打开应用默认显示最新一期开奖信息
2. **切换彩票类型**：点击Tab标签在大乐透和双色球之间切换
3. **查看历史数据**：点击左右按钮切换期号，查看历史开奖记录
4. **刷新数据**：下拉刷新获取最新数据

### 彩票算奖使用
1. **添加彩票**：点击"添加彩票"按钮，输入彩票号码
2. **管理彩票**：长按彩票可编辑或删除，点击名称可修改
3. **单票计算**：点击单张彩票的"计算"按钮查看中奖结果
4. **批量计算**：点击"计算全部"一键计算所有彩票
5. **结果查看**：计算结果自动弹出，显示中奖等级和匹配号码

### 高级功能
- **组合票支持**：创建组合票时系统自动拆解为所有可能组合
- **缓存优化**：应用退出后再次进入时自动加载缓存数据
- **防抖处理**：按钮点击防抖，防止重复操作

## 🔧 开发指南

### 添加新功能
1. **添加新彩票类型**
   ```kotlin
   // 1. 在 LotteryType.kt 中添加枚举
   enum class LotteryType {
       DALETOU, SHUANGSEQIU, NEW_LOTTERY
   }
   
   // 2. 在 API 服务中添加支持
   // 3. 在 Repository 中实现数据获取逻辑
   ```

2. **扩展计算功能**
   ```kotlin
   // 在 LotteryResult.kt 中添加新的计算逻辑
   fun calculateAdvancedPrize(ticket: UserTicket, drawResult: DrawResult): LotteryResult
   ```

### 调试技巧
- **网络请求调试**：查看Logcat标签 `LotteryApiService`
- **数据缓存调试**：查看Logcat标签 `LotteryRepository`
- **UI状态调试**：查看Logcat标签 `CalculationViewModel`
- **内存泄漏检测**：使用Android Profiler监控内存使用

### 性能优化建议
1. **图片优化**：使用WebP格式，启用图片压缩
2. **网络优化**：启用HTTP/2，配置连接池
3. **内存优化**：使用弱引用管理大对象
4. **启动优化**：延迟初始化非核心组件

## 📊 API集成

### MXNZP彩票API
应用集成MXNZP彩票API，支持以下接口：

| 接口名称 | 功能描述 | 使用场景 |
|---------|---------|---------|
| `aim_lottery` | 指定期号开奖信息 | 查看特定期号开奖结果 |
| `latest_lottery` | 最新一期开奖信息 | 获取最新开奖数据 |
| `history_lottery` | 历史开奖信息 | 查看历史开奖记录 |
| `prize_calc` | 中奖结果计算 | 计算彩票中奖等级 |

### 数据缓存策略
```kotlin
// 三级缓存策略
1. 内存缓存：Repository静态缓存，应用生命周期内有效
2. 本地存储：Room数据库持久化存储
3. 网络请求：实时API数据，保证数据最新性
```

### 错误处理机制
- **网络异常**：自动重试机制，降级到缓存数据
- **API限制**：请求频率限制，避免被封禁
- **数据解析**：Gson安全解析，异常捕获处理

## 🤝 贡献指南

我们欢迎任何形式的贡献！

### 贡献流程
1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 代码规范
- **Kotlin风格**：遵循官方Kotlin编码规范
- **命名规范**：使用描述性命名，避免缩写
- **注释要求**：公共API必须添加文档注释
- **测试要求**：新功能必须包含单元测试

### 提交信息规范
```
类型(范围): 描述

[详细描述]

[关联Issue]
```

类型包括：feat、fix、docs、style、refactor、test、chore

## 📝 版本历史

### v1.0.0 (当前版本)
- ✅ 基础开奖信息：大乐透、双色球开奖信息展示
- ✅ 彩票算奖核心功能：个人彩票管理、中奖计算
- ✅ 数据缓存：智能缓存机制，提升性能
- ✅ 用户界面：Material Design设计，响应式布局
- ✅ API集成：MXNZP彩票API完整集成

### 未来计划
- 🚧 UI重构：迁移到Jetpack Compose
- 🚧 多语言支持：中英文国际化
- 🚧 数据分析：中奖统计和趋势分析
- 🚧 推送通知：开奖提醒功能
- 🚧 云同步：多设备数据同步

## 📄 许可证

本项目采用 **MIT许可证** - 查看 [LICENSE](LICENSE) 文件了解详情

## 🙏 致谢

- **MXNZP团队**：提供稳定可靠的彩票API服务
- **Android社区**：优秀的开源库和技术支持
- **所有贡献者**：感谢每一位为项目做出贡献的人

---

<p align="center">
  Made with ❤️ for lottery enthusiasts
</p>

<p align="center">
  ⭐️ 如果这个项目对你有帮助，请给个Star支持！
</p>