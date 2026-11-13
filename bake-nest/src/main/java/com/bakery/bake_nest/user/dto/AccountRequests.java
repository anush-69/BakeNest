package com.bakery.bake_nest.user.dto;

import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

// Requests expose only fields customers are allowed to change.
public class AccountRequests {
    @Getter
    @Setter
    public static class Profile {
        @NotBlank
        @Email
        @Size(max = 255)
        private String email;

        @NotBlank
        @Size(max = 100)
        private String firstName;

        @NotBlank
        @Size(max = 100)
        private String lastName;

        @Pattern(regexp = "[+0-9 ()-]{0,30}")
        private String phone;
    }

    @Getter
    @Setter
    public static class Registration extends Profile {
        @NotBlank
        @Pattern(regexp = "[A-Za-z0-9_.-]{3,50}")
        private String username;

        @NotBlank
        @Size(min = 8, max = 72)
        private String password;
    }

    @Getter
    @Setter
    public static class PasswordChange {
        @NotBlank private String currentPassword;

        @NotBlank
        @Size(min = 8, max = 72)
        private String newPassword;
    }
}
