package org.belex.paymentschecker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.belex.paymentschecker.dto.ExplanationResult;
import org.belex.paymentschecker.modal.Discrepancy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns a deterministic discrepancy into a plain-language explanation via an
 * LLM. The model never decides *whether* two records match - that decision
 * has already been made by {@link ReconciliationService}. It only explains
 * and suggests next steps.
 */
@Service
public class LlmExplanationService {

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    public ExplanationResult explain(Discrepancy d) {
        if (apiKey == null || apiKey.isBlank()) {
            return ExplanationResult.failure("OPENAI_API_KEY is not configured on the server.");
        }

        String prompt = "Discrepancy type: " + d.getType() + "\n"
                + "Order: " + d.getOrderId() + "\n"
                + "Payment reference: " + d.getPaymentRef() + "\n"
                + "Amount at risk: " + d.getAmountAtRisk() + "\n"
                + "System note: " + d.getDescription() + "\n\n"
                + "Explain in plain language what likely happened and what a revenue-operations "
                + "person should do about it. Reply as JSON with exactly two keys: "
                + "\"explanation\" and \"recommended_action\".";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", 0.2);
        body.put("response_format", Map.of("type", "json_object"));
        body.put("messages", new Object[]{
                Map.of("role", "system", "content",
                        "You are a financial reconciliation assistant. You only explain and summarise "
                                + "discrepancies that have already been detected by deterministic rules; "
                                + "you never decide whether records match. Always reply with strict JSON."),
                Map.of("role", "user", "content", prompt)
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        try {
            var response = restTemplate.postForObject(OPENAI_URL, new HttpEntity<>(body, headers), String.class);
            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices").get(0).path("message").path("content").asText();
            JsonNode parsed = objectMapper.readTree(content);

            if (!parsed.has("explanation") || !parsed.has("recommended_action")) {
                return ExplanationResult.failure("The model response was missing expected fields.");
            }
            return ExplanationResult.ok(parsed.get("explanation").asText(), parsed.get("recommended_action").asText());
        } catch (Exception e) {
            return ExplanationResult.failure("Could not get an explanation from the LLM: " + e.getMessage());
        }
    }
}
