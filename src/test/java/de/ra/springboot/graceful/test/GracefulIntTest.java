package de.ra.springboot.graceful.test;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import de.ra.springboot.graceful.GracefulShutdownHook;
import de.ra.springboot.graceful.GracefulSpringApplication;

@RunWith(SpringRunner.class)
@EnableAutoConfiguration
@ComponentScan
public class GracefulIntTest {

	private ConfigurableApplicationContext app;

	@Before
	public void setup() {
		System.setProperty("logging.level.root", "DEBUG");
		app = GracefulSpringApplication.run(GracefulIntTest.class, new String[] { "--server.port=0" });
	}

	@Test(timeout = 30000)
	public void gracefulProcessShutdownTest() throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		ExecutorService httpExecutor = Executors.newSingleThreadExecutor();
		Future<String> httpFuture = httpExecutor.submit(() -> {
			return restTemplate.getForObject(getUrl("singlesleep"), String.class);
		});
		// Sleep, otherwise the socket is closed, before connected
		Thread.sleep(1000);
		app.getBean(GracefulShutdownHook.class).run();
		String ret = httpFuture.get();
		assertEquals("finish", ret);
	}

	@Test(timeout = 30000)
	public void test503ErrorAndGraceful() throws InterruptedException, ExecutionException, URISyntaxException {
		RestTemplate restTemplate = new RestTemplate();
		ExecutorService httpExecutor = Executors.newSingleThreadExecutor();
		Future<String> httpFuture = httpExecutor.submit(() -> {
			return restTemplate.getForObject(getUrl("singlesleep"), String.class);
		});

		// Sleep, otherwise the socket is closed, before connected
		Thread.sleep(1000);

		ExecutorService shutDownExecutor = Executors.newSingleThreadExecutor();
		Future<Void> shutdownFuture = shutDownExecutor.submit(() -> {
			app.getBean(GracefulShutdownHook.class).run();
			return null;
		});

		Thread.sleep(500);

		// Create second request which should be rejected
		RestTemplate restTemplate2 = new RestTemplate();
		RequestEntity<String> request = RequestEntity.post(new URI(getUrl("singlesleep")))
				.accept(MediaType.APPLICATION_JSON).body("Hello");
		ResponseEntity<String> response = null;
		try {
			response = restTemplate2.exchange(request, String.class);
			assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
		} catch (HttpServerErrorException e) {
			assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getStatusCode());
		}
		// Check that first request was processed
		String ret = httpFuture.get();
		assertEquals("finish", ret);
		shutdownFuture.get();
	}

	private String getUrl(String service) {
		String port = app.getEnvironment().getProperty("local.server.port");
		return "http://localhost:" + port + "/" + service;
	}

}
