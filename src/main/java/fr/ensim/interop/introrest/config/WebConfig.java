package fr.ensim.interop.introrest.config;

import fr.ensim.interop.introrest.security.ApiTokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private ApiTokenInterceptor apiTokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //registry.addInterceptor(apiTokenInterceptor)
         //     .addPathPatterns("/joke/**"); // protège toutes les routes /joke/*
    }
}
