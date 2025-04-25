package com.example.mutualfollowers.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookResponse {
    private boolean success;
    private String message;
    private ResponsePayload data;
}
