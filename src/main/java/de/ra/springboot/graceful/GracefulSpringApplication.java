package de.ra.springboot.graceful;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Creates a SpringBoot Application and enables the graceful shutdown hooks
 * 
 * @author ramato
 *
 */
public class GracefulSpringApplication {

	private static final Logger LOG = LoggerFactory.getLogger(GracefulSpringApplication.class);

	/**
	 * Creates a new SpringBoot Application instance and registers the
	 * GracefulShutdownHook
	 * 
	 * @param appClazz
	 * @param args
	 * @return
	 */
	public static ConfigurableApplicationContext run(Class<?> appClazz, String... args) {
		LOG.debug("Starting Graceful SpringBoot Application");
		SpringApplication app = new SpringApplication(appClazz);
		app.setRegisterShutdownHook(false);
		ConfigurableApplicationContext applicationContext = app.run(args);
		LOG.debug("Register Shutdown Hook");
		GracefulShutdownHook hook = applicationContext.getBean(GracefulShutdownHook.class);
		hook.init(applicationContext);
		Runtime.getRuntime().addShutdownHook(new Thread(hook));
		LOG.debug("Shutdown Hook registered");
		return applicationContext;
	}

}
