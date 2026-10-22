
package com.example.sana.Service.Business;

import com.example.sana.Enums.BusinessToolType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service responsible for identifying and evaluating business risks.
 * Delegates AI execution and JSON validation to BusinessAnalysisService.
 */
@Service
@AllArgsConstructor
public class RiskAnalysisService {

    // Shared service responsible for AI execution and response validation.
    private final BusinessAnalysisService businessAnalysisService;


    /**
     * Analyzes business risks and generates prevention, mitigation,
     * and contingency recommendations.
     *
     * @param inputJson business and risk context represented as JSON
     * @return AI-generated risk analysis as JSON
     */
    public String analyze(String inputJson) {

        // Execute the analysis using the Risk Analyzer tool type.
        return businessAnalysisService.analyze(
                BusinessToolType.RISK_ANALYZER,
                inputJson
        );
    }
}
