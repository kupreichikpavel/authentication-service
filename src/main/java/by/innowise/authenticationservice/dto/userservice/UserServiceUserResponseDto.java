package by.innowise.authenticationservice.dto.userservice;

import java.time.Instant;
import java.time.LocalDate;

public record UserServiceUserResponseDto(
    Long id,
    String name,
    String surname,
    LocalDate birthDate,
    String email,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {
}
