package com.example.sana.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Set;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class ChangeProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Positive(message = "Proposal ID must be greater than zero")
    private Integer id;

    @NotBlank(message = "Proposal title must not be blank")
    @Size(max = 255, message = "Proposal title must not exceed 255 characters")
    private String title;

    @Column(columnDefinition = "TEXT")
    @NotBlank(message = "Proposal description must not be blank")
    private String description;

    @NotBlank(message = "Proposal status must not be blank")
    @Size(max = 255, message = "Proposal status must not exceed 255 characters")
    private String status;

    @NotNull(message = "Base version is required")
    @PositiveOrZero(message = "Base version must be zero or greater")
    private Integer baseVersion;

    @CreationTimestamp
    @PastOrPresent(message = "Proposal creation time must not be in the future")
    private LocalDateTime createdAt;

    @PastOrPresent(message = "Proposal review time must not be in the future")
    private LocalDateTime reviewedAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", referencedColumnName = "id", nullable = false)
    @JsonIgnore
    private Project project;

    @ManyToOne
    @JoinColumn(name = "ai_interaction_id", referencedColumnName = "id")
    @JsonIgnore
    private AIInteraction aiInteraction;

    @OneToMany(mappedBy = "changeProposal")
    private Set<ChangeProposalItem> items;
}
