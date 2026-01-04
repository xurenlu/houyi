package com.ruoran.houyi.mq;

import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.SessionCredentialsProvider;
import org.apache.rocketmq.client.apis.StaticSessionCredentialsProvider;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * RocketMQ 5.0 gRPC SDK Producer 测试
 * 用于验证 RocketMQ 连接和消息发送
 */
public class RocketMqProducerTest {
    
    // 从环境变量读取配置
    private static final String ENDPOINT = System.getenv("ROCKETMQ_ENDPOINT") != null ? 
        System.getenv("ROCKETMQ_ENDPOINT") : "rmq-cn-v3m4likw605.cn-shanghai.rmq.aliyuncs.com:8080";
    private static final String USERNAME = System.getenv("ROCKETMQ_USERNAME");
    private static final String PASSWORD = System.getenv("ROCKETMQ_PASSWORD");
    private static final String TOPIC = System.getenv("ROCKETMQ_TOPIC") != null ? 
        System.getenv("ROCKETMQ_TOPIC") : "wechat-archive-msg";
    private static final String RETRY_TOPIC = System.getenv("ROCKETMQ_RETRY_TOPIC") != null ? 
        System.getenv("ROCKETMQ_RETRY_TOPIC") : "wechat-archive-retry";
    private static final String TAG = "wechat_msg";
    
    /**
     * 诊断测试：检查网络连通性和配置
     */
    @Test
    public void testDiagnose() throws Exception {
        System.out.println("========================================");
        System.out.println("🔍 RocketMQ 5.0 gRPC SDK 诊断测试");
        System.out.println("========================================");
        
        // 1. 检查环境变量
        System.out.println("\n1️⃣ 检查环境变量...");
        System.out.println("  ENDPOINT: " + ENDPOINT);
        System.out.println("  USERNAME: " + (USERNAME != null ? USERNAME.substring(0, Math.min(8, USERNAME.length())) + "***" : "❌ 未设置!"));
        System.out.println("  PASSWORD: " + (PASSWORD != null ? "***已设置***" : "❌ 未设置!"));
        System.out.println("  TOPIC: " + TOPIC);
        
        if (USERNAME == null || PASSWORD == null) {
            System.err.println("❌ 环境变量未设置，请设置 ROCKETMQ_USERNAME 和 ROCKETMQ_PASSWORD");
            return;
        }
        
        // 2. 检查网络连通性
        System.out.println("\n2️⃣ 检查网络连通性...");
        String host = ENDPOINT.split(":")[0];
        int port = Integer.parseInt(ENDPOINT.split(":")[1]);
        System.out.println("  目标地址: " + host + ":" + port);
        
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 5000);
            System.out.println("  ✅ 网络连接成功！");
        } catch (Exception e) {
            System.err.println("  ❌ 网络连接失败: " + e.getMessage());
            System.err.println("  请检查：");
            System.err.println("    - 是否需要禁用代理？（gRPC 不能走 HTTP 代理）");
            System.err.println("    - 防火墙是否阻止了连接？");
            return;
        }
        
        // 3. 测试 Producer 创建
        System.out.println("\n3️⃣ 测试 Producer 创建...");
        
        ClientServiceProvider provider = ClientServiceProvider.loadService();
        SessionCredentialsProvider credentialsProvider = 
            new StaticSessionCredentialsProvider(USERNAME, PASSWORD);
        ClientConfiguration clientConfiguration = ClientConfiguration.newBuilder()
            .setEndpoints(ENDPOINT)
            .setCredentialProvider(credentialsProvider)
            .build();
        
        try (Producer producer = provider.newProducerBuilder()
                .setClientConfiguration(clientConfiguration)
                .setTopics(TOPIC)  // 只订阅主 Topic（重试 Topic 可能不存在）
                .build()) {
            System.out.println("  ✅ Producer 创建成功！");
        }
        
        System.out.println("\n========================================");
        System.out.println("诊断完成，所有检查通过！");
        System.out.println("========================================");
    }
    
    /**
     * 发送 FIFO 顺序消息测试
     */
    @Test
    public void testSendFifoMessage() throws Exception {
        System.out.println("========================================");
        System.out.println("🚀 RocketMQ 5.0 FIFO 消息测试");
        System.out.println("========================================");
        
        if (USERNAME == null || PASSWORD == null) {
            System.err.println("❌ 请设置环境变量 ROCKETMQ_USERNAME 和 ROCKETMQ_PASSWORD");
            return;
        }
        
        System.out.println("Endpoint: " + ENDPOINT);
        System.out.println("Topic: " + TOPIC);
        System.out.println("Username: " + USERNAME.substring(0, Math.min(8, USERNAME.length())) + "***");
        
        ClientServiceProvider provider = ClientServiceProvider.loadService();
        SessionCredentialsProvider credentialsProvider = 
            new StaticSessionCredentialsProvider(USERNAME, PASSWORD);
        ClientConfiguration clientConfiguration = ClientConfiguration.newBuilder()
            .setEndpoints(ENDPOINT)
            .setCredentialProvider(credentialsProvider)
            .build();
        
        System.out.println("\n创建 Producer...");
        
        try (Producer producer = provider.newProducerBuilder()
                .setClientConfiguration(clientConfiguration)
                .setTopics(TOPIC)
                .build()) {
            
            System.out.println("✅ Producer 创建成功！");
            
            // 构建 FIFO 消息
            String messageGroup = "test-group-001";  // 相同 group 的消息保证顺序
            String messageBody = "{\"test\":\"FIFO 消息测试\",\"timestamp\":" + System.currentTimeMillis() + "}";
            
            org.apache.rocketmq.client.apis.message.Message message = provider.newMessageBuilder()
                .setTopic(TOPIC)
                .setTag(TAG)
                .setKeys("test-fifo-" + System.currentTimeMillis())
                .setMessageGroup(messageGroup)  // FIFO 消息必须设置 MessageGroup
                .setBody(messageBody.getBytes(StandardCharsets.UTF_8))
                .build();
            
            System.out.println("\n发送 FIFO 消息...");
            System.out.println("消息内容: " + messageBody);
            System.out.println("MessageGroup: " + messageGroup);
            
            SendReceipt sendReceipt = producer.send(message);
            
            System.out.println("\n========================================");
            System.out.println("✅ FIFO 消息发送成功！");
            System.out.println("Message ID: " + sendReceipt.getMessageId());
            System.out.println("========================================");
            
        } catch (ClientException e) {
            System.err.println("\n========================================");
            System.err.println("❌ FIFO 消息发送失败！");
            System.err.println("错误: " + e.getMessage());
            System.err.println("========================================");
            throw e;
        }
    }
    
    /**
     * 发送延迟消息测试
     */
    @Test
    public void testSendDelayMessage() throws Exception {
        System.out.println("========================================");
        System.out.println("🚀 RocketMQ 5.0 延迟消息测试");
        System.out.println("========================================");
        
        if (USERNAME == null || PASSWORD == null) {
            System.err.println("❌ 请设置环境变量 ROCKETMQ_USERNAME 和 ROCKETMQ_PASSWORD");
            return;
        }
        
        System.out.println("Endpoint: " + ENDPOINT);
        System.out.println("Topic: " + RETRY_TOPIC);
        System.out.println("Username: " + USERNAME.substring(0, Math.min(8, USERNAME.length())) + "***");
        
        ClientServiceProvider provider = ClientServiceProvider.loadService();
        SessionCredentialsProvider credentialsProvider = 
            new StaticSessionCredentialsProvider(USERNAME, PASSWORD);
        ClientConfiguration clientConfiguration = ClientConfiguration.newBuilder()
            .setEndpoints(ENDPOINT)
            .setCredentialProvider(credentialsProvider)
            .build();
        
        System.out.println("\n创建 Producer...");
        
        try (Producer producer = provider.newProducerBuilder()
                .setClientConfiguration(clientConfiguration)
                .setTopics(RETRY_TOPIC)
                .build()) {
            
            System.out.println("✅ Producer 创建成功！");
            
            // 构建延迟消息
            long delayMs = 30000;  // 30 秒延迟
            long deliveryTimestamp = System.currentTimeMillis() + delayMs;
            String messageBody = "{\"test\":\"延迟消息测试\",\"timestamp\":" + System.currentTimeMillis() + "}";
            
            org.apache.rocketmq.client.apis.message.Message message = provider.newMessageBuilder()
                .setTopic(RETRY_TOPIC)
                .setTag(TAG)
                .setKeys("test-delay-" + System.currentTimeMillis())
                .setDeliveryTimestamp(deliveryTimestamp)  // 延迟消息设置投递时间
                .setBody(messageBody.getBytes(StandardCharsets.UTF_8))
                .build();
            
            System.out.println("\n发送延迟消息...");
            System.out.println("消息内容: " + messageBody);
            System.out.println("延迟时间: " + delayMs + " ms");
            System.out.println("预计投递时间: " + new java.util.Date(deliveryTimestamp));
            
            SendReceipt sendReceipt = producer.send(message);
            
            System.out.println("\n========================================");
            System.out.println("✅ 延迟消息发送成功！");
            System.out.println("Message ID: " + sendReceipt.getMessageId());
            System.out.println("========================================");
            
        } catch (ClientException e) {
            System.err.println("\n========================================");
            System.err.println("❌ 延迟消息发送失败！");
            System.err.println("错误: " + e.getMessage());
            System.err.println("========================================");
            throw e;
        }
    }
}
