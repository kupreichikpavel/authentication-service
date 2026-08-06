package by.innowise.authenticationservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TokenValidationRequestDto(

    @NotBlank(message = "Token must not be blank")
    String token
) {

}
