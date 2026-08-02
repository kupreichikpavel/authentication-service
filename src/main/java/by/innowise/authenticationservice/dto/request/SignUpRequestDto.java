package by.innowise.authenticationservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SignUpRequestDto(

        @NotBlank(message = "Login must not be blank")
        @Size(
                max = 255,
                message = "Login must not exceed 255 characters"
        )
        String login,

        @NotBlank(message = "Password must not be blank")
        @Size(
                min = 8,
                max = 128,
                message = "Password length must be between 8 and 128 characters"
        )
        String password,

        @NotBlank(message = "Name must not be blank")
        @Size(
                max = 255,
                message = "Name must not exceed 255 characters"
        )
        String name,

        @NotBlank(message = "Surname must not be blank")
        @Size(
                max = 255,
                message = "Surname must not exceed 255 characters"
        )
        String surname,

        @NotNull(message = "Birth date must not be null")
        @Past(message = "Birth date must be in the past")
        LocalDate birthDate,

        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must have a valid format")
        @Size(
                max = 255,
                message = "Email must not exceed 255 characters"
        )
        String email

) {
}
