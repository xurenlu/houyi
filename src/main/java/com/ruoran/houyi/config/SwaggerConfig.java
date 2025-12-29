package com.ruoran.houyi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI 配置
 * 使用 SpringDoc OpenAPI 3.0 (支持 Spring Boot 3)
 *
 * @author renlu
 * @author refactored
 */
@Configuration
public class SwaggerConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("企业微信会话存档 API")
                        .version("1.0.0")
                        .description("企业微信会话存档系统 API 文档"));
    }
}
