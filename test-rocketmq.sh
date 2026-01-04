#!/bin/bash
# RocketMQ 5.0 连接测试脚本

set -e

TEST_DIR="/tmp/rocketmq-test-$$"
mkdir -p "$TEST_DIR"
cd "$TEST_DIR"

echo "========================================="
echo "🚀 RocketMQ 5.0 gRPC SDK 独立测试"
echo "========================================="
echo "测试目录: $TEST_DIR"
echo ""

# 创建 pom.xml
cat > pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.test</groupId>
    <artifactId>rocketmq-test</artifactId>
    <version>1.0</version>
    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.apache.rocketmq</groupId>
            <artifactId>rocketmq-client-java</artifactId>
            <version>5.0.7</version>
        </dependency>
    </dependencies>
</project>
EOF

# 创建测试代码
mkdir -p src/main/java
cat > src/main/java/RocketMqTest.java << 'EOF'
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.SessionCredentialsProvider;
import org.apache.rocketmq.client.apis.StaticSessionCredentialsProvider;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;

import java.nio.charset.StandardCharsets;

public class RocketMqTest {
    public static void main(String[] args) throws Exception {
        String endpoint = System.getenv("ROCKETMQ_ENDPOINT");
        String username = System.getenv("ROCKETMQ_USERNAME");
        String password = System.getenv("ROCKETMQ_PASSWORD");
        String topic = System.getenv("ROCKETMQ_TOPIC");
        
        if (endpoint == null) endpoint = "rmq-cn-v3m4likw605.cn-shanghai.rmq.aliyuncs.com:8080";
        if (topic == null) topic = "wechat-archive-msg";
        
        System.out.println("========================================");
        System.out.println("RocketMQ 5.0 gRPC SDK 测试");
        System.out.println("========================================");
        System.out.println("Endpoint: " + endpoint);
        System.out.println("Topic: " + topic);
        System.out.println("Username: " + (username != null ? username.substring(0, Math.min(8, username.length())) + "***" : "未设置"));
        System.out.println("");
        
        if (username == null || password == null) {
            System.err.println("❌ 请设置环境变量:");
            System.err.println("   ROCKETMQ_USERNAME=<实例用户名>");
            System.err.println("   ROCKETMQ_PASSWORD=<实例密码>");
            System.exit(1);
        }
        
        ClientServiceProvider provider = ClientServiceProvider.loadService();
        
        SessionCredentialsProvider credentialsProvider = 
            new StaticSessionCredentialsProvider(username, password);
        
        ClientConfiguration clientConfiguration = ClientConfiguration.newBuilder()
            .setEndpoints(endpoint)
            .setCredentialProvider(credentialsProvider)
            .build();
        
        System.out.println("创建 Producer...");
        
        try (Producer producer = provider.newProducerBuilder()
                .setClientConfiguration(clientConfiguration)
                .setTopics(topic)
                .build()) {
            
            System.out.println("✅ Producer 创建成功！");
            
            String messageBody = "{\"test\":\"RocketMQ 5.0 测试\",\"timestamp\":" + System.currentTimeMillis() + "}";
            
            // 顺序消息必须设置 MessageGroup（分区键）
            String messageGroup = "test-group-001";
            
            org.apache.rocketmq.client.apis.message.Message message = provider.newMessageBuilder()
                .setTopic(topic)
                .setTag("test")
                .setKeys("test-" + System.currentTimeMillis())
                .setMessageGroup(messageGroup)  // FIFO 消息必须设置 MessageGroup
                .setBody(messageBody.getBytes(StandardCharsets.UTF_8))
                .build();
            
            System.out.println("MessageGroup: " + messageGroup + " (FIFO 顺序消息)");
            
            System.out.println("发送消息: " + messageBody);
            
            SendReceipt sendReceipt = producer.send(message);
            
            System.out.println("");
            System.out.println("========================================");
            System.out.println("✅ 消息发送成功！");
            System.out.println("Message ID: " + sendReceipt.getMessageId());
            System.out.println("========================================");
            
        } catch (ClientException e) {
            System.err.println("");
            System.err.println("========================================");
            System.err.println("❌ 测试失败！");
            System.err.println("错误: " + e.getMessage());
            System.err.println("========================================");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
EOF

echo "编译项目..."
mvn compile -q

echo ""
echo "运行测试..."
echo ""

# 运行测试
mvn exec:java -Dexec.mainClass="RocketMqTest" -q

# 清理
cd /
rm -rf "$TEST_DIR"

