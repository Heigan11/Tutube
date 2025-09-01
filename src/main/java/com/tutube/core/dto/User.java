package com.tutube.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class User {

    @Id
    private Long id;

    private String firstName;
    private String lastName;
    private double age;

    private Double factAge = null;
    private int level = 1;
    private Double successRate = 0.0;
    private Integer attemptsCount = 0;
}
