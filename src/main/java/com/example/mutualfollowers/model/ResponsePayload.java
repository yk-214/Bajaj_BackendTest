package com.example.mutualfollowers.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponsePayload {
    private String requestUserId;
    private String requestUsername;
    private List<User> mutualFollowers;
    private int count;
}
