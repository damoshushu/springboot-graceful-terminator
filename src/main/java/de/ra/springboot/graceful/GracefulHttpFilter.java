package de.ra.springboot.graceful;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * Counts the active HTTP threads. Returns 503 if shutdown is enabled
 * 
 * @author ramato
 *
 */
@Component
public class GracefulHttpFilter implements Filter {

	private static final Logger LOG = LoggerFactory.getLogger(GracefulHttpFilter.class);
	private static final AtomicLongFieldUpdater<GracefulHttpFilter> activeRequestsUpdater = AtomicLongFieldUpdater
			.newUpdater(GracefulHttpFilter.class, "activeRequests");
	@SuppressWarnings("unused")
	private volatile long activeRequests;
	private volatile boolean shutdown;
	private volatile CountDownLatch latch;
	private int timeout;

	/**
	 * Processes the requests. Return HTTP 503 if shutdown enabled, or count the
	 * requests and process
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if (shutdown) {
			HttpServletResponse resp = (HttpServletResponse) response;
			resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "SpringBoot shutting down");
			LOG.debug("Rejected Request; shutdown running");
		} else {
			// Service running, count request and process
			activeRequestsUpdater.incrementAndGet(this);
			try {
				chain.doFilter(request, response);
			} finally {
				if (shutdown) {
					getCountDownLatch().countDown();
					LOG.debug("Shutdown was running. Request processed gracefully");
				} else {
					LOG.debug("Request processed");
				}
				activeRequestsUpdater.decrementAndGet(this);
			}
		}
	}

	/**
	 * Creates and/or returns the CountDownLatch
	 * 
	 * @return
	 */
	private synchronized CountDownLatch getCountDownLatch() {
		if (latch == null) {
			try {
				latch = new CountDownLatch((int) activeRequestsUpdater.get(this));
			} catch (IllegalArgumentException e) {
				LOG.error("activeRequestsUpdater is less than zero. Using zero instead", e);
				latch = new CountDownLatch(0);
			}
		}
		return latch;
	}

	/**
	 * Enables the shutdown logic. All upcoming requests will be rejected with
	 * HTTP 503 / Waits until all running requests are finished
	 * 
	 * @throws InterruptedException
	 */
	public void shutdown() throws InterruptedException {
		this.shutdown = true;
		getCountDownLatch().await(timeout, TimeUnit.MILLISECONDS);
	}

	/**
	 * Register the Filter with SpringBoot FilterRegistrationBean
	 * 
	 * @return
	 */
	@Bean
	public FilterRegistrationBean myFilterBean() {
		final FilterRegistrationBean filterRegBean = new FilterRegistrationBean();
		filterRegBean.setFilter(this);
		filterRegBean.addUrlPatterns("/*");
		filterRegBean.setEnabled(Boolean.TRUE);
		filterRegBean.setName("Graceful HTTP Filter");
		filterRegBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
		filterRegBean.setAsyncSupported(Boolean.TRUE);
		LOG.debug("Graceful HTTP Filter registered");
		return filterRegBean;
	}

	public void init(FilterConfig filterConfig) throws ServletException {

	}

	public void destroy() {

	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

}
