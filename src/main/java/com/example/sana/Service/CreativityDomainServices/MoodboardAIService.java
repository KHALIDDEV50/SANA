package com.example.sana.Service.CreativityDomainServices;

import com.example.sana.DTO.CreativityDomainDTOs.MoodboardAIDTO.*;
import com.example.sana.DTO.CreativityDomainDTOs.OpenverseDTO;
import com.example.sana.DTO.CreativityDomainDTOs.CreativityAIRequestDTO;
import com.example.sana.DTO.AIDTOs.AIRunResponseDTO;
import com.example.sana.Enums.AITask;
import com.example.sana.Enums.ExecutionStatus;
import com.example.sana.Enums.RepresentationType;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Repository.ProjectRepository;
import com.example.sana.Repository.ProjectOutputRepository;
import com.example.sana.Config.N8nProperties;
import com.example.sana.Service.AIServices.AIRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Moodboard AI through the existing Creativity ADVISE queue, n8n gateway and callback. */
@Service
@RequiredArgsConstructor
public class MoodboardAIService {
    private static final Logger log = LoggerFactory.getLogger(MoodboardAIService.class);
    private final ProjectRepository projects;
    private final ProjectOutputRepository outputs;
    private final CreativityAIRequestService creativityRequests;
    private final AIRequestService sharedRequests;
    private final N8nProperties n8n;
    private final JsonMapper json;
    private final OpenverseService openverse;
    private final MoodboardService moodboards;
    private final Map<String, Draft> drafts = new ConcurrentHashMap<>();

    private static final class Draft {
        final Integer projectId;
        final String session;
        final String requestId;
        volatile Preview preview;
        volatile String state = "GENERATING";
        volatile Integer savedId;
        Draft(Integer projectId, String session, String requestId, Preview preview) {
            this.projectId = projectId;
            this.session = session;
            this.requestId = requestId;
            this.preview = preview;
        }
    }

    /** Uses the same /webhook/sana/ai as STORY_MAP. NO independent webhook or API key. */
    public boolean configured() {
        return n8n.isEnabled() && n8n.isWorkerEnabled();
    }

    private static String planningPrompt(String userPrompt, String language) {
        // The existing n8n response schema admits ADVICE (a string message), not a Moodboard proposal.
        // The returned message is a *serialized* Moodboard plan and is validated before Openverse lookup.
        return "SANA_MOODBOARD_PLAN. This is an ADVISE task only, NOT a graph edit. " +
                "Do not return a graph proposal. Return an ADVICE result whose message is ONLY a compact " +
                "single valid JSON object serialized as a string. JSON keys: " +
                "title (string), theme (string), searchTerms (array of 1 to 3 concise English image search " +
                "queries), notes (array of 1 to 4 short creative notes), palette (array of 3 to 5 objects, " +
                "each with name and hex such as #26364C). " +
                "Keep under 200 words; use no Markdown, no outer output wrapper, no image URLs and no invented " +
                "Openverse identifiers. Use the supplied project context as inspiration only. " +
                "The existing Sana result outer type must be ADVICE with clarification=null and proposal=null. " +
                "Output language: " + language + ". User moodboard request: " + userPrompt;
    }

    private Plan parsePlan(String message) {
        if (message == null || message.length() > 16000) throw invalidPlan();
        String raw = message.trim();
        if (raw.startsWith("```")) {
            int newline = raw.indexOf('\n');
            int endFence = raw.lastIndexOf("```");
            if (newline < 0 || endFence <= newline) throw invalidPlan();
            raw = raw.substring(newline + 1, endFence).trim();
        }
        try {
            Plan plan = json.readerFor(Plan.class).with(
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readValue(raw);
            return validatePlan(plan);
        } catch (RuntimeException ex) {
            throw invalidPlan();
        }
    }

    private static Plan validatePlan(Plan p) {
        if (p == null) throw invalidPlan();
        requireText(p.title(), 120);
        requireText(p.theme(), 600);
        requireStrings(p.searchTerms(), 1, 3, 120);
        requireStrings(p.notes(), 1, 4, 800);
        if (p.palette() == null || p.palette().size() < 3 || p.palette().size() > 5) throw invalidPlan();
        for (Color c : p.palette()) {
            if (c == null) throw invalidPlan();
            requireText(c.name(), 60);
            if (c.hex() == null || !c.hex().matches("#[0-9a-fA-F]{6}")) throw invalidPlan();
        }
        var terms = p.searchTerms().stream().map(String::trim).distinct().toList();
        if (terms.isEmpty()) throw invalidPlan();
        return new Plan(p.title().trim(), p.theme().trim(), terms,
                p.notes().stream().map(String::trim).toList(), List.copyOf(p.palette()));
    }
    private static void requireStrings(List<String> list, int min, int max, int length) {
        if (list == null || list.size() < min || list.size() > max) throw invalidPlan();
        list.forEach(value -> requireText(value, length));
    }
    private static void requireText(String value, int limit) {
        if (value == null || value.isBlank() || value.length() > limit || value.indexOf('\0') >= 0)
            throw invalidPlan();
    }
    private static ResponseStatusException invalidPlan() {
        return fail(HttpStatus.BAD_GATEWAY,
                "Moodboard n8n planner returned invalid JSON. Check its plan response.");
    }

    public Preview generate(Integer projectId, Generate request, String session) {
        if (!configured()) throw fail(HttpStatus.SERVICE_UNAVAILABLE,
                "Sana n8n gateway/worker must be enabled. Uses the existing AI webhook.");
        var project = projects.findById(projectId).orElseThrow(() -> fail(HttpStatus.NOT_FOUND, "Project not found"));
        if (!"CREATIVITY".equals(project.getDomain())) throw fail(HttpStatus.BAD_REQUEST, "Creativity project required");
        drafts.entrySet().removeIf(e -> !e.getValue().preview.expiresAt().isAfter(Instant.now()));
        if (drafts.size() >= 100 || drafts.values().stream().filter(d -> d.session.equals(session)).count() >= 6)
            throw fail(HttpStatus.TOO_MANY_REQUESTS, "Too many pending moodboards; reject a draft or retry later.");

        // The existing AIRequestService only allows ADVISE on an existing GRAPH output.
        // Do not forge a graph or target the MOODBOARD/IMAGE representation.
        ProjectOutput graph = outputs.findAllByProject_Id(projectId).stream()
                .filter(o -> o.getRepresentationType() == RepresentationType.GRAPH)
                .filter(o -> o.getGraphRevision() != null)
                .sorted(Comparator.comparingInt(o -> "STORY_MAP".equals(o.getOutputType()) ? 0 : 1))
                .findFirst().orElseThrow(() -> fail(HttpStatus.CONFLICT,
                        "Existing Sana n8n ADVISE needs a story GRAPH output in this project. Create a story map first."));

        CreativityAIRequestDTO ai = new CreativityAIRequestDTO();
        ai.setTask(AITask.ADVISE);
        ai.setOutputId(graph.getId());
        ai.setBaseRevision(graph.getGraphRevision());
        ai.setPrompt(planningPrompt(request.prompt(), request.language()));
        ai.setSelectedElementIds(List.of());
        AIRunResponseDTO run = creativityRequests.start(projectId, ai);

        String id = UUID.randomUUID().toString();
        Instant expires = Instant.now().plus(15, ChronoUnit.MINUTES);
        Preview preview = new Preview(id, projectId, "GENERATING", expires,
                "", "", List.of(), List.of(), List.of(),
                List.of("Waiting for the existing n8n callback. This does not save a moodboard."), false, null);
        drafts.put(id, new Draft(projectId, session, run.getRequestId(), preview));
        return preview;
    }

    private Preview buildPreview(Draft d, Plan plan) {
        Integer projectId = d.projectId;
        String id = d.preview.draftId();
        Instant expires = d.preview.expiresAt();
        // Search results already contain genuine Openverse IDs, metadata and licence fields.
        // Prefer a detail lookup when available, but do not discard valid search results
        // just because the detail API is temporarily unavailable or rate-limited.
        Map<String, OpenverseDTO.Image> candidates = new LinkedHashMap<>();
        int searchFailures = 0;
        for (String term : plan.searchTerms()) {
            for (int page = 1; page <= 2 && candidates.size() < 60; page++) {
                try {
                    var response = openverse.searchImages(term, page, 20);
                    for (var image : response.images()) {
                        if (image.id() != null && image.id().matches(
                                "(?i)[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}")) {
                            candidates.putIfAbsent(image.id().toLowerCase(Locale.ROOT), image);
                        }
                    }
                    if (response.images().isEmpty()) break;
                } catch (RuntimeException ex) {
                    searchFailures++;
                    log.warn("Openverse moodboard search failed (page {}): {}", page, ex.getMessage());
                    break;
                }
            }
        }
        List<ImageReference> images = new ArrayList<>();
        boolean detailApiAvailable = true;
        int unusable = 0;
        for (var candidate : candidates.values()) {
            if (images.size() >= 6) break;
            OpenverseDTO.Image actual = candidate;
            if (detailApiAvailable) {
                try {
                    actual = openverse.getImage(candidate.id());
                } catch (RuntimeException ex) {
                    if ("Openverse image not found".equals(ex.getMessage())) {
                        unusable++;
                        continue;
                    }
                    // Only trust metadata returned by Openverse search; never invent URLs.
                    detailApiAvailable = false;
                    log.warn("Openverse image details unavailable, using its search metadata: {}", ex.getMessage());
                }
            }
            if (!candidate.id().equalsIgnoreCase(actual.id())) {
                unusable++;
                continue;
            }
            OpenverseDTO.Image safe = previewImage(actual);
            if (safe == null) {
                unusable++;
                continue;
            }
            int i = images.size();
            images.add(new ImageReference(safe, "Openverse", 30 + (i % 3) * 250,
                    35 + (i / 3) * 380));
        }
        if (images.size() < 3) {
            log.warn("Openverse moodboard insufficient references: candidates={}, usable={}, " +
                            "rejected={}, searchFailures={}",
                    candidates.size(), images.size(), unusable, searchFailures);
            throw fail(HttpStatus.BAD_GATEWAY,
                    "Openverse لم يوفر ثلاث صور صالحة للعرض. راجعي سجل Spring Boot لمعرفة السبب.");
        }

        List<Note> notes = new ArrayList<>();
        List<String> text = new ArrayList<>(); text.add(plan.theme()); text.addAll(plan.notes());
        for (int i = 0; i < text.size(); i++)
            notes.add(new Note(text.get(i), "#25334D", "#FFF0CE", 30 + (i % 3) * 250, 800 + (i / 3) * 320));
        Preview preview = new Preview(id, projectId, "PENDING", expires,
                plan.title(), plan.theme(), List.copyOf(images), List.copyOf(notes), plan.palette(),
                List.of("Images are selected by search metadata, not by AI visual inspection."), false, null);
        return preview;
    }

    public Preview get(Integer projectId, String id, String session) {
        Draft d = require(projectId, id, session);
        synchronized (d) {
            if ("GENERATING".equals(d.state)) {
                AIRunResponseDTO run = sharedRequests.getRun(projectId, UUID.fromString(d.requestId));
                if (run.getStatus() == ExecutionStatus.FAILED || run.getStatus() == ExecutionStatus.TIMED_OUT) {
                    d.state = "FAILED";
                    throw fail(HttpStatus.BAD_GATEWAY,
                            "The shared Sana n8n request failed/timed out. Check callback connectivity and n8n execution.");
                }
                if (run.getStatus() == ExecutionStatus.COMPLETED) {
                    if (run.getResult() == null || run.getResult().type() !=
                            com.example.sana.DTO.IntegrationDTOs.N8nCallbackDTO.ResultType.ADVICE) {
                        d.state = "FAILED";
                        throw fail(HttpStatus.BAD_GATEWAY,
                                "Existing Sana n8n returned no Moodboard ADVICE; no board was saved.");
                    }
                    try {
                        Plan plan = parsePlan(run.getResult().message());
                        d.preview = buildPreview(d, plan);
                        d.state = "PENDING";
                    } catch (ResponseStatusException ex) {
                        d.state = "FAILED";
                        throw ex;
                    }
                }
            }
            if ("FAILED".equals(d.state)) throw fail(HttpStatus.BAD_GATEWAY,
                    "Moodboard planning failed; request a new preview. No moodboard was saved.");
            Preview p = d.preview;
            return new Preview(p.draftId(), p.projectId(), d.state, p.expiresAt(), p.title(), p.theme(),
                    p.images(), p.notes(), p.palette(), p.warnings(), false, d.savedId);
        }
    }

    public Saved approve(Integer projectId, String id, String session) {
        Draft d = require(projectId, id, session);
        synchronized (d) {
            if ("APPROVED".equals(d.state)) return new Saved(id, d.savedId, projectId);
            if (!"PENDING".equals(d.state)) throw fail(HttpStatus.CONFLICT, "Only pending previews can be approved");
            // Existing MoodboardService owns the transaction, storage and rollback.
            var saved = moodboards.saveApprovedDraft(projectId, id, session, d.preview);
            d.savedId = saved.id();
            d.state = "APPROVED";
            return new Saved(id, saved.id(), projectId);
        }
    }

    public void reject(Integer projectId, String id, String session) {
        Draft d = require(projectId, id, session);
        synchronized (d) {
            if ("APPROVED".equals(d.state)) throw fail(HttpStatus.CONFLICT, "Already approved");
            d.state = "REJECTED";
        }
    }

    private Draft require(Integer projectId, String id, String session) {
        Draft d = drafts.get(id);
        if (d == null || !d.projectId.equals(projectId) || !d.session.equals(session))
            throw fail(HttpStatus.NOT_FOUND, "Moodboard draft not found in this session");
        if (!d.preview.expiresAt().isAfter(Instant.now())) {
            drafts.remove(id);
            throw fail(HttpStatus.GONE, "Moodboard preview expired; request a new one");
        }
        return d;
    }

    /** Require HTTPS for images (browser mixed-content safety), but do not reject a
     * valid Openverse image merely because its source or licence page uses HTTP.
     * Optional non-HTTPS thumbnails are dropped so the UI can show the HTTPS original.
     */
    private static OpenverseDTO.Image previewImage(OpenverseDTO.Image image) {
        if (image == null || image.license() == null || image.license().isBlank()
                || !publicUrl(image.sourceUrl(), true)) return null;
        String full = publicUrl(image.imageUrl(), false) ? image.imageUrl() : null;
        String thumb = publicUrl(image.thumbnail(), false) ? image.thumbnail() : null;
        if (full == null && thumb == null) return null;
        return new OpenverseDTO.Image(image.id(), image.title(), full != null ? full : thumb,
                thumb, image.creator(), image.creatorUrl(), image.license(),
                image.licenseVersion(), image.licenseUrl(), image.sourceUrl(), image.attribution());
    }

    private static boolean publicUrl(String raw, boolean allowHttp) {
        if (raw == null || raw.length() > 2048) return false;
        try {
            URI u = URI.create(raw);
            String host = u.getHost();
            String scheme = u.getScheme();
            if (host == null || u.getUserInfo() != null || scheme == null
                    || !("https".equalsIgnoreCase(scheme)
                    || (allowHttp && "http".equalsIgnoreCase(scheme)))) return false;
            host = host.toLowerCase(Locale.ROOT);
            return host.contains(".") && !host.endsWith(".localhost")
                    && !host.endsWith(".local") && !host.endsWith(".internal")
                    && !host.matches("[0-9.]+") && !host.contains(":");
        } catch (IllegalArgumentException e) { return false; }
    }
    private static ResponseStatusException fail(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
