
package com.example.sana.Service.Business;

import com.example.sana.Enums.BusinessToolType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service responsible for analyzing and building a business model.
 * Delegates AI execution and JSON validation to BusinessAnalysisService.
 */
@Service
@AllArgsConstructor
public class BusinessModelService {

    // Shared service responsible for AI execution and response validation.
    private final BusinessAnalysisService businessAnalysisService;


    /**
     * Analyzes the business information using the Business Model Builder.
     *
     * @param inputJson business information represented as JSON
     * @return AI-generated business model analysis as JSON
     */
    public String analyze(String inputJson) {

        // Execute the analysis using the Business Model Builder tool type.
        return businessAnalysisService.analyze(
                BusinessToolType.BUSINESS_MODEL_BUILDER,
                inputJson
        );
    }
}
