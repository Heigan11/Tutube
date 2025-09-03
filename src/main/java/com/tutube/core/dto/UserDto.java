package com.tutube.core.dto;


import com.tutube.core.conrollers.UserController;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private String userName;
    private String firstName;
    private String lastName;
    private Double age;
    private Double factAge;
    private int level;
    private Double successRate;
    private Integer attemptsCount;

    public static UserDto convertToUserDto(User user) {
        return UserDto.builder()
                .userName(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .age(user.getAge())
                .factAge(user.getFactAge())
                .level(user.getLevel())
                .successRate(user.getSuccessRate())
                .attemptsCount(user.getAttemptsCount())
        .build();
    }
}


