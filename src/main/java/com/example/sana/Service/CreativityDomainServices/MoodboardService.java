package com.example.sana.Service.CreativityDomainServise;

import com.example.sana.API.ApiException;
import com.example.sana.DTO.CreativityDomainDTOs.MoodboardDTO;
import com.example.sana.DTO.CreativityDomainDTOs.OpenverseDTO;
import com.example.sana.Enums.RepresentationType;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Repository.ProjectOutputRepository;
import com.example.sana.Repository.ProjectRepository;
import com.example.sana.Repository.CreativityDomainRepository.MoodboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MoodboardService {

    private static final String IMAGE = "MOODBOARD_IMAGE";
    private static final String LABEL = "MOODBOARD_LABEL";

    private final ProjectRepository projectRepository;
    private final ProjectOutputRepository projectOutputRepository;
    private final MoodboardRepository moodboardRepository;
    private final OpenverseService openverseService;
    private final JsonMapper jsonMapper;

    // Create a board as an ordinary ProjectOutput; reuse the existing database schema.
    @Transactional
    public MoodboardDTO.BoardSummary createBoard(Integer projectId, MoodboardDTO.CreateBoard request) {
        if (projectId == null || !projectRepository.existsById(projectId)) {
            throw new ApiException("Project not found");
        }
        String title = request.title().trim();
        ProjectOutput output = new ProjectOutput();
        output.setTitle(title);
        output.setOutputType("MOODBOARD");
        output.setRepresentationType(RepresentationType.IMAGE);
        output.setCreatedAt(LocalDateTime.now());
        output.setUpdatedAt(output.getCreatedAt());

        // A reference avoids loading the project's complete entity relationships.
        output.setProject(projectRepository.getReferenceById(projectId));
        ProjectOutput saved = projectOutputRepository.save(output);
        return new MoodboardDTO.BoardSummary(saved.getId(), projectId, saved.getTitle());
    }

    @Transactional(readOnly = true)
    public List<MoodboardDTO.BoardSummary> getProjectBoards(Integer projectId) {
        if (projectId == null || !projectRepository.existsById(projectId)) {
            throw new ApiException("Project not found");
        }
        List<MoodboardDTO.BoardSummary> boards = new ArrayList<>();
        for (Object[] row : moodboardRepository.findBoardsByProject(projectId)) {
            boards.add(boardSummary(row));
        }
        return boards;
    }

    // Return only moodboard nodes, not all project edges/worlds/characters.
    @Transactional(readOnly = true)
    public MoodboardDTO.Board getBoard(Integer outputId) {
        MoodboardDTO.BoardSummary board = requireBoard(outputId);
        List<MoodboardDTO.Item> items = new ArrayList<>();
        for (Object[] row : moodboardRepository.findItems(outputId)) {
            items.add(toItem(row));
        }
        return new MoodboardDTO.Board(board.id(), board.projectId(), board.title(), items);
    }

    @Transactional
    public MoodboardDTO.BoardSummary renameBoard(Integer outputId, MoodboardDTO.RenameBoard request) {
        MoodboardDTO.BoardSummary board = requireBoard(outputId);
        String title = request.title().trim();
        if (moodboardRepository.renameBoard(outputId, title, LocalDateTime.now()) != 1) {
            throw new ApiException("Moodboard could not be renamed");
        }
        return new MoodboardDTO.BoardSummary(board.id(), board.projectId(), title);
    }

    // Fetch external image metadata first, then persist it as an ordinary canvas node.
    // Openverse is never trusted to send database IDs or executable JavaScript.
    @Transactional
    public MoodboardDTO.Item addImage(Integer outputId, MoodboardDTO.AddImage request) {
        requireBoard(outputId);
        validatePosition(request.positionX(), request.positionY());
        OpenverseDTO.Image image = openverseService.getImage(request.openverseId());
        return createItem(outputId, IMAGE, json(image), request.positionX(), request.positionY());
    }

    @Transactional
    public MoodboardDTO.Item addLabel(Integer outputId, MoodboardDTO.AddLabel request) {
        requireBoard(outputId);
        validatePosition(request.positionX(), request.positionY());
        MoodboardDTO.LabelData data = labelData(request.text(), request.color(), request.backgroundColor(), request.fontSize());
        return createItem(outputId, LABEL, json(data), request.positionX(), request.positionY());
    }

    // Drag/drop position updates do not trigger expensive JPA entity loads.
    @Transactional
    public MoodboardDTO.Item moveItem(Integer outputId, Integer itemId, MoodboardDTO.MoveItem request) {
        requireBoard(outputId);
        validatePosition(request.positionX(), request.positionY());
        if (moodboardRepository.moveItem(outputId, itemId, request.positionX(), request.positionY(), LocalDate.now()) != 1) {
            throw new ApiException("Moodboard item not found");
        }
        return getItem(outputId, itemId);
    }

    @Transactional
    public MoodboardDTO.Item editLabel(Integer outputId, Integer itemId, MoodboardDTO.EditLabel request) {
        requireBoard(outputId);
        MoodboardDTO.LabelData data = labelData(request.text(), request.color(), request.backgroundColor(), request.fontSize());
        if (moodboardRepository.updateLabel(outputId, itemId, json(data), LocalDate.now()) != 1) {
            throw new ApiException("Moodboard label not found");
        }
        return getItem(outputId, itemId);
    }

    @Transactional
    public void deleteItem(Integer outputId, Integer itemId) {
        requireBoard(outputId);
        if (moodboardRepository.deleteItem(outputId, itemId) != 1) {
            throw new ApiException("Moodboard item not found");
        }
    }

    @Transactional(readOnly = true)
    public MoodboardDTO.Item getItem(Integer outputId, Integer itemId) {
        requireBoard(outputId);
        List<Object[]> rows = moodboardRepository.findItem(outputId, itemId);
        if (rows.isEmpty()) {
            throw new ApiException("Moodboard item not found");
        }
        return toItem(rows.get(0));
    }

    private MoodboardDTO.Item createItem(Integer outputId, String type, String data, Double x, Double y) {
        ProjectNode node = new ProjectNode();
        node.setType(type);
        node.setDataJson(data);
        node.setPositionX(x);
        node.setPositionY(y);
        node.setCreatedAt(LocalDate.now());
        node.setUpdatedAt(node.getCreatedAt());
        node.setProjectOutput(projectOutputRepository.getReferenceById(outputId));
        ProjectNode saved = moodboardRepository.saveAndFlush(node);
        return getItem(outputId, saved.getId());
    }

    // Validate all outgoing board items using simple scalar rows.
    private MoodboardDTO.BoardSummary requireBoard(Integer outputId) {
        if (outputId == null || outputId < 1) {
            throw new ApiException("Invalid moodboard ID");
        }
        List<Object[]> rows = moodboardRepository.findBoard(outputId);
        if (rows.isEmpty()) {
            throw new ApiException("Moodboard not found");
        }
        return boardSummary(rows.get(0));
    }

    private MoodboardDTO.BoardSummary boardSummary(Object[] row) {
        return new MoodboardDTO.BoardSummary((Integer) row[0], (Integer) row[1], (String) row[2]);
    }

    private MoodboardDTO.Item toItem(Object[] row) {
        String type = (String) row[1];
        try {
            if (IMAGE.equals(type)) {
                OpenverseDTO.Image image = jsonMapper.readValue((String) row[2], OpenverseDTO.Image.class);
                return new MoodboardDTO.Item((Integer) row[0], type, (Double) row[3], (Double) row[4], image, null);
            }
            if (LABEL.equals(type)) {
                MoodboardDTO.LabelData label = jsonMapper.readValue((String) row[2], MoodboardDTO.LabelData.class);
                return new MoodboardDTO.Item((Integer) row[0], type, (Double) row[3], (Double) row[4], null, label);
            }
            throw new ApiException("Unsupported moodboard item type");
        } catch (JacksonException e) {
            throw new ApiException("Saved moodboard item contains invalid JSON");
        }
    }

    private MoodboardDTO.LabelData labelData(String text, String color, String background, Integer fontSize) {
        String cleanColor = color == null || color.isBlank() ? "#FFFFFF" : color.trim();
        String cleanBackground = background == null || background.isBlank() ? "#25334D" : background.trim();
        int size = fontSize == null ? 18 : fontSize;
        if (!cleanColor.matches("#[0-9a-fA-F]{6}") || !cleanBackground.matches("#[0-9a-fA-F]{6}") || size < 12 || size > 48) {
            throw new ApiException("Invalid label style: use #RRGGBB and fontSize 12-48");
        }
        if (text == null || text.isBlank() || text.length() > 1000) {
            throw new ApiException("Label text must contain 1-1000 characters");
        }
        return new MoodboardDTO.LabelData(text.trim(), cleanColor, cleanBackground, size);
    }

    private void validatePosition(Double x, Double y) {
        if (x == null || y == null || !Double.isFinite(x) || !Double.isFinite(y) ||
                Math.abs(x) > 100000 || Math.abs(y) > 100000) {
            throw new ApiException("Position must be finite and within -100000 to 100000");
        }
    }

    private String json(Object object) {
        try {
            return jsonMapper.writeValueAsString(object);
        } catch (JacksonException e) {
            throw new ApiException("Cannot save moodboard item JSON");
        }
    }
}
