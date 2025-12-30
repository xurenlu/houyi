# 日志警告修复说明

## 问题描述

在应用启动时出现以下两个日志信息：

1. **Commons Logging 冲突警告**
   ```
   Standard Commons Logging discovery in action with spring-jcl: 
   please remove commons-logging.jar from classpath in order to avoid potential conflicts
   ```

2. **Logback 配置文件路径信息**
   ```
   |-INFO in ch.qos.logback.core.joran.spi.ConfigurationWatchList@7c2b6087 - 
   URL [jar:nested:/home/admin/houyi/houyi-0.0.1-SNAPSHOT.jar/!BOOT-INF/classes/!/logback-spring.xml] 
   is not of type file
   ```

## 问题分析

### 1. Commons Logging 冲突

- **原因**：某些第三方依赖（如 aliyun-sdk-oss、ons-client、c3p0）传递引入了 `commons-logging`
- **影响**：可能导致日志框架冲突，Spring Boot 3.x 使用 `spring-jcl` 统一管理日志
- **级别**：INFO（警告，非错误）

### 2. Logback 配置文件路径

- **原因**：Spring Boot 打包为可执行 JAR 后，logback-spring.xml 位于嵌套 JAR 内部
- **影响**：logback 无法监控 JAR 内配置文件的变化（scan 功能失效）
- **级别**：INFO（正常现象）

## 解决方案

### 1. 排除 commons-logging 依赖

在 `pom.xml` 中对以下依赖添加了 exclusions：

- `aliyun-sdk-oss`
- `ons-client` (RocketMQ)
- `c3p0`

```xml
<dependency>
    <groupId>com.aliyun.oss</groupId>
    <artifactId>aliyun-sdk-oss</artifactId>
    <version>3.18.1</version>
    <exclusions>
        <exclusion>
            <groupId>commons-logging</groupId>
            <artifactId>commons-logging</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

### 2. 关闭 Logback Debug 模式

修改 `logback-spring.xml`：

```xml
<!-- 修改前 -->
<configuration scan="true" scanPeriod="60 seconds" debug="true">

<!-- 修改后 -->
<configuration scan="true" scanPeriod="60 seconds" debug="false">
```

这样可以减少 logback 内部日志输出，避免显示不必要的 INFO 信息。

## 重新构建

修改后需要重新构建项目：

```bash
# 清理并重新打包
mvn clean package -DskipTests

# 或使用项目的构建脚本
./make.sh
```

## 验证

重新启动应用后，这两个警告信息应该不再出现：

```bash
# 启动应用
./scripts/start.sh start

# 查看日志
tail -f /var/log/houyi/houyi.log
```

## 注意事项

1. **scan 功能限制**：由于配置文件在 JAR 包内，logback 的 `scan="true"` 功能在生产环境中不会生效。如需修改日志配置，需要重新打包并重启应用。

2. **外部配置文件**（可选）：如果需要在运行时动态修改日志配置，可以将 logback-spring.xml 放到外部：
   ```bash
   java -Dlogging.config=/path/to/logback-spring.xml -jar houyi.jar
   ```

3. **日志级别调整**：可以通过 Spring Boot 的配置文件动态调整日志级别，无需修改 logback-spring.xml：
   ```yaml
   # application-prod.yml
   logging:
     level:
       com.ruoran.houyi: WARN
       org.springframework: INFO
   ```

## 相关文件

- `pom.xml` - Maven 依赖配置
- `src/main/resources/logback-spring.xml` - Logback 日志配置
- `scripts/start.sh` - 应用启动脚本

## 参考资料

- [Spring Boot Logging](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.logging)
- [Logback Configuration](https://logback.qos.ch/manual/configuration.html)
- [Commons Logging Bridge](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#spring-jcl)

