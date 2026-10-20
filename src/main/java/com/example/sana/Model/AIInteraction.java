package com.example.sana.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class AIInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Positive(message = "AI interaction ID must be greater than zero")
    private Integer id;

    @NotBlank(message = "AI action type must not be blank")
    @Size(max = 255, message = "AI action type must not exceed 255 characters")
    private String actionType;

    @Column(columnDefinition = "TEXT")
    @NotBlank(message = "AI prompt summary must not be blank")
    private String promptSummary;

    @Column(columnDefinition = "LONGTEXT")
    private String responseJson;

    @NotBlank(message = "AI model name must not be blank")
    @Size(max = 255, message = "AI model name must not exceed 255 characters")
    private String modelName;

    @NotBlank(message = "AI interaction status must not be blank")
    @Size(max = 255, message = "AI interaction status must not exceed 255 characters")
    private String status;

    @CreationTimestamp
    @PastOrPresent(message = "AI interaction creation time must not be in the future")
    private LocalDateTime createdAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", referencedColumnName = "id", nullable = false)
    @JsonIgnore
    private Project project;
}
