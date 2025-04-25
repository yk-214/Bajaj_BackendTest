# Mutual Followers Challenge

A Spring Boot application that automatically interacts with a remote API at startup, processes data, and submits results to a webhook.

## Features

- Calls the `/generateWebhook` endpoint at application startup
- Solves the assigned problem based on the registration number:
  - Question 1 (odd regNo): Identifies mutual follow pairs
  - Question 2 (even regNo): Finds nth-level followers
- Sends the results to the provided webhook with JWT authentication
- Implements retry logic (up to 4 attempts) for webhook submission

## Implementation Details

### Flow

1. On startup, the application makes a POST request to the webhook generation endpoint
2. The response includes a webhook URL, access token, and problem data
3. The application determines which problem to solve based on the registration number
4. The problem is solved, and the result is formatted according to requirements
5. The result is posted to the webhook URL with the access token in the Authorization header

### Problems

#### Question 1: Mutual Followers

Identifies mutual follow pairs where both users follow each other. Output is in the format of `[min, max]` pairs.

Example:
```json
{
  "regNo": "REG12347",
  "outcome": [[1, 2], [3, 4]]
}
```

#### Question 2: Nth-Level Followers

Given a start ID and nth level, returns user IDs that are exactly n levels away in the "follows" list.

Example:
```json
{
  "regNo": "REG12347",
  "outcome": [4, 5]
}
```

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

## Running the Application

Build and run the application with:

```bash
mvn clean install
mvn spring-boot:run
```

The application will automatically:
1. Call the webhook generation endpoint
2. Process the data
3. Send the result to the provided webhook

## Configuration

Configuration options in `application.properties`:

```properties
# API configuration
api.base-url=https://bfhldevapigw.healthrx.co.in/hiring
api.registration.name=John Doe
api.registration.regNo=REG12347
api.registration.email=john@example.com

# Retry configuration 
spring.retry.max-attempts=4
spring.retry.initial-interval=1000
spring.retry.multiplier=1.5
spring.retry.max-interval=10000
``` 