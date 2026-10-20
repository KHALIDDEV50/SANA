package com.example.sana.API;


import lombok.AllArgsConstructor;
import lombok.Getter;


@AllArgsConstructor
public class ApiException extends RuntimeException {
    private final String message;
}
