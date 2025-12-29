package com.ruoran.houyi.service;

import com.ruoran.houyi.config.WeWorkCorpProperties;
import com.ruoran.houyi.model.CorpInfo;
import com.ruoran.houyi.repo.CorplistRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 企业配置服务
 * 从本地配置文件加载企业信息，替代外部 API
 *
 * @author refactored
 */
@Service
@Slf4j
public class CorpConfigService {

    @Resource
    private WeWorkCorpProperties weWorkCorpProperties;

    @Resource
    private CorplistRepo corplistRepo;

    /**
     * 应用启动时同步配置到数据库
     */
    @PostConstruct
    public void syncCorpConfigToDatabase() {
        log.info("开始同步企业配置到数据库...");
        
        List<WeWorkCorpProperties.CorpConfig> corps = weWorkCorpProperties.getCorps();
        if (corps == null || corps.isEmpty()) {
            log.warn("未配置任何企业信息，请检查配置文件");
            return;
        }

        int syncCount = 0;
        for (WeWorkCorpProperties.CorpConfig corpConfig : corps) {
            try {
                syncCorpConfig(corpConfig);
                syncCount++;
            } catch (Exception e) {
                log.error("同步企业配置失败, corpId: {}", corpConfig.getCorpId(), e);
            }
        }

        log.info("企业配置同步完成，成功同步 {} 个企业", syncCount);
    }

    /**
     * 同步单个企业配置
     */
    private void syncCorpConfig(WeWorkCorpProperties.CorpConfig corpConfig) {
        String corpId = corpConfig.getCorpId();
        
        Optional<CorpInfo> existingCorpOpt = corplistRepo.findFirstByCorpid(corpId);
        
        CorpInfo corpInfo;
        if (existingCorpOpt.isPresent()) {
            // 更新现有配置
            corpInfo = existingCorpOpt.get();
            log.debug("更新企业配置: {}", corpId);
        } else {
            // 创建新配置
            corpInfo = new CorpInfo();
            corpInfo.setCorpid(corpId);
            log.info("创建新企业配置: {}", corpId);
        }

        // 更新配置信息
        corpInfo.setCorpname(corpConfig.getCorpName());
        corpInfo.setSecret(corpConfig.getSecret());
        corpInfo.setPrikey(corpConfig.getPrivateKey());
        corpInfo.setStatus(corpConfig.getEnabled() ? 1L : 0L);

        corplistRepo.save(corpInfo);
        log.info("企业配置已保存: {} - {}", corpId, corpConfig.getCorpName());
    }

    /**
     * 获取所有启用的企业配置
     */
    public List<WeWorkCorpProperties.CorpConfig> getEnabledCorps() {
        return weWorkCorpProperties.getCorps().stream()
                .filter(corp -> corp.getEnabled() != null && corp.getEnabled())
                .collect(Collectors.toList());
    }

    /**
     * 根据 corpId 获取配置
     */
    public Optional<WeWorkCorpProperties.CorpConfig> getCorpConfig(String corpId) {
        return weWorkCorpProperties.getCorps().stream()
                .filter(corp -> corp.getCorpId().equals(corpId))
                .findFirst();
    }

    /**
     * 刷新企业配置（重新从配置文件同步）
     */
    public void refreshCorpConfig() {
        log.info("手动刷新企业配置...");
        syncCorpConfigToDatabase();
    }
}

