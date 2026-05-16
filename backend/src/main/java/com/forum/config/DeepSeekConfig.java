package com.forum.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "deepseek.api")
public class DeepSeekConfig {

    private String key;
    private String url = "https://api.deepseek.com/chat/completions";
    private String model = "deepseek-v4-flash";

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}
