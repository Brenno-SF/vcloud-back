package com.bsf.vcloud.controller;


import com.bsf.vcloud.dtos.login.LoginRequest;
import com.bsf.vcloud.dtos.login.LoginResponse;
import com.bsf.vcloud.sevice.LoginService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.naming.AuthenticationException;

@RestController
@RequestMapping("/login")
public class LoginController {
    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping()
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) throws AuthenticationException {
        return ResponseEntity.ok(loginService.login(loginRequest)) ;
    }

}
