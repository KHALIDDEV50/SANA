package com.example.sana.Model;

import com.example.sana.Model.CreativityDomainModels.*;
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
    @Column(columnDefinition = "double not null")
    private Double positionX;
    @NotNull(message = "positon Y cannot be empty")
    @Column(columnDefinition = "double not null")
    private Double positionY;
    @Column(columnDefinition = "date")
    private LocalDate createdAt;
    @Column(columnDefinition = "date")
    private LocalDate updatedAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_output_id", referencedColumnName = "id", nullable = false)
    @JsonIgnore
    private ProjectOutput projectOutput;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "sourceNode")
    @JsonIgnore
    private Set<ProjectEdge> outgoingEdges;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "targetNode")
    @JsonIgnore
    private Set<ProjectEdge> incomingEdges;


    @OneToOne(cascade = CascadeType.ALL, mappedBy = "projectNode")
    @PrimaryKeyJoinColumn
    private CharacterProfile characterProfile;

    @OneToOne(cascade = CascadeType.ALL,mappedBy = "projectNode")
    @PrimaryKeyJoinColumn
    private World world;

    @OneToOne(cascade = CascadeType.ALL,mappedBy = "projectNode")
    @PrimaryKeyJoinColumn
    private Location location;

    @OneToOne(cascade = CascadeType.ALL,mappedBy = "projectNode")
    @PrimaryKeyJoinColumn
    private WorldRule worldRule;

    @OneToOne(cascade = CascadeType.ALL,mappedBy = "projectNode")
    @PrimaryKeyJoinColumn
    private StoryEvent storyEvent;



}
