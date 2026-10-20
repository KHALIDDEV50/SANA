package com.example.sana.Model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
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
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String projectGoal;

    @Column(columnDefinition = "TEXT")
    private String actorsJson;

    @Column(columnDefinition = "TEXT")
    private String componentsJson;

    @Column(columnDefinition = "TEXT")
    private String constraintsJson;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;


    // Relations
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private Project project;
}
