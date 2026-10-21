package com.example.sana.Model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_understandings")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectUnderstanding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotEmpty(message = "Project goal is required")
    @Size(max = 10000, message = "Project goal must not exceed 10000 characters")
    @Column(columnDefinition = "TEXT")
    private String projectGoal;

    @Size(max = 10000, message = "Actors JSON must not exceed 10000 characters")
    @Column(columnDefinition = "TEXT")
    private String actorsJson;

    @Size(max = 10000, message = "Components JSON must not exceed 10000 characters")
    @Column(columnDefinition = "TEXT")
    private String componentsJson;

    @Size(max = 10000, message = "Constraints JSON must not exceed 10000 characters")
    @Column(columnDefinition = "TEXT")
    private String constraintsJson;

    @NotNull(message = "Creation date is required")
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @NotNull(message = "Update date is required")
    @Column(nullable = false)
    private LocalDateTime updatedAt;


    // Relations
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private Project project;
}
