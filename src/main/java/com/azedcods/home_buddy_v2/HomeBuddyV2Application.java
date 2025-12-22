package com.azedcods.home_buddy_v2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HomeBuddyV2Application {

	public static void main(String[] args) {
		SpringApplication.run(HomeBuddyV2Application.class, args);
	}

}
