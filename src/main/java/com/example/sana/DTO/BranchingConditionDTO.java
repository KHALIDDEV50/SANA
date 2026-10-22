package com.example.sana.DTO;

import com.example.sana.Enums.BranchingComparisonOperator;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BranchingConditionDTO {

    @NotNull
    private BranchingComparisonOperator operator;

    @NotNull
    private Integer value;
}
