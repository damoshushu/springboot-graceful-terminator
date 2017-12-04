package de.ra.springboot.graceful;

import javax.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * GracefulShutdown Properties
 * 
 * @author ramato
 *
 */
@ConfigurationProperties(prefix = "graceful")
public class GracefulProperties {

	private Timeout timeout = new Timeout();

	/**
	 * Timeouts
	 * 
	 * @author ramato
	 *
	 */
	public static class Timeout {

		/**
		 * Timeout (in ms) to wait for running Requests to complete
		 * 
		 */
		@Min(100)
		private Integer graceful = 10000;

		/**
		 * Timeout (in ms) to wait until all containers are shutdown. Bust be
		 * greater than http timeout
		 * 
		 */
		@Min(200)
		private Integer container = 15000;

		public Integer getGraceful() {
			return graceful;
		}

		public void setGraceful(Integer graceful) {
			this.graceful = graceful;
		}

		public Integer getContainer() {
			return container;
		}

		public void setContainer(Integer container) {
			this.container = container;
		}

	}

	public Timeout getTimeout() {
		return timeout;
	}

	public void setTimeout(Timeout timeout) {
		this.timeout = timeout;
	}

}
