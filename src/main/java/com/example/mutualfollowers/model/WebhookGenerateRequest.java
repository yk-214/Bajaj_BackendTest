package com.example.mutualfollowers.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookGenerateRequest {
    private String name;
    private String regNo;
    private String email;
} 