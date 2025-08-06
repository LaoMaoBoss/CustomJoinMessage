# CustomJoinMessage 统一智能插件架构设计

## 🎯 项目目标

创建一个智能统一插件，能够：
- 🔍 自动检测运行环境（代理端/后端，具体核心类型）
- 🎛️ 智能切换运行模式（独立模式/协作模式）
- 🌐 多平台兼容（Velocity/BC + Paper/Spigot/Folia）
- ⚙️ 统一配置文件系统
- 📡 代理-后端智能通信

## 🏗️ 新架构设计

### 📁 项目结构
```
CustomJoinMessage/
├── src/main/java/com/example/customjoinmessage/
│   ├── core/                           # 核心系统
│   │   ├── CustomJoinMessagePlugin.java    # 主插件类
│   │   ├── PlatformDetector.java          # 平台检测器
│   │   ├── PluginMode.java                # 运行模式枚举
│   │   └── MessageManager.java            # 消息管理器
│   │
│   ├── config/                         # 配置系统
│   │   ├── ConfigManager.java             # 配置管理器
│   │   ├── MessageConfig.java             # 消息配置
│   │   └── PluginConfig.java              # 插件配置
│   │
│   ├── platform/                       # 平台适配器
│   │   ├── AbstractPlatformAdapter.java   # 抽象适配器
│   │   ├── proxy/                         # 代理端适配器
│   │   │   ├── VelocityAdapter.java       # Velocity适配器
│   │   │   └── BungeeCordAdapter.java     # BungeeCord适配器
│   │   └── backend/                       # 后端适配器
│   │       ├── PaperAdapter.java          # Paper适配器
│   │       ├── SpigotAdapter.java         # Spigot适配器
│   │       └── FoliaAdapter.java          # Folia适配器
│   │
│   ├── communication/                  # 通信系统
│   │   ├── ProxyBackendCommunicator.java  # 代理-后端通信
│   │   ├── MessageChannel.java            # 消息通道
│   │   └── ProtocolHandler.java           # 协议处理器
│   │
│   └── utils/                          # 工具类
│       ├── MessageFormatter.java          # 消息格式化
│       ├── PlayerTracker.java             # 玩家追踪
│       └── LoggerUtil.java                # 日志工具
│
├── src/main/resources/
│   ├── config.yml                      # 统一配置文件
│   ├── messages.yml                    # 消息配置文件
│   ├── plugin.yml                      # Bukkit插件描述
│   └── velocity-plugin.json            # Velocity插件描述
│
└── build.gradle.kts                    # 构建配置
```

### 🔍 运行模式

#### 模式1: 后端独立模式
```
后端服务器 (Paper/Spigot/Folia)
└── CustomJoinMessage插件
    ├── 拦截系统加入/离开消息
    ├── 发送自定义消息
    ├── 处理服务器内部切换
    └── 完整功能运行
```

#### 模式2: 代理+后端协作模式
```
代理服务器 (Velocity/BungeeCord)          后端服务器 (Paper/Spigot/Folia)
└── CustomJoinMessage插件                └── CustomJoinMessage插件
    ├── 检测玩家跨服务器移动                   ├── 检测到代理端插件存在
    ├── 发送加入/离开/切换消息                ├── 禁用自身消息发送功能
    ├── 管理全局玩家状态                     ├── 仅保留消息拦截功能
    └── 与后端插件通信                       └── 拦截系统默认消息
```

## 🔧 核心组件设计

### 1. PlatformDetector (平台检测器)
```java
public enum PlatformType {
    VELOCITY, BUNGEECORD,           // 代理端
    PAPER, SPIGOT, FOLIA,          // 后端
    UNKNOWN
}

public class PlatformDetector {
    public static PlatformType detectPlatform();
    public static boolean isProxyServer();
    public static boolean isBackendServer();
    public static String getServerVersion();
}
```

### 2. PluginMode (运行模式)
```java
public enum PluginMode {
    BACKEND_STANDALONE,     // 后端独立运行
    PROXY_MASTER,          // 代理端主控模式
    BACKEND_SLAVE          // 后端从属模式（仅拦截）
}
```

### 3. 配置文件系统
```yaml
# config.yml
plugin:
  mode: "auto"  # auto, standalone, proxy, backend
  debug: true
  
communication:
  enable-proxy-backend-sync: true
  channel: "customjoinmessage:sync"
  
features:
  welcome-message: true
  server-switch-message: true
  custom-join-format: true
  custom-leave-format: true

# messages.yml  
messages:
  join: "<green>✅ <yellow>{player}</yellow> 加入了服务器！</green>"
  leave: "<red>❌ <yellow>{player}</yellow> 离开了服务器！</red>"
  welcome: "<yellow>欢迎来到服务器，{player}！</yellow>"
  switch: "<gray>{player} 从 {from} 切换到 {to}</gray>"
  
placeholders:
  player: "玩家名"
  from: "来源服务器"  
  to: "目标服务器"
  time: "时间"
```

## 🔄 工作流程

### 插件启动流程
1. **平台检测** → 识别当前运行环境
2. **模式决定** → 根据环境和配置决定运行模式
3. **适配器加载** → 加载对应平台的适配器
4. **通信建立** → 如果是协作模式，建立代理-后端通信
5. **功能激活** → 根据模式激活相应功能

### 消息处理流程
**独立模式：**
```
玩家加入 → 后端拦截系统消息 → 发送自定义消息
```

**协作模式：**
```
玩家加入 → 后端拦截系统消息 → 通知代理端 → 代理端发送自定义消息
```

## 🎯 技术实现要点

### 1. 多平台兼容性
- 使用反射动态加载平台特定的API
- 抽象适配器模式统一接口
- 条件编译避免依赖冲突

### 2. 智能通信协议
- 使用PluginMessage通道进行代理-后端通信
- 定义标准协议格式
- 支持消息确认和重试机制

### 3. 配置热重载
- 支持不重启服务器修改配置
- 配置变化自动同步到所有节点
- 配置验证和错误处理

这个新架构将提供：
✅ 完全的平台兼容性
✅ 智能的运行模式切换  
✅ 用户友好的配置系统
✅ 高效的代理-后端协作
✅ 可扩展的插件架构