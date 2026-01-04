package com.ruoran.houyi.mq;

import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.AccessChannel;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.junit.jupiter.api.Test;

/**
 * RocketMQ Producer 测试
 * 用于验证 RocketMQ 连接和消息发送
 */
public class RocketMqProducerTest {
    
    // 从环境变量读取配置
    private static final String ACCESS_KEY = System.getenv("ALIYUN_ACCESS_KEY");
    private static final String SECRET_KEY = System.getenv("ALIYUN_ACCESS_SECRET");
    private static final String NAME_SERVER_ADDR = "rmq-cn-e4k4hry5b07.cn-shanghai.rmq.aliyuncs.com:8080";
    private static final String NAMESPACE = "rmq-cn-e4k4hry5b07";
    private static final String TOPIC = "wechat-archive-msg";
    private static final String GROUP_ID = "wechat_msg_gid_order";
    private static final String TAG = "wechat_msg";
    
    @Test
    public void testProducerSendMessage() throws Exception {
        System.out.println("========================================");
        System.out.println("开始测试 RocketMQ Producer");
        System.out.println("接入点: " + NAME_SERVER_ADDR);
        System.out.println("实例 ID: " + NAMESPACE);
        System.out.println("Topic: " + TOPIC);
        System.out.println("Group ID: " + GROUP_ID);
        System.out.println("========================================");
        
        // 创建 ACL RPCHook
        RPCHook rpcHook = new AclClientRPCHook(
            new SessionCredentials(ACCESS_KEY, SECRET_KEY)
        );
        
        // 创建 Producer
        DefaultMQProducer producer = new DefaultMQProducer(GROUP_ID, rpcHook);
        
        try {
            // 配置 Producer（参照生产代码 RocketMqProducerConfig.java）
            System.out.println("\n配置 Producer...");
            producer.setProducerGroup(GROUP_ID);
            producer.setAccessChannel(AccessChannel.CLOUD);
            producer.setEnableTrace(true);
            producer.setNamesrvAddr(NAME_SERVER_ADDR);
            // RocketMQ 5.0 Serverless 不需要设置 Namespace
            // Namespace 已经包含在 NameServer 地址（实例 ID）中
            // 设置会导致 Topic 名称变成 "namespace%topic" 格式，无法找到路由
            // producer.setNamespaceV2(NAMESPACE);
            producer.setSendMsgTimeout(3000);
            
            System.out.println("Producer 配置完成:");
            System.out.println("  - Producer Group: " + producer.getProducerGroup());
            System.out.println("  - NameServer: " + producer.getNamesrvAddr());
            System.out.println("  - 注意：RocketMQ 5.0 Serverless 不设置 Namespace（已包含在 NameServer 地址中）");
            
            // 启动 Producer
            System.out.println("\n启动 Producer...");
            producer.start();
            System.out.println("✅ Producer 启动成功！");
            
            // 等待一下，让 Producer 获取路由信息
            System.out.println("\n等待 5 秒，让 Producer 获取路由信息...");
            Thread.sleep(5000);
            System.out.println("准备发送消息到 Topic: " + TOPIC);
            
            // 构建测试消息
            String messageBody = "{\"test\":\"RocketMQ 连接测试\",\"timestamp\":" + System.currentTimeMillis() + "}";
            Message message = new Message(
                TOPIC,
                TAG,
                messageBody.getBytes(RemotingHelper.DEFAULT_CHARSET)
            );
            message.setKeys("test-" + System.currentTimeMillis());
            
            // 发送消息
            System.out.println("\n发送测试消息...");
            System.out.println("消息内容: " + messageBody);
            
            SendResult sendResult = producer.send(message);
            
            System.out.println("\n========================================");
            System.out.println("✅ 消息发送成功！");
            System.out.println("Message ID: " + sendResult.getMsgId());
            System.out.println("Send Status: " + sendResult.getSendStatus());
            System.out.println("Queue ID: " + sendResult.getMessageQueue().getQueueId());
            System.out.println("Queue Offset: " + sendResult.getQueueOffset());
            System.out.println("========================================");
            
        } catch (Exception e) {
            System.err.println("\n========================================");
            System.err.println("❌ 测试失败！");
            System.err.println("错误类型: " + e.getClass().getName());
            System.err.println("错误信息: " + e.getMessage());
            System.err.println("========================================");
            e.printStackTrace();
            throw e;
        } finally {
            // 关闭 Producer
            if (producer != null) {
                System.out.println("\n关闭 Producer...");
                producer.shutdown();
                System.out.println("Producer 已关闭");
            }
        }
    }
    
    @Test
    public void testProducerWithoutNamespace() throws Exception {
        System.out.println("========================================");
        System.out.println("测试不设置 Namespace 的情况");
        System.out.println("========================================");
        
        RPCHook rpcHook = new AclClientRPCHook(
            new SessionCredentials(ACCESS_KEY, SECRET_KEY)
        );
        
        DefaultMQProducer producer = new DefaultMQProducer(GROUP_ID, rpcHook);
        
        try {
            producer.setProducerGroup(GROUP_ID);
            producer.setAccessChannel(AccessChannel.CLOUD);
            producer.setEnableTrace(true);
            producer.setNamesrvAddr(NAME_SERVER_ADDR);
            // 不设置 Namespace
            producer.setSendMsgTimeout(3000);
            
            System.out.println("启动 Producer（不设置 Namespace）...");
            producer.start();
            System.out.println("✅ Producer 启动成功！");
            
            Thread.sleep(2000);
            
            String messageBody = "{\"test\":\"无 Namespace 测试\"}";
            Message message = new Message(TOPIC, TAG, messageBody.getBytes(RemotingHelper.DEFAULT_CHARSET));
            message.setKeys("test-no-ns-" + System.currentTimeMillis());
            
            SendResult sendResult = producer.send(message);
            System.out.println("✅ 消息发送成功（无 Namespace）！Message ID: " + sendResult.getMsgId());
            
        } catch (Exception e) {
            System.err.println("❌ 无 Namespace 测试失败: " + e.getMessage());
            throw e;
        } finally {
            if (producer != null) {
                producer.shutdown();
            }
        }
    }
}

