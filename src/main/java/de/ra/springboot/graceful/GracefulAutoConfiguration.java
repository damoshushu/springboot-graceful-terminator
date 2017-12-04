package de.ra.springboot.graceful;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the graceful Beans
 * 
 * @author ramato
 *
 */
@Configuration
@EnableConfigurationProperties(GracefulProperties.class)
@ConditionalOnClass(GracefulSpringApplication.class)
public class GracefulAutoConfiguration {

	@Autowired
	GracefulProperties properties;

	@Bean
	@ConditionalOnMissingBean
	public GracefulHttpFilter gracefulHttpFiler() {
		GracefulHttpFilter filter = new GracefulHttpFilter();
		filter.setTimeout(properties.getTimeout().getGraceful());
		return filter;
	}

	@Bean
	@ConditionalOnMissingBean
	public GracefulShutdownHook gracefulShutdownHook() {
		GracefulShutdownHook hook = new GracefulShutdownHook();
		hook.setTimeout(properties.getTimeout().getContainer());
		return hook;
	}

}
