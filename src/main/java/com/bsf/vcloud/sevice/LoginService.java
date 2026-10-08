package com.bsf.vcloud.sevice;


import com.bsf.vcloud.dtos.login.LoginRequest;
import com.bsf.vcloud.dtos.login.LoginResponse;
import com.bsf.vcloud.entity.Users;
import com.bsf.vcloud.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import javax.naming.AuthenticationException;
import java.time.Instant;
import java.util.Optional;

@Service
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    public LoginService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
    }

    public LoginResponse login(LoginRequest loginUserRequestDTO) throws AuthenticationException {

        Optional<Users> optUser = userRepository.findByEmail(loginUserRequestDTO.email());

        if(optUser.isEmpty() || !isPasswordValid(loginUserRequestDTO.password(), optUser.get().getPassword())) {
            throw new AuthenticationException("Email/Password incorrect");
        }

        Users user =  optUser.get();


        long expirationTime = System.currentTimeMillis() + 3600000; // 1 hour expiration time

        JwtClaimsSet jwt = JwtClaimsSet.builder()
                .issuer("v-cloud")
                .subject(user.getEmail())
                .expiresAt(Instant.now().plusSeconds(expirationTime))
                .issuedAt(Instant.now())
                .claim("userId", user.getId())
                .claim("email", user.getEmail())
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(jwt)).getTokenValue();

        return new LoginResponse(token, expirationTime);

    }

    private boolean isPasswordValid(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
