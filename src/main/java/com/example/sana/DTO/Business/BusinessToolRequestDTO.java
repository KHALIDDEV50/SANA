package com.example.sana.DTO.Business;

import com.example.sana.Enums.BusinessToolType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BusinessToolRequestDTO(

        @NotNull
        @Positive
        Integer projectId,

        @NotNull
        BusinessToolType toolType,

        String inputJson
) {
}