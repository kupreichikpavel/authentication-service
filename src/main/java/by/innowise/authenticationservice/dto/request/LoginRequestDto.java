package by.innowise.authenticationservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(

        @NotBlank(message = "Login must not be blank")
        String login,

        @NotBlank(message = "Password must not be blank")
        String password
) {
}
