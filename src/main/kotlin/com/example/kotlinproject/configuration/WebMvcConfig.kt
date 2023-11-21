package com.example.kotlinproject.configuration

import com.example.kotlinproject.auth.AuthInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableWebMvc
class WebMvcConfig(val authInterceptor: AuthInterceptor) : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry
            .addMapping("/**") // 모든 경로에 대해
            .allowedOrigins(
                "http://localhost:5000",
                "http://localhost:8080",
                "http://127.0.0.1:8080",
                "http://127.0.0.1:5000",
                "http://192.168.100.109:8080",
                "http://192.168.100.109:5000",
                "http://192.168.100.153:8080",
                "http://192.168.100.153:5000"
            )
            .allowedMethods("*")
            .allowedHeaders("*")
            .allowCredentials(true);
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(authInterceptor)
    }

}
