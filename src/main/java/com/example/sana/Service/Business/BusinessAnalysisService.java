
package com.example.sana.Service.Business;

import com.example.sana.Enums.BusinessToolType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@AllArgsConstructor
public class BusinessAnalysisService {

    // Sends prompts to the configured AI model.
    private final ChatClient chatClient;

    // Builds the shared system prompt and tool-specific instructions.
    private final BusinessPromptBuilder promptBuilder;

    // Validates the structure and content of the AI response.
    private final BusinessResponseValidator responseValidator;

    // Parses and serializes JSON data.
    private final ObjectMapper objectMapper;



    /**
     * Executes the selected business analysis tool.
     *
     * @param toolType  The business analysis tool to execute.
     * @param inputJson The input data as a JSON object string.
     * @return The validated AI response as a JSON string.
     */
    public String analyze(BusinessToolType toolType, String inputJson) {

        // Ensure that a valid analysis tool has been selected.
        if (toolType == null) {
            throw new IllegalArgumentException("Business tool type is required.");
        }

        // Reject null, empty, or whitespace-only input.
        if (!StringUtils.hasText(inputJson)) {
            throw new IllegalArgumentException("Input JSON must not be empty.");
        }

        // Validate the input and normalize its JSON representation.
        String normalizedInput = normalizeInputJson(inputJson);

        // Build the system prompt using the actual method in BusinessPromptBuilder.
        String systemPrompt = promptBuilder.build(toolType);

        // Send the system instructions and business data to the AI model.
        String responseJson = chatClient.prompt()
                .system(systemPrompt)
                .user(normalizedInput)
                .call()
                .content();

        // Ensure that the AI model returned a non-empty response.
        if (!StringUtils.hasText(responseJson)) {
            throw new IllegalStateException("AI returned an empty response.");
        }

        // Validate the AI response against the selected tool's expected structure.
        JsonNode validatedResponse = responseValidator.validate(responseJson, toolType);

        try {
            // Return the validated response as a normalized JSON string.
            return objectMapper.writeValueAsString(validatedResponse);

        } catch (JsonProcessingException exception) {
            // Report an error if the validated response cannot be serialized.
            throw new IllegalStateException(
                    "Failed to serialize validated AI response.",
                    exception);
        }
    }

    /**
     * Validates the input JSON and returns a normalized JSON object string.
     *
     * @param inputJson The original JSON input.
     * @return The normalized JSON object.
     */
    private String normalizeInputJson(String inputJson) {
        try {
            // Parse the input string into a JSON tree.
            JsonNode inputNode = objectMapper.readTree(inputJson);

            // This service accepts JSON objects, not arrays or primitive values.
            if (inputNode == null || !inputNode.isObject()) {
                throw new IllegalArgumentException("Input must be a valid JSON object.");
            }

            // Serialize the parsed object to produce consistent JSON formatting.
            return objectMapper.writeValueAsString(inputNode);

        } catch (JsonProcessingException exception) {
            // Reject malformed JSON and preserve the original parsing error.
            throw new IllegalArgumentException("Input must contain valid JSON.", exception);
        }
    }
}
