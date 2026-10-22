package com.example.sana.DTO;

import com.example.sana.Enums.BranchingEffectOperator;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BranchingEffectDTO {

    @NotNull
    private BranchingEffectOperator operator;

    @NotNull
    private Integer value;
}
