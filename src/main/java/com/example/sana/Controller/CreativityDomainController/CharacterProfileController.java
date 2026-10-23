package com.example.sana.Controller.CreativityDomainController;

import com.example.sana.API.ApiResponse;
import com.example.sana.DTO.CreativityDomainDTOs.CharacterProfileDTO;
import com.example.sana.Service.CreativityDomainServise.CharacterProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/character-profile")
@RequiredArgsConstructor
public class CharacterProfileController {

    private final CharacterProfileService characterProfileService;

    @GetMapping("/get")
    public ResponseEntity<?> getAllCharacterProfiles() {
        return ResponseEntity.status(200).body(characterProfileService.getAllCharacterProfiles());
    }

    @GetMapping("/get/by/{id}")
    public ResponseEntity<?> getCharacterProfileById(@PathVariable Integer id) {
        return ResponseEntity.status(200).body(characterProfileService.getCharacterProfileById(id));
    }


    @PostMapping("/add/{outputId}")
    public ResponseEntity<?> addCharacterProfile(@PathVariable Integer outputId, @RequestBody @Valid CharacterProfileDTO dto) {
        characterProfileService.addCharacterProfile(outputId, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Character profile added successfully"));
    }



    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateCharacterProfile(@PathVariable Integer id, @RequestBody @Valid CharacterProfileDTO dto) {
        characterProfileService.updateCharacterProfile(id, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Character profile updated successfully"));
    }


    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteCharacterProfile(@PathVariable Integer id) {
        characterProfileService.deleteCharacterProfile(id);
        return ResponseEntity.status(200).body(new ApiResponse("Character profile deleted successfully"));
    }
}


