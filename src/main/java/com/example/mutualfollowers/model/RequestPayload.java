package com.example.mutualfollowers.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestPayload {
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotBlank(message = "Username is required")
    private String username;
}
