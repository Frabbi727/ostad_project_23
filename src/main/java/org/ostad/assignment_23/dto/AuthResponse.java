package org.ostad.assignment_23.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String email;
    private String message;

    public AuthResponse(String message) {
        this.message = message;
    }
}
