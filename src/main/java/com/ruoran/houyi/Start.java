/*
 * This file is part of the zyan/wework-msgaudit.
 *
 * (c) 读心印 <aa24615@qq.com>
 *
 * This source file is subject to the MIT license that is bundled
 * with this source code in the file LICENSE.
 */


package com.ruoran.houyi;

import com.ruoran.houyi.model.CorpInfo;
import com.ruoran.houyi.mq.HouyiTcpRetryConsumer;
import com.ruoran.houyi.repo.CorplistRepo;
import com.ruoran.houyi.service.CorpConfigService;
import com.ruoran.houyi.service.EventBus;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author renlu
 */
@Slf4j
@Component
@Data
public class Start implements CommandLineRunner {


    @Resource
    CorplistRepo corplistRepo;

    @Resource
    DownloadThreadKeeper downloadThreadKeeper;

    @Resource
    SpringContextUtils springContextUtils;

    @Resource
    CorpConfigService corpConfigService;

    @Resource
    HouyiTcpRetryConsumer tcpRetryConsumer;

    @Resource
    EventBus eventBus;

    Set<String> copySet = new CopyOnWriteArraySet<>();

    public void triggerCorpId(String corpId) throws Exception {
        Optional<CorpInfo> corpInfoOptional = corplistRepo.findFirstByCorpid(corpId);
        if(!corpInfoOptional.isPresent()){
            throw new Exception("启动corp任务失败");
        }
        if(copySet.contains(corpId)){
            return;
        }
        CorpInfo corpInfo = corpInfoOptional.get();
        updateCorpInfo(corpInfo);
        Message t =new Message();
        t.init(corpInfo.getCorpid(), corpInfo.getSecret(),corpInfo.getPrikey());
        springContextUtils.autowireBean(t);
        copySet.add(corpId);
        eventBus.getCorpsStatus().put(corpInfo.getCorpid(),true);
        t.start();

    }

    /**
     * 更新企业信息（已废弃，现在从配置文件读取）
     * 保留此方法以兼容旧代码，但不再调用外部 API
     */
    @Deprecated
    private void updateCorpInfo(CorpInfo corpInfo) {
        // 企业信息现在从配置文件读取，由 CorpConfigService 在启动时自动同步
        // 这里只做日志记录
        log.debug("企业信息已从配置文件加载: {} - {}", corpInfo.getCorpid(), corpInfo.getCorpname());
    }

    @Override
    public  void run(String... args){
        log.info("=== 企业微信会话存档系统启动 ===");

        // 初始化各个组件
        downloadThreadKeeper.init();
        // RocketMQ 5.0 TCP 重试消费者会自动初始化（@PostConstruct）
        
        // RocketMQ 5.0 TCP 重试消费者会自动初始化和启动（@PostConstruct）

        // 从数据库获取所有启用的企业（配置已在 CorpConfigService 启动时同步）
        List<CorpInfo> corpInfos = corplistRepo.findAllByStatus(1L);
        log.info("发现 {} 个启用的企业配置", corpInfos.size());

        // 为每个企业创建消息处理线程
        List<Message> messageList  = new ArrayList<>();
        for (CorpInfo corpInfo : corpInfos) {
            log.info("准备启动企业: {} ({})", corpInfo.getCorpname(), corpInfo.getCorpid());
            
            Message t = new Message();
            t.init(corpInfo.getCorpid(), corpInfo.getSecret(), corpInfo.getPrikey());
            springContextUtils.autowireBean(t);
            copySet.add(corpInfo.getCorpid());
            eventBus.getCorpsStatus().put(corpInfo.getCorpid(), true);
            messageList.add(t);
        }
        
        // 启动所有消息处理线程
        for(Message message : messageList){
            message.start();
        }

        log.info("=== 所有企业消息处理线程已启动 ===");
    }




}
