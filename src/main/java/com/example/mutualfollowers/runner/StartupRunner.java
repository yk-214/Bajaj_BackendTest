package com.example.mutualfollowers.runner;

import com.example.mutualfollowers.model.WebhookGenerateResponse;
import com.example.mutualfollowers.model.WebhookResult;
import com.example.mutualfollowers.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StartupRunner implements CommandLineRunner {

    private final WebhookService webhookService;

    @Override
    public void run(String... args) {
        log.info("Application started, initiating webhook generation and problem solution");

        try {
            // Step 1: Generate webhook
            WebhookGenerateResponse webhookResponse = webhookService.generateWebhook();
            log.info("Webhook generated. Processing problem...");
            
            // Step 2: Process the problem based on the data
            WebhookResult result = webhookService.processProblem(webhookResponse);
            log.info("Problem processed. Result: {}", result);
            
            // Step 3: Send the result to the webhook
            webhookService.sendWebhookResult(
                    webhookResponse.getWebhook(),
                    webhookResponse.getAccessToken(),
                    result
            );
            log.info("Result sent to webhook successfully");
            
        } catch (Exception e) {
            log.error("Error during startup webhook processing", e);
        }
    }
}
