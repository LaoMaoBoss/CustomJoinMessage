# CustomJoinMessage

一款用于 Velocity 代理服务器的 Minecraft 插件，用于自定义玩家加入和离开服务器时的消息。

## 功能特性

- 🎨 自定义玩家加入消息
- 🎨 自定义玩家离开消息  
- 🌈 支持 MiniMessage 格式的富文本消息
- 📝 详细的日志记录
- ⚡ 轻量级，高性能

## 环境要求

- Java 21 或更高版本
- Velocity 3.2.0 或更高版本

## 构建和安装

1. 克隆项目：
   ```bash
   git clone <your-repo-url>
   cd CustomJoinMessage
   ```

2. 编译项目：
   ```bash
   ./gradlew build
   ```

3. 安装插件：
   - 将生成的 `build/libs/CustomJoinMessage-1.0.0.jar` 复制到 Velocity 服务器的 `plugins` 目录
   - 重启 Velocity 服务器

## 使用说明

插件安装后会自动工作，默认消息格式：
- 加入消息：`✅ {玩家名} 加入了服务器！`（绿色）
- 离开消息：`❌ {玩家名} 离开了服务器！`（红色）

## 开发

本项目使用 Gradle 构建，支持以下命令：

- `./gradlew build` - 编译项目
- `./gradlew clean` - 清理构建文件
- `./gradlew shadowJar` - 生成包含依赖的 jar 文件

## 许可证

[MIT License](LICENSE)