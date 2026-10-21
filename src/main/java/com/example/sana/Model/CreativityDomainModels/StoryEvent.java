package com.example.sana.Model.CreativityDomainModels;

import com.example.sana.Model.ProjectNode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class StoryEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotEmpty(message = "title cannot be empty")
    @Column(columnDefinition = "varchar(255) not null")
    private String title;
    @NotEmpty(message = "description cannot be empty")
    @Column(columnDefinition = "text not null")
    private String description;
    @Column(columnDefinition = "varchar(50)")
    private String eventType;
    @NotNull(message = "order cannot be empty")
    @Column(columnDefinition = "int not null")
    private Integer presentationOrder;
    @Column(columnDefinition = "varchar(50)")
    private String inWorldTime;
    @Column(columnDefinition = "text")
    private String notes;
    @Column(columnDefinition = "date")
    private LocalDate createdAt;
    @Column(columnDefinition = "date")
    private LocalTime updatedAt;



    @OneToOne
    @JoinColumn(name = "project_node_id")
    private ProjectNode projectNode;



}
