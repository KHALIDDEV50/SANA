package com.example.sana.Controller.CreativityDomainController;

import com.example.sana.API.ApiResponse;
import com.example.sana.DTO.CreativityDomainDTOs.PlayableCharacterDTO;
import com.example.sana.Service.CreativityDomainServise.CharacterBalanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/character-balance")
@RequiredArgsConstructor
public class CharacterBalanceController {

    private final CharacterBalanceService characterBalanceService;


    // Get all playable characters in a ProjectOutput
    // Can also be used to populate an optional character selection screen
    @GetMapping("/get/{outputId}/characters")
    public ResponseEntity<?> getAllCharacters(
            @PathVariable Integer outputId) {

        return ResponseEntity.status(200).body(characterBalanceService.getAllCharacters(outputId));
    }


    // Get one playable character and its gameplay stats
    @GetMapping("/{outputId}/characters/{characterId}")
    public ResponseEntity<?> getCharacterById(
            @PathVariable Integer outputId,
            @PathVariable Integer characterId) {

        return ResponseEntity.status(200).body(characterBalanceService.getCharacterById(outputId, characterId));
    }


    // Add a playable character
    // All gameplay details except the name are optional
    @PostMapping("/{outputId}/characters/add")
    public ResponseEntity<?> addCharacter(
            @PathVariable Integer outputId,
            @RequestBody @Valid PlayableCharacterDTO dto) {

        characterBalanceService.addCharacter(outputId, dto);

        return ResponseEntity.status(200).body(new ApiResponse("Playable character added successfully"));
    }


    // Update a playable character
    @PutMapping("/{outputId}/characters/update/{characterId}")
    public ResponseEntity<?> updateCharacter(
            @PathVariable Integer outputId,
            @PathVariable Integer characterId,
            @RequestBody @Valid PlayableCharacterDTO dto) {

        characterBalanceService.updateCharacter(outputId, characterId, dto);

        return ResponseEntity.status(200).body(new ApiResponse("Playable character updated successfully"));
    }


    // Delete only the playable character
    @DeleteMapping("/{outputId}/characters/delete/{characterId}")
    public ResponseEntity<?> deleteCharacter(
            @PathVariable Integer outputId,
            @PathVariable Integer characterId) {

        characterBalanceService.deleteCharacter(outputId, characterId);

        return ResponseEntity.status(200).body(new ApiResponse("Playable character deleted successfully"));
    }


    // Link a playable character to an existing story character
    @PutMapping("/{outputId}/characters/{characterId}/profile/{profileId}")
    public ResponseEntity<?> linkCharacterProfile(
            @PathVariable Integer outputId,
            @PathVariable Integer characterId,
            @PathVariable Integer profileId) {

        characterBalanceService.linkCharacterProfile(outputId, characterId, profileId);

        return ResponseEntity.status(200).body(new ApiResponse("Character profile linked successfully"));
    }


    // Unlink the story character without deleting it
    @DeleteMapping("/{outputId}/characters/{characterId}/profile")
    public ResponseEntity<?> unlinkCharacterProfile(
            @PathVariable Integer outputId,
            @PathVariable Integer characterId) {

        characterBalanceService.unlinkCharacterProfile(outputId, characterId);

        return ResponseEntity.status(200).body(new ApiResponse("Character profile unlinked successfully"));
    }


    // Analyze the balance of characters with complete gameplay stats
    // Does not change any character data
    @GetMapping("/{outputId}/analyze")
    public ResponseEntity<?> analyzeBalance(
            @PathVariable Integer outputId) {

        return ResponseEntity.status(200).body(characterBalanceService.analyzeBalance(outputId));
    }
}
