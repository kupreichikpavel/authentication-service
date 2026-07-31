package by.innowise.authenticationservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequestDto(

        @NotBlank(message = "Refresh token must not be blank")
        String refreshToken
) {
}
