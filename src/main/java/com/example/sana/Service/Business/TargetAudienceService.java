
package com.example.sana.Service.Business;

import com.example.sana.Enums.BusinessToolType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service responsible for identifying and analyzing target audiences.
 * Delegates AI execution and JSON validation to BusinessAnalysisService.
 */
@Service
@AllArgsConstructor
public class TargetAudienceService {

    // Shared service responsible for AI execution and response validation.
    private final BusinessAnalysisService businessAnalysisService;

    /**
     * Creates the service with its required analysis dependency.
     *
     * @param businessAnalysisService shared AI analysis service
     */
   /* public TargetAudienceService(
            BusinessAnalysisService businessAnalysisService) {
        this.businessAnalysisService = businessAnalysisService;
    }*/

    /**
     * Analyzes customer segments, needs, motivations, and buying barriers.
     *
     * @param inputJson business and customer information represented as JSON
     * @return AI-generated target audience analysis as JSON
     */
    public String analyze(String inputJson) {

        // Execute the analysis using the Target Audience Builder tool type.
        return businessAnalysisService.analyze(
                BusinessToolType.TARGET_AUDIENCE_BUILDER,
                inputJson
        );
    }
}
