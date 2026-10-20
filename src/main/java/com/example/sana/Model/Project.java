package com.example.sana.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "projects")
@Data
@AllArgsConstructor
@NoArgsConstructor

public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String domain;

    @Column(nullable = false, length = 50)
    private String status;

    @Version
    @Column(nullable = false)
    private Integer version = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;


    // Relations

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToOne(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private ProjectUnderstanding understanding;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "project")
    private Set<ProjectNode> nodes;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "project")
    private Set<ProjectEdge> edges;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "project")
    private Set<ProjectRecord> records;

}
