package com.example.sana.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
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
public class ProjectEdge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotEmpty(message = "type can not be empty")
    @Column(columnDefinition = "varchar(50) not null")
    private String type;
    @NotEmpty(message = "label cannot be empty")
    @Column(columnDefinition = "varchar(50) not null")
    private String label;

    private String dataJson;
    @Column(columnDefinition = "date")
    private LocalDate createdAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_output_id", referencedColumnName = "id", nullable = false)
    @JsonIgnore
    private ProjectOutput projectOutput;

    @ManyToOne(optional = false)
    @JoinColumn(name = "source_node_id", referencedColumnName = "id", nullable = false)
    private ProjectNode sourceNode;

    @ManyToOne(optional = false)
    @JoinColumn(name = "target_node_id", referencedColumnName = "id", nullable = false)
    private ProjectNode targetNode;
}
