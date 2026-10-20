package com.example.sana.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class ProjectNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotEmpty(message = "Type cannot be empty")
    @Column(columnDefinition = "varchar(50) not null")
    private String type;

    private String dataJson;
    @NotNull(message = "position X cannot be null")
    @Column(columnDefinition = "double not empty")
    private Double positionX;
    @NotNull(message = "positon Y cannot be empty")
    @Column(columnDefinition = "double not empty")
    private Double positionY;
    @Column(columnDefinition = "date")
    private LocalDate createdAt;
    @Column(columnDefinition = "date")
    private LocalDate updatedAt;

    @ManyToOne
    @JoinColumn
    @JsonIgnore
    private Project project;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "sourceNode")
    private Set<ProjectEdge> outgoingEdges;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "targetNode")
    private Set<ProjectEdge> incomingEdges;


}
