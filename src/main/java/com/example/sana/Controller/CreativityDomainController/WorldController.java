package com.example.sana.Controller.CreativityDomainController;


import com.example.sana.API.ApiResponse;
import com.example.sana.DTO.CreativityDomainDTOs.WorldDTO;
import com.example.sana.Service.CreativityDomainServise.WorldService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/world")
@RequiredArgsConstructor
public class WorldController {

    private final WorldService worldService;

    @GetMapping("/get")
    public ResponseEntity<?> getAllWorlds() {
        return ResponseEntity.status(200).body(worldService.getAllWorlds());
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<?> getWorldById(@PathVariable Integer id) {
        return ResponseEntity.status(200).body(worldService.getWorldById(id));
    }

    @PostMapping("/add/{outputId}")
    public ResponseEntity<?> addWorld(@PathVariable Integer outputId, @RequestBody @Valid WorldDTO dto) {
        worldService.addWorld(outputId, dto);
        return ResponseEntity.status(200).body(new ApiResponse("World added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateWorld(@PathVariable Integer id, @RequestBody @Valid WorldDTO dto) {
        worldService.updateWorld(id, dto);
        return ResponseEntity.status(200).body(new ApiResponse("World updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteWorld(@PathVariable Integer id) {
        worldService.deleteWorld(id);
        return ResponseEntity.status(200).body(new ApiResponse("World deleted successfully"));
    }
}

