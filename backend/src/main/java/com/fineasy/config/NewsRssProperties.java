package com.fineasy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "news.rss")
public class NewsRssProperties {

    private List<String> urls = new ArrayList<>();

    private boolean collectionEnabled = false;

    public List<String> getUrls() {
        return urls;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }

    public boolean isCollectionEnabled() {
        return collectionEnabled;
    }

    public void setCollectionEnabled(boolean collectionEnabled) {
        this.collectionEnabled = collectionEnabled;
    }
}
