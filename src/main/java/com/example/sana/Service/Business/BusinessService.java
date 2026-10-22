
package com.example.sana.Service.Business;

import com.example.sana.DTO.Business.BusinessToolRequestDTO;
import com.example.sana.Enums.BusinessToolType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@AllArgsConstructor
public class BusinessService {

    // Services responsible for executing individual business analysis tools.
    private final BusinessModelService businessModelService;
    private final TargetAudienceService targetAudienceService;
    private final SwotAnalysisService swotAnalysisService;
    private final CompetitorAnalysisService competitorAnalysisService;
    private final RiskAnalysisService riskAnalysisService;

    /**
     * Creates the orchestration service and injects all analysis services.
     *
     * @param businessModelService Business model analysis service.
     * @param targetAudienceService Target audience analysis service.
     * @param swotAnalysisService SWOT analysis service.
     * @param competitorAnalysisService Competitor analysis service.
     * @param riskAnalysisService Risk analysis service.
     */
   /* public BusinessService(
            BusinessModelService businessModelService,
            TargetAudienceService targetAudienceService,
            SwotAnalysisService swotAnalysisService,
            CompetitorAnalysisService competitorAnalysisService,
            RiskAnalysisService riskAnalysisService) {

        this.businessModelService = businessModelService;
        this.targetAudienceService = targetAudienceService;
        this.swotAnalysisService = swotAnalysisService;
        this.competitorAnalysisService = competitorAnalysisService;
        this.riskAnalysisService = riskAnalysisService;
    }*/

    /**
     * Validates the request and delegates execution to the selected tool.
     *
     * @param request Contains the project ID, tool type, and input JSON.
     * @return The analysis result as a JSON string.
     */
    public String executeTool(BusinessToolRequestDTO request) {

        // Validate the request before executing any analysis.
        validateRequest(request);

        // Replace missing or blank input with an empty JSON object.
        String inputJson = normalizeInput(request.inputJson());

        // Route the request to the service associated with the selected tool.
        return switch (request.toolType()) {

            case BUSINESS_MODEL_BUILDER ->
                    businessModelService.analyze(inputJson);

            case TARGET_AUDIENCE_BUILDER ->
                    targetAudienceService.analyze(inputJson);

            case SWOT_ANALYZER ->
                    swotAnalysisService.analyze(inputJson);

            case COMPETITOR_ANALYSIS ->
                    competitorAnalysisService.analyze(inputJson);

            case RISK_ANALYZER ->
                    riskAnalysisService.analyze(inputJson);
        };
    }

    /**
     * Validates the request object and its required fields.
     *
     * Note: Project existence and user ownership are not checked here.
     *
     * @param request The incoming business analysis request.
     */
    private void validateRequest(BusinessToolRequestDTO request) {

        // Ensure that the request object was provided.
        if (request == null) {
            throw new IllegalArgumentException(
                    "Business tool request is required.");
        }

        // Ensure that the project ID is a positive number.
        if (request.projectId() == null || request.projectId() <= 0) {
            throw new IllegalArgumentException(
                    "A valid project ID is required.");
        }

        // Ensure that a business analysis tool was selected.
        if (request.toolType() == null) {
            throw new IllegalArgumentException(
                    "Business tool type is required.");
        }
    }

    /**
     * Normalizes missing input into an empty JSON object.
     *
     * Note: JSON syntax validation is performed by BusinessAnalysisService.
     *
     * @param inputJson The original input JSON string.
     * @return The normalized input JSON string.
     */
    private String normalizeInput(String inputJson) {

        // Return an empty JSON object when the input is null or blank.
        if (!StringUtils.hasText(inputJson)) {
            return "{}";
        }

        // Preserve non-empty input; the analysis layer validates its JSON syntax.
        return inputJson;
    }
}
