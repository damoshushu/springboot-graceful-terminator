package de.ra.springboot.graceful.test;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GracefulTestService {

	@RequestMapping("/singlesleep")
	public String singlesleep() throws InterruptedException {
		Thread.sleep(10000);
		return "finish";
	}
}
