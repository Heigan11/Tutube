package com.tutube.core.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
@Schema(description = "User entity")
public class User implements UserDetails {

    @Id
    @Schema(description = "User ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @JsonProperty("userName")
    @Schema(description = "Username (email address)", example = "testUser@test.com", required = true)
    private String userName;

    @Schema(description = "Password", example = "encryptedPassword")
    private String password;

    @Schema(description = "First name", example = "John")
    private String firstName;

    @Schema(description = "Last name", example = "Doe")
    private String lastName;

    @Schema(description = "Birth date", example = "1990-01-01")
    private LocalDate birthDate;

    @Schema(description = "Calculated age", example = "33.5", accessMode = Schema.AccessMode.READ_ONLY)
    private Double age;

    @Schema(description = "Factual age", example = "34.0")
    private Double factAge;

    @Schema(description = "User level", example = "1")
    private int level = 1;

    @Schema(description = "Success rate", example = "85.5")
    private Double successRate = 0.0;

    @Schema(description = "Attempts count", example = "10")
    private Integer attemptsCount = 0;

    // Конструктор для регистрации
    public User(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getUsername() {
        return userName;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
