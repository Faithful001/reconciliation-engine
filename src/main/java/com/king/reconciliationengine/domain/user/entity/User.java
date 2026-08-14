package com.king.reconciliationengine.domain.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Schema(description = "User entity detail")
public class User {
    @Id
    @GeneratedValue(strategy= GenerationType.UUID)
    @Schema(description = "Unique user identifier", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    private UUID id;

    @Column(nullable = false)
    @Schema(description = "User's first name", example = "John")
    private String firstName;

    @Column(nullable = false)
    @Schema(description = "User's last name", example = "Doe")
    private String lastName;

    @Column(nullable = false, unique = true)
    @Schema(description = "User's email address", example = "johndoe@example.com")
    private String email;

    @Column(nullable = false)
    @JsonIgnore
    @Schema(hidden = true)
    private String password;

    @Column(nullable = false)
    @Schema(description = "User creation timestamp")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Schema(description = "User last update timestamp")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
