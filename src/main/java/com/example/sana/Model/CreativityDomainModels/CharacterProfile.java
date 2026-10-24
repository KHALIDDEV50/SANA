package com.example.sana.Model.CreativityDomainModels;

import com.example.sana.Model.ProjectNode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class CharacterProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotEmpty(message = "name cannot be empty")
    @Column(columnDefinition = "varchar(255) not null")
    private String name;
    @NotEmpty(message = "description cannot be empty")
    @Column(columnDefinition = "text not null")
    private String shortDescription;
    @NotEmpty(message = "role cannot be empty")
    @Column(columnDefinition = "varchar(50) not null")
    private String role;
    @Column(columnDefinition = "varchar(100)")
    private String aliases;
    @NotNull(message = "age cannot be empty")
    @Positive
    @Column(columnDefinition = "int not null")
    private Integer age;
    @Column(columnDefinition = "text")
    private String appearance;
    @Column(columnDefinition = "varchar(50)")
    private String traits;
    @Column(columnDefinition = "varchar(50)")
    private String strengths;
    @Column(columnDefinition = "varchar(50)")
    private String weaknesses;
    @Column(columnDefinition = "varchar(50)")
    private String skills;
    @Column(columnDefinition = "text")
    private String history;
    @Column(columnDefinition = "varchar(50)")
    private String goals;
    @Column(columnDefinition = "varchar(50)")
    private String fears;
    @Column(columnDefinition = "text")
    private String innerConflict;
    @Column(columnDefinition = "varchar(50)")
    private String secrets;
    @Column(columnDefinition = "varchar(50)")
    private String characterValues;
    @Column(columnDefinition = "text")
    private String characterArc;
    @Column(columnDefinition = "text")
    private String writerNotes;
    @Column(columnDefinition = "date")
    private LocalDate createdAt;
    @Column(columnDefinition = "date")
    private LocalDate updatedAt;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_node_id")
    private ProjectNode projectNode;





}
