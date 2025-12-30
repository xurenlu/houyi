package com.ruoran.houyi.sync;

import com.ruoran.houyi.config.DashController;
import com.ruoran.houyi.model.CorpInfo;
import com.ruoran.houyi.pojo.YapiResult;
import com.ruoran.houyi.repo.CorplistRepo;
import com.ruoran.houyi.service.CorpInfoApi;
import com.ruoran.houyi.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Optional;

/**
 * @author lh
 */

@Component
@Slf4j
public class SyncCorpService {
    @Value("${corpUrl:}")
    private String corpUrl;

    @Autowired
    private CorplistRepo corplistRepo;

    @Resource
    CorpInfoApi corpInfoApi;

    @Scheduled(fixedRate = 3600 * 24 * 1000)
    public void sync() {
        // corpUrl 已废弃，企业配置现在从 wework-corps.yml 读取
        if (StringUtils.isEmpty(corpUrl)) {
            log.debug("corpUrl 未配置，跳过企业信息同步（企业配置现在从 wework-corps.yml 读取）");
            return;
        }
        
        String json = HttpClientUtil.doGet(corpUrl);
        if (StringUtils.isNotEmpty(json)) {
            JSONObject jsonObject = new JSONObject(json);
            if (jsonObject.has("data")) {
                JSONArray datas = jsonObject.getJSONArray("data");
                for (int i = 0; i < datas.length(); i++) {
                    JSONObject data = datas.getJSONObject(i);
                    if (data.has("wxCorpid")) {
                        String wxCorpid = data.getString("wxCorpid");
                        Optional<CorpInfo> optionalCorpInfo = corplistRepo.findFirstByCorpid(wxCorpid);
                        if (!optionalCorpInfo.isPresent()) {
                            CorpInfo corpInfo = new CorpInfo();
                            YapiResult result = new YapiResult();
                            try {
                                result = corpInfoApi.getCorpInfo(wxCorpid);
                            } catch (Exception e) {
                                log.error("请求crop数据出错：", e);
                            }
                            DashController.saveCorp(wxCorpid, corpInfo, result, corplistRepo);
                        }
                    }
                }
            }
        }
    }
}
