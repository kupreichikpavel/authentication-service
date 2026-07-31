package by.innowise.authenticationservice.dto.userservice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserServiceUserResponseDto(
        Long id,
        Boolean active
) {

    public UserServiceUserResponseDto(Long id) {
        this(id, null);
    }
}
