package org.nerve.boot.web.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import static org.nerve.boot.Const.EMPTY;

@Configuration
@ConditionalOnProperty(name ="nerve.cache.enable", matchIfMissing = true, havingValue = "true")
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfiguration extends CachingConfigurerSupport {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Resource
    CacheConfig cacheConfig;
	@Resource
	CacheProperties cacheProperties;

	@PostConstruct
	protected void init(){
		logger.info("[Cache] 初始化 CacheConfiguration (set nerve.cache.enable=false if you want to disable customed cache)");
		if(logger.isDebugEnabled())
			logger.debug("[Cache] {}", cacheConfig.caches);
	}

	@Bean
	@Override
	public CaffeineCacheManager cacheManager() {
		CaffeineCacheManager cacheManager = new CaffeineCacheManager();
		if (!CollectionUtils.isEmpty(cacheProperties.getCacheNames())) {
			cacheManager.setCacheNames(cacheProperties.getCacheNames());
		}

		cacheConfig.caches.forEach((name, spec) -> {
			boolean hasSpec = StringUtils.hasText(spec);
			if(!hasSpec)
				logger.warn("[Cache] 检测到 name={} 的cache没有定义 spec 属性...",name);

			Cache cache = Caffeine.from(hasSpec?spec: EMPTY).build();
			cacheManager.registerCustomCache(name, cache);
			if(logger.isDebugEnabled()) logger.debug("[Cache] added cache name={} {}", name, cache);
		});

		// 设置默认的缓存规则
		String spec = cacheProperties.getCaffeine().getSpec();
		if(StringUtils.hasText(spec)){
			cacheManager.setCaffeineSpec(CaffeineSpec.parse(spec));
			logger.info("[Cache] 默认缓存配置 = {}", spec);
		}

		return cacheManager;
	}
}
