package com.example.sana.Repository.CreativityDomainRepository;

import com.example.sana.Model.CreativityDomainModels.StoryEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoryEventRepository extends JpaRepository<StoryEvent, Integer> {

    StoryEvent findStoryEventById(Integer id);

}
