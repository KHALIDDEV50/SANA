package com.example.sana.Service.CreativityDomainServise;

import com.example.sana.API.ApiException;
import com.example.sana.DTO.StoryEventDTO;
import com.example.sana.Model.CreativityDomainModels.StoryEvent;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Repository.CreativityDomainRepository.StoryEventRepository;
import com.example.sana.Repository.ProjectNodeRepository;
import com.example.sana.Repository.ProjectOutputRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoryEventService {

    private final StoryEventRepository storyEventRepository;
    private final ProjectNodeRepository projectNodeRepository;
    private final ProjectOutputRepository projectOutputRepository;


    // Get all story events
    public List<StoryEvent> getAllStoryEvents() {
        return storyEventRepository.findAll();
    }


    // Get a specific story event by ID
    public StoryEvent getStoryEventById(Integer id) {

        StoryEvent storyEvent = storyEventRepository.findStoryEventById(id);

        if (storyEvent == null) {
            throw new ApiException("Story event not found");
        }

        return storyEvent;
    }


    // Create a StoryEvent and its ProjectNode together
    @Transactional
    public void addStoryEvent(Integer outputId, StoryEventDTO dto) {

        ProjectOutput output = projectOutputRepository.findProjectOutputById(outputId);

        if (output == null) {
            throw new ApiException("Project output not found");
        }

        // Create ProjectNode
        ProjectNode node = new ProjectNode();

        node.setType("STORY_EVENT");
        node.setPositionX(dto.getPositionX());
        node.setPositionY(dto.getPositionY());
        node.setProjectOutput(output);
        node.setCreatedAt(LocalDate.now());
        node.setUpdatedAt(LocalDate.now());

        // Create StoryEvent
        StoryEvent storyEvent = new StoryEvent();

        storyEvent.setTitle(dto.getTitle());
        storyEvent.setDescription(dto.getDescription());
        storyEvent.setEventType(dto.getEventType());
        storyEvent.setPresentationOrder(dto.getPresentationOrder());
        storyEvent.setInWorldTime(dto.getInWorldTime());
        storyEvent.setNotes(dto.getNotes());
        storyEvent.setCreatedAt(LocalDate.now());
        storyEvent.setUpdatedAt(LocalDate.now());

        // Link both sides
        storyEvent.setProjectNode(node);
        node.setStoryEvent(storyEvent);

        // Save node and event using CascadeType.ALL
        projectNodeRepository.save(node);
    }


    // Update StoryEvent details and its existing Node position
    // Does not create a new ProjectNode or change its type
    @Transactional
    public void updateStoryEvent(Integer id, StoryEventDTO dto) {

        StoryEvent storyEvent = storyEventRepository.findStoryEventById(id);

        if (storyEvent == null) {
            throw new ApiException("Story event not found");
        }

        ProjectNode node = storyEvent.getProjectNode();

        if (node == null) {
            throw new ApiException("Project node not found");
        }

        // Update StoryEvent
        storyEvent.setTitle(dto.getTitle());
        storyEvent.setDescription(dto.getDescription());
        storyEvent.setEventType(dto.getEventType());
        storyEvent.setPresentationOrder(dto.getPresentationOrder());
        storyEvent.setInWorldTime(dto.getInWorldTime());
        storyEvent.setNotes(dto.getNotes());
        storyEvent.setUpdatedAt(LocalDate.now());

        // Update existing ProjectNode
        node.setPositionX(dto.getPositionX());
        node.setPositionY(dto.getPositionY());
        node.setUpdatedAt(LocalDate.now());

        storyEventRepository.save(storyEvent);
        projectNodeRepository.save(node);
    }


    // Delete StoryEvent and its associated ProjectNode
    @Transactional
    public void deleteStoryEvent(Integer id) {

        StoryEvent storyEvent = storyEventRepository.findStoryEventById(id);

        if (storyEvent == null) {
            throw new ApiException("Story event not found");
        }

        ProjectNode node = storyEvent.getProjectNode();

        if (node == null) {
            storyEventRepository.delete(storyEvent);
            return;
        }

        projectNodeRepository.delete(node);
    }
}

