package com.temperies;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan(basePackages = "com.temperies")
public class TandilProxyApplication {

	public static void main(String[] args) {
		SpringApplication.run(TandilProxyApplication.class, args);
	}

}
