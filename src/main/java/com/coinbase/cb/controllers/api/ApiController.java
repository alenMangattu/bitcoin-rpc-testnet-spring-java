package com.coinbase.cb.controllers.api;

import com.coinbase.cb.controllers.api.api_services.LoginInController;
import com.coinbase.cb.controllers.api.api_services.RegisterController;
import com.coinbase.cb.models.DTOs.UserDTO;
import com.coinbase.cb.models.DbTables.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/api/v1")
public class ApiController {

    private final RegisterController signUpController;
    private final LoginInController loginController;
    
    public ApiController(RegisterController signUpController, 
                        LoginInController loginController) {
        this.signUpController = signUpController;
        this.loginController = loginController;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<UserDTO> signup(@RequestBody User user) {
        return signUpController.execute(user);
    }

    @PostMapping("/auth/login")
    public ResponseEntity<UserDTO> login(@RequestBody User user) {
        return loginController.execute(user);
    }
}
