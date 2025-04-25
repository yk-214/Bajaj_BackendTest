package com.example.mutualfollowers.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NthLevelRequest {
    private Integer n;
    private Integer findId;
    private List<ApiUser> users;
} 