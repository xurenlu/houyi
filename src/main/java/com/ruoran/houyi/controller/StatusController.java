package com.ruoran.houyi.controller;

import com.ruoran.houyi.sync.SyncCorpService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

/**
 * @author lh
 */
@RestController
public class StatusController {

    @Resource
     SyncCorpService syncCorpService;

    @GetMapping("status")
    public Integer status(){
        return 200;
    }

    @GetMapping("/sync")
    public Object go(){
        syncCorpService.sync();
        return "ok";
    }
}
