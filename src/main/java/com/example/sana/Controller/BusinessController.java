
package com.example.sana.Controller;

import com.example.sana.DTO.Business.BusinessToolRequestDTO;
import com.example.sana.Service.Business.BusinessService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for business analysis operations.
 * Handles HTTP requests and delegates business logic to BusinessService.
 */
@RestController
@RequestMapping("/api/business")
@AllArgsConstructor
public class BusinessController {

    // Service responsible for executing business analysis tools.
    private final BusinessService businessService;

    /**
     * Executes the selected business analysis tool.
     *
     * @param request validated business analysis request
     * @return the AI-generated analysis as a JSON response
     */
    @PostMapping(
            value = "/tools",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> executeTool(@Valid @RequestBody BusinessToolRequestDTO request) {

        // Execute the selected tool and retrieve its validated JSON response.
        String result = businessService.executeTool(request);

        // Return the analysis result with HTTP 200 OK.
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result);
    }
}
