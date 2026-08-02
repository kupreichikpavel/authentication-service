package by.innowise.authenticationservice.dto.userservice;

import java.time.LocalDate;

public record UserServiceCreateRequestDto(
    String name,
    String surname,
    LocalDate birthDate,
    String email
) {

}
