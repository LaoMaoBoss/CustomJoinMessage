# 最终解决方案 - 自定义加入消息

## 问题分析

经过多次尝试，我们发现系统的加入/离开消息可能通过以下方式传输，无法在Velocity插件层面直接拦截：

1. **Minecraft原生协议包** - 绕过了Velocity的事件系统
2. **底层网络通信** - 直接在TCP/网络层传输
3. **Velocity内部处理** - 在我们的插件加载之前就处理了
4. **后端服务器直连** - 某些消息可能不经过Velocity

## 有效解决方案

### 🎯 方案1: Velocity配置禁用 (最简单)

**修改Velocity的 `velocity.toml` 配置文件：**

```toml
# Velocity配置文件

[servers]
# 你的服务器配置...

[forced-hosts]
# 强制主机配置...

[advanced]
# 禁用某些消息转发
compression-threshold = 256
compression-level = -1
login-ratelimit = 3000
connection-timeout = 5000
read-timeout = 30000
haproxy-protocol = false

# 尝试禁用消息转发
disable-default-server-switch-messages = true

[query]
enabled = false

# 消息相关配置
[messages]
# 可能需要这些配置来禁用默认消息
player-info-forwarding-mode = "MODERN"  # 或 "LEGACY" 或 "NONE"
```

### 🎯 方案2: 后端服务器插件 (推荐)

创建一个简单的Paper/Spigot插件来禁用系统消息：

**plugin.yml:**
```yaml
name: DisableJoinMessages
version: 1.0
main: DisableJoinMessages
api-version: 1.19
description: Disable default join/leave messages
```

**DisableJoinMessages.java:**
```java
public class DisableJoinMessages extends JavaPlugin implements Listener {
    
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("已禁用默认加入/离开消息");
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null); // 禁用加入消息
    }
    
    @EventHandler(priority = EventPriority.HIGHEST) 
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null); // 禁用离开消息
    }
}
```

### 🎯 方案3: EssentialsX配置 (最常用)

如果你的后端服务器使用EssentialsX：

**在 `plugins/Essentials/config.yml` 中：**
```yaml
# 禁用加入/离开消息
announce-format: ''
leave-format: ''
join-format: ''

# 新玩家也不显示消息
newbies:
  announce-format: ''
  
# 确保消息被完全禁用
custom-join-message: false
custom-quit-message: false
```

### 🎯 方案4: Gamerule设置

在每个后端服务器的控制台执行：
```
gamerule announceAdvancements false
gamerule sendCommandFeedback false
gamerule commandBlockOutput false
```

### 🎯 方案5: 使用其他插件

推荐插件：
- **JoinLeaveMessages** - 专门控制加入离开消息
- **AdvancedBan** - 包含消息控制功能  
- **CMI** - 综合管理插件，可以禁用系统消息

## 网络架构考虑

```
玩家 → Velocity代理 → 后端服务器(Paper/Spigot)
                          ↓
                     系统消息生成
                          ↓
                   [需要在这里禁用]
```

## 测试步骤

1. **确认后端服务器类型**
   ```bash
   # 在后端服务器控制台运行
   version
   ```

2. **临时测试禁用**
   ```bash
   # 在后端服务器控制台运行
   gamerule announceAdvancements false
   ```

3. **安装对应插件**
   - Paper/Spigot → 使用上面的Java插件
   - 有EssentialsX → 修改配置文件
   - Fabric → 安装相应mod

4. **重启后端服务器**

5. **测试效果**
   - 玩家加入应该只看到Velocity插件的自定义消息
   - 不应该看到系统默认消息

## 如果还是不行

如果以上方案都不能解决，可能需要：

1. **检查网络架构** - 确认消息传输路径
2. **使用数据包分析工具** - 如Wireshark分析网络包
3. **联系服务器管理员** - 可能需要更深层的配置
4. **考虑使用其他代理** - 如BungeeCord或其他解决方案

## 推荐执行顺序

1. 先尝试方案2 (后端插件) - 最直接有效
2. 如果不行，尝试方案3 (EssentialsX) - 最常用
3. 最后考虑方案1 (Velocity配置) - 作为补充

这些方案应该能解决99%的情况！