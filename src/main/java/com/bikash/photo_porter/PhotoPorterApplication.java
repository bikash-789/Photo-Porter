package com.bikash.photo_porter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class PhotoPorterApplication {
	public static void main(String[] args) {
		SpringApplication.run(PhotoPorterApplication.class, args);
	}
}
