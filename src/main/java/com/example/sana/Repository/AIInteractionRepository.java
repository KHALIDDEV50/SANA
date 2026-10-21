package com.example.sana.Repository;

import com.example.sana.Model.AIInteraction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AIInteractionRepository extends JpaRepository<AIInteraction, Integer> {

    AIInteraction findAIInteractionById(Integer id);
}
