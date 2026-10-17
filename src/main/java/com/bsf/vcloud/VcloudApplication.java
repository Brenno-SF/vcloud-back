package com.bsf.vcloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class VcloudApplication {

	public static void main(String[] args) {
		SpringApplication.run(VcloudApplication.class, args);
	}

}
