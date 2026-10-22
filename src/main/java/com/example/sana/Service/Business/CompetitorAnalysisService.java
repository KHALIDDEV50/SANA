
package com.example.sana.Service.Business;

import com.example.sana.Enums.BusinessToolType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service responsible for analyzing competitors and competitive positioning.
 * Uses only the supplied information and the configured AI analysis rules.
 */
@Service
@AllArgsConstructor
public class CompetitorAnalysisService {

    // Shared service responsible for AI execution and response validation.
    private final BusinessAnalysisService businessAnalysisService;

    /**
     * Compares competitors and identifies potential differentiation
     * opportunities and missing competitive information.
     *
     * @param inputJson competitor and business information represented as JSON
     * @return AI-generated competitor analysis as JSON
     */
    public String analyze(String inputJson) {

        // Execute the analysis using the Competitor Analysis tool type.
        return businessAnalysisService.analyze(
                BusinessToolType.COMPETITOR_ANALYSIS,
                inputJson
        );
    }
}
