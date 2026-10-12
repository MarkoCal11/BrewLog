package hr.tvz.brewlog.brewlogapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BrewlogappApplication {

	static void main(String[] args) {
		SpringApplication.run(BrewlogappApplication.class, args);
	}
}