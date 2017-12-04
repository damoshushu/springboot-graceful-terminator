package de.ra.springboot.graceful;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Handles the JVM, SpringBoot Application Shutdown gracefully
 * 
 * Custom Implementation for:
 * https://docs.openshift.com/container-platform/3.6/dev_guide/deployments/advanced_deployment_strategies.html#graceful-termination
 * 
 * @author ramato
 *
 */
@Component
public class GracefulShutdownHook implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(GracefulShutdownHook.class);
	private final List<EmbeddedServletContainer> embeddedContainers = new ArrayList<>();
	private ConfigurableApplicationContext applicationContext;
	private int timeout;
		
	@Autowired
	private GracefulHttpFilter filter;

	/**
	 * Initializes the Application Context
	 * 
	 * @param applicationContext
	 */
	public void init(ConfigurableApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
//		this.filter = this.applicationContext.getBean(GracefulHttpFilter.class);
//		this.properties = this.applicationContext.getBean(GracefulProperties.class);
		LOG.debug("ApplicationContext initialized");
	}

	/**
	 * Is called by the JVM to shutdown the application (e.g. when receiving
	 * SIGTERM). First enable the shutdown logic in GracefulHttpFilter to return
	 * HTTP Code 503 for new incoming requests. This is needed, because Tomcat
	 * doesn't instantly close the sockets when pausing the connector:
	 * https://github.com/spring-projects/spring-boot/issues/4657 Returning 503
	 * will will force the Loadbalancer to forward new requests to other pods.
	 * Then it will wait until all open requests are handled. Then the
	 * application context ist shutting down.
	 * 
	 */
	public void run() {
		try {
			LOG.info("Graceful shutdown triggered");
			// Shutdown HTTP
			ExecutorService httpExecutor = Executors.newSingleThreadExecutor();
			Future<Void> httpFuture = httpExecutor.submit(() -> {
				shutdownHttpFilter();
				shutdownHTTPConnector();
				return null;
			});
			try {
				httpFuture.get(timeout, TimeUnit.MILLISECONDS);
				LOG.info("HTTP shutdown finished");
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				LOG.error("HTTP graceful shutdown failed", e);
			}
			// TODO Shutdown JMS

		} finally {
			LOG.info("Shutdown ApplicationContext");
			shutdownApplication();
		}
	}

	/**
	 * Set returncode 503 for new requests. And wait for running requests to
	 * complete.
	 */
	private void shutdownHttpFilter() {
		try {
			LOG.debug("Trigger HTTPFilter shutdown");
			filter.shutdown();
			LOG.debug("Trigger HTTPFilter finished");
		} catch (InterruptedException e) {
			LOG.error("shutdownHttpFilter failed", e);
		}
	}

	/**
	 * Stops the HTTP Connector
	 */
	private void shutdownHTTPConnector() {
		LOG.debug("Shutting down embedded containers");
		for (EmbeddedServletContainer embeddedServletContainer : embeddedContainers) {
			embeddedServletContainer.stop();
		}
		LOG.debug("Shutting down embedded containers finishes");
	}

	/**
	 * Close the SpringBoot Application Context
	 */
	private void shutdownApplication() {
		applicationContext.close();
	}

	@EventListener
	public synchronized void onContainerInitialized(EmbeddedServletContainerInitializedEvent event) {
		embeddedContainers.add(event.getEmbeddedServletContainer());
		LOG.debug("EmbeddedServletContainer registered");
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

}
