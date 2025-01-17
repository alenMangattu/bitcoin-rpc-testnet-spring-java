package com.coinbase.cb;

import com.coinbase.cb.configuration.security.CustomUserDetails;
import com.coinbase.cb.repository.UserRespository;
import com.coinbase.cb.util.validators.RegisterInputValidator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class CbApplication {

	private final UserRespository userRespository;

	public CbApplication(UserRespository userRespository) {
		this.userRespository = userRespository;
	}

	public static void main(String[] args) {
		SpringApplication.run(CbApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void initializeValidator() {
		RegisterInputValidator.setUserRepository(this.userRespository);
	}

	@GetMapping("/sono") 
	public String helloWforld() {
		CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return String.format("Current user: %s %s (%s)", 
			userDetails.getFirstName(), 
			userDetails.getLastName(), 
			userDetails.getUsername());
	}
}
