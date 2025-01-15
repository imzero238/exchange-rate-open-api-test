package com.nayoung.exchangerateopenapitest.swagger;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "금융 서비스",
                description = "이체, 결제, 환율 관련 API 제공",
                version = "0.0.1"
        )
)
@Configuration
public class OpenApiDefinition {
}
