package by.innowise.authenticationservice.controller;

import by.innowise.authenticationservice.dto.request.LoginRequestDto;
import by.innowise.authenticationservice.dto.request.RefreshTokenRequestDto;
import by.innowise.authenticationservice.dto.request.SignUpRequestDto;
import by.innowise.authenticationservice.dto.request.TokenValidationRequestDto;
import by.innowise.authenticationservice.dto.response.RegistrationResponseDto;
import by.innowise.authenticationservice.dto.response.TokenResponseDto;
import by.innowise.authenticationservice.dto.response.TokenValidationResponseDto;
import by.innowise.authenticationservice.service.RegistrationService;
import by.innowise.authenticationservice.service.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final TokenService tokenService;
    private final RegistrationService registrationService;

    @PostMapping("/sign-up")
    public ResponseEntity<RegistrationResponseDto> signUp(
            @Valid @RequestBody SignUpRequestDto request
    ) {
        RegistrationResponseDto response =
                registrationService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/token")
    public ResponseEntity<TokenResponseDto> createToken(
            @Valid @RequestBody LoginRequestDto request
    ) {
        TokenResponseDto response =
                tokenService.createToken(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponseDto> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDto request
    ) {
        TokenResponseDto response =
                tokenService.refreshToken(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate")
    public ResponseEntity<TokenValidationResponseDto> validateToken(
            @Valid @RequestBody TokenValidationRequestDto request
    ) {
        TokenValidationResponseDto response =
                tokenService.validateToken(request);

        return ResponseEntity.ok(response);
    }
}
