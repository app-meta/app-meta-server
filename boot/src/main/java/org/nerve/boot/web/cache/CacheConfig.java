package org.nerve.boot.web.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "nerve.cache")
public class CacheConfig {
	boolean enable=false;
	Map<String, String> caches = new HashMap<>();

	public boolean isEnable() {
		return enable;
	}

	public CacheConfig setEnable(boolean enable) {
		this.enable = enable;
		return this;
	}

	public Map<String, String> getCaches() {
		return caches;
	}

	public CacheConfig setCaches(Map<String, String> caches) {
		this.caches = caches;
		return this;
	}
}

