package com.example.sana.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class ChangeProposalItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Change operation must not be blank")
    @Size(max = 255, message = "Change operation must not exceed 255 characters")
    private String operation;

    @NotBlank(message = "Change target type must not be blank")
    @Size(max = 255, message = "Change target type must not exceed 255 characters")
    private String targetType;

    @Positive(message = "Change target ID must be greater than zero")
    private Long targetId;

    @Column(columnDefinition = "LONGTEXT")
    private String beforeJson;

    @Column(columnDefinition = "LONGTEXT")
    private String afterJson;

    @ManyToOne(optional = false)
    @JoinColumn(name = "change_proposal_id", referencedColumnName = "id", nullable = false)
    @JsonIgnore
    private ChangeProposal changeProposal;
}
