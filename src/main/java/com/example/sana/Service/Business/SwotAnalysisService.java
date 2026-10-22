
package com.example.sana.Service.Business;

import com.example.sana.Enums.BusinessToolType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service responsible for analyzing business strengths, weaknesses,
 * opportunities, and threats, and deriving strategic recommendations.
 */
@Service
@AllArgsConstructor
public class SwotAnalysisService {

    // Shared service responsible for AI execution and response validation.
    private final BusinessAnalysisService businessAnalysisService;

    /**
     * Creates the service with its required analysis dependency.
     *
     * @param businessAnalysisService shared AI analysis service
     */
    /*public SwotAnalysisService(
            BusinessAnalysisService businessAnalysisService) {
        this.businessAnalysisService = businessAnalysisService;
    }*/

    /**
     * Performs a SWOT analysis and generates related strategic actions.
     *
     * @param inputJson business information represented as JSON
     * @return AI-generated SWOT analysis as JSON
     */
    public String analyze(String inputJson) {

        // Execute the analysis using the SWOT Analyzer tool type.
        return businessAnalysisService.analyze(
                BusinessToolType.SWOT_ANALYZER,
                inputJson
        );
    }
}
