package scoula.coin.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Trading Bot API 문서")
                        .version("1.0.0")
                        .description("Trading Bot API")
                        .contact(new Contact()
                                .name("Jisu Gim")
                                .email("jisu0259@naver.com"))
                );
    }
}
