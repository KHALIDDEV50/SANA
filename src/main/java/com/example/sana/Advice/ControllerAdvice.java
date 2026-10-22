package com.example.sana.Advice;

import com.example.sana.API.ApiException;
import com.example.sana.API.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.databind.exc.InvalidFormatException;

import java.util.Arrays;

@RestControllerAdvice
public class ControllerAdvice {


    // Api Exception
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse> handleApiException(
            ApiException e) {

        // Print real error in console
        e.printStackTrace();

        return ResponseEntity.status(400).body(new ApiResponse(e.getMessage()));
    }

    // Handles invalid arguments supplied to application services.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleIllegalArgumentException(
            IllegalArgumentException e) {

        return ResponseEntity.badRequest().body(new ApiResponse(e.getMessage()));
    }



    // Validation Exception
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidationException(
            MethodArgumentNotValidException e) {

        String message = e.getBindingResult()
                .getFieldError()
                .getDefaultMessage();

        return ResponseEntity.status(400).body(new ApiResponse(message));
    }


    // Constraint Violation Exception
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse> handleConstraintViolationException(
            ConstraintViolationException e) {

        e.printStackTrace();

        return ResponseEntity.status(400).body(new ApiResponse(e.getMessage()));
    }


    // Database Exception
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException e) {

        e.printStackTrace();

        return ResponseEntity.status(400)
                .body(new ApiResponse("Database error: " + e.getMessage()));
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse> handleUnreadableRequest(HttpMessageNotReadableException e) {
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof InvalidFormatException invalidFormat
                    && invalidFormat.getTargetType() != null
                    && invalidFormat.getTargetType().isEnum()) {
                String message = "Invalid " + invalidFormat.getTargetType().getSimpleName()
                        + ". Allowed values: " + Arrays.toString(invalidFormat.getTargetType().getEnumConstants());
                return ResponseEntity.status(400).body(new ApiResponse(message));
            }
            cause = cause.getCause();
        }
        return ResponseEntity.status(400)
                .body(new ApiResponse("Request body contains malformed JSON or an invalid value"));
    }

    // General Exception
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleException(
            Exception e) {

        // Print full error in console
        e.printStackTrace();

        return ResponseEntity.status(500).body(new ApiResponse("Error: " + e.getMessage()));
    }
}
