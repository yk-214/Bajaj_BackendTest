package com.example.mutualfollowers.service;

import com.example.mutualfollowers.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${api.base-url:https://bfhldevapigw.healthrx.co.in/hiring}")
    private String apiBaseUrl;

    @Value("${api.registration.name:John Doe}")
    private String name;

    @Value("${api.registration.regNo:REG12347}")
    private String regNo;

    @Value("${api.registration.email:john@example.com}")
    private String email;

    public WebhookGenerateResponse generateWebhook() {
        try {
            log.info("Generating webhook for registration: {}", regNo);
            
            WebhookGenerateRequest request = new WebhookGenerateRequest(name, regNo, email);
            
            RequestBody requestBody = RequestBody.create(
                    objectMapper.writeValueAsString(request),
                    MediaType.parse("application/json")
            );
            
            Request httpRequest = new Request.Builder()
                    .url(apiBaseUrl + "/generateWebhook")
                    .post(requestBody)
                    .header("Content-Type", "application/json")
                    .build();
            
            try (Response response = okHttpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected response code: " + response);
                }
                
                String responseBody = response.body().string();
                log.debug("Response body: {}", responseBody);
                WebhookGenerateResponse webhookResponse = objectMapper.readValue(responseBody, WebhookGenerateResponse.class);
                log.info("Successfully generated webhook. URL: {}", webhookResponse.getWebhook());
                return webhookResponse;
            }
        } catch (Exception e) {
            log.error("Error generating webhook", e);
            throw new RuntimeException("Failed to generate webhook", e);
        }
    }
    
    @Retryable(value = Exception.class, maxAttempts = 4, backoff = @Backoff(delay = 1000))
    public void sendWebhookResult(String webhookUrl, String accessToken, Object result) {
        try {
            log.info("Sending webhook result to: {}", webhookUrl);
            
            RequestBody requestBody = RequestBody.create(
                    objectMapper.writeValueAsString(result),
                    MediaType.parse("application/json")
            );
            
            Request request = new Request.Builder()
                    .url(webhookUrl)
                    .post(requestBody)
                    .header("Content-Type", "application/json")
                    .header("Authorization", accessToken)
                    .build();
            
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Webhook call failed with response code: {}", response.code());
                    throw new IOException("Webhook call failed: " + response);
                }
                
                log.info("Successfully sent webhook result. Response code: {}", response.code());
            }
        } catch (Exception e) {
            log.error("Error sending webhook result", e);
            throw new RuntimeException("Failed to send webhook result", e);
        }
    }
    
    public WebhookResult processProblem(WebhookGenerateResponse webhookResponse) {
        try {
            // Log the entire response for debugging
            log.debug("Processing webhook response: {}", objectMapper.writeValueAsString(webhookResponse));
            
            // Determine which problem to solve based on the regNo
            int lastTwoDigits = Integer.parseInt(regNo.substring(regNo.length() - 2));
            boolean isOdd = lastTwoDigits % 2 != 0;
            
            WebhookResult result = new WebhookResult();
            result.setRegNo(regNo);
            
            if (isOdd) {
                // Process mutual followers (Question 1)
                log.info("Processing mutual followers problem (Question 1)");
                result.setOutcome(findMutualFollowers(webhookResponse.getData()));
            } else {
                // Process nth level followers (Question 2)
                log.info("Processing nth level followers problem (Question 2)");
                result.setOutcome(findNthLevelFollowers(webhookResponse.getData()));
            }
            
            return result;
        } catch (Exception e) {
            log.error("Error processing problem", e);
            throw new RuntimeException("Failed to process problem", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private List<List<Integer>> findMutualFollowers(Map<String, Object> data) {
        try {
            log.debug("Finding mutual followers from data: {}", objectMapper.writeValueAsString(data));
            
            List<ApiUser> users = new ArrayList<>();
            
            // Handle different data formats
            Object usersObj = data.get("users");
            if (usersObj instanceof List) {
                List<Map<String, Object>> usersList = (List<Map<String, Object>>) usersObj;
                for (Map<String, Object> userMap : usersList) {
                    ApiUser user = new ApiUser();
                    user.setId((Integer) userMap.get("id"));
                    user.setName((String) userMap.get("name"));
                    user.setFollows((List<Integer>) userMap.get("follows"));
                    users.add(user);
                }
            } else if (usersObj instanceof Map) {
                Map<String, Object> usersMap = (Map<String, Object>) usersObj;
                if (usersMap.containsKey("users") && usersMap.get("users") instanceof List) {
                    List<Map<String, Object>> usersList = (List<Map<String, Object>>) usersMap.get("users");
                    for (Map<String, Object> userMap : usersList) {
                        ApiUser user = new ApiUser();
                        user.setId((Integer) userMap.get("id"));
                        user.setName((String) userMap.get("name"));
                        user.setFollows((List<Integer>) userMap.get("follows"));
                        users.add(user);
                    }
                } else {
                    throw new RuntimeException("Unexpected users data format");
                }
            } else {
                throw new RuntimeException("Unexpected users data format");
            }
            
            // Create a map of user ID to follows list for efficient lookup
            Map<Integer, Set<Integer>> userFollows = new HashMap<>();
            for (ApiUser user : users) {
                userFollows.put(user.getId(), new HashSet<>(user.getFollows()));
            }
            
            // Find mutual follows (where user A follows user B and user B follows user A)
            Set<List<Integer>> mutualFollowsSet = new HashSet<>();
            for (ApiUser user : users) {
                for (Integer followId : user.getFollows()) {
                    Set<Integer> followFollows = userFollows.get(followId);
                    if (followFollows != null && followFollows.contains(user.getId())) {
                        // Mutual follow found - add as [min, max] pair
                        List<Integer> pair = Arrays.asList(
                                Math.min(user.getId(), followId),
                                Math.max(user.getId(), followId)
                        );
                        mutualFollowsSet.add(pair);
                    }
                }
            }
            
            // Convert to list and sort
            List<List<Integer>> result = new ArrayList<>(mutualFollowsSet);
            result.sort((a, b) -> {
                int compare = a.get(0).compareTo(b.get(0));
                if (compare == 0) {
                    return a.get(1).compareTo(b.get(1));
                }
                return compare;
            });
            
            log.info("Found {} mutual follower pairs", result.size());
            return result;
        } catch (Exception e) {
            log.error("Error finding mutual followers", e);
            throw new RuntimeException("Failed to process mutual followers", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private List<Integer> findNthLevelFollowers(Map<String, Object> data) {
        try {
            log.debug("Finding nth level followers from data: {}", objectMapper.writeValueAsString(data));
            
            // Extract users data in different formats
            Map<String, Object> usersData;
            if (data.containsKey("users") && data.get("users") instanceof Map) {
                usersData = (Map<String, Object>) data.get("users");
            } else {
                usersData = data;
            }
            
            Integer n = (Integer) usersData.get("n");
            Integer findId = (Integer) usersData.get("findId");
            
            // Get users list in different formats
            List<ApiUser> users = new ArrayList<>();
            Object usersObj = usersData.get("users");
            
            if (usersObj instanceof List) {
                List<Map<String, Object>> usersList = (List<Map<String, Object>>) usersObj;
                for (Map<String, Object> userMap : usersList) {
                    ApiUser user = new ApiUser();
                    user.setId((Integer) userMap.get("id"));
                    user.setName((String) userMap.get("name"));
                    user.setFollows((List<Integer>) userMap.get("follows"));
                    users.add(user);
                }
            } else {
                throw new RuntimeException("Unexpected users data format");
            }
            
            // Create a map of user ID to user object for efficient lookup
            Map<Integer, ApiUser> userMap = new HashMap<>();
            for (ApiUser user : users) {
                userMap.put(user.getId(), user);
            }
            
            // BFS to find nth level followers
            Set<Integer> visited = new HashSet<>();
            Queue<Integer> queue = new LinkedList<>();
            Map<Integer, Integer> levelMap = new HashMap<>();
            
            // Start with the findId
            queue.add(findId);
            visited.add(findId);
            levelMap.put(findId, 0);
            
            while (!queue.isEmpty()) {
                Integer currentId = queue.poll();
                Integer currentLevel = levelMap.get(currentId);
                
                if (currentLevel == n) {
                    continue; // Don't explore beyond level n
                }
                
                ApiUser currentUser = userMap.get(currentId);
                if (currentUser == null || currentUser.getFollows() == null) {
                    continue;
                }
                
                for (Integer followId : currentUser.getFollows()) {
                    if (!visited.contains(followId)) {
                        visited.add(followId);
                        queue.add(followId);
                        levelMap.put(followId, currentLevel + 1);
                    }
                }
            }
            
            // Find all users at exactly level n
            List<Integer> nthLevelFollowers = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : levelMap.entrySet()) {
                if (entry.getValue() == n) {
                    nthLevelFollowers.add(entry.getKey());
                }
            }
            
            // Sort the result
            Collections.sort(nthLevelFollowers);
            
            log.info("Found {} followers at level {}", nthLevelFollowers.size(), n);
            return nthLevelFollowers;
        } catch (Exception e) {
            log.error("Error finding nth level followers", e);
            throw new RuntimeException("Failed to process nth level followers", e);
        }
    }
}
