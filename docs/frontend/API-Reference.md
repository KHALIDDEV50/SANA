# Frontend API source reference

Snapshot: 25 October 2026. This inventory is extracted from controller annotations and Java signatures; it is not a record of executed requests or an OpenAPI specification. For the AI flow (request, polling, proposal review) and the full rules of every diagram type, read [the n8n integration guide](../N8n-Backend-Integration.md).

## Reading this reference

- Paths are relative to the configured Spring Boot base URL.
- `{name}` identifies a path parameter. `@RequestParam` values belong in the query string; they are required unless the signature says `required = false`.
- `@RequestBody` identifies the JSON body type. Most writes return a message rather than the saved object. GET return shapes come from the corresponding service or response DTO.
- All signatures below are current source declarations. Generic entity CRUD and machine endpoints are included for completeness, not as preferred UI flows.
- `AIRequestController` is the AI request/review API for all four domains. Legacy AIInteraction/ChangeProposal/ChangeProposalItem CRUD must not be used to manipulate managed n8n runs/proposals.
- `DiagramController` (`/api/v1/diagram`) is the manual editor for every diagram type. Start with `GET /api/v1/diagram/types` to get each type's node and edge types. The Architecture routes are kept for existing clients and behave the same as `/api/v1/diagram` limited to ARCHITECTURE.
- Creativity diagrams (STORY_MAP, BRANCHING_STORY) are still edited through the Creativity controllers. `/api/v1/diagram` can list, read and rename them but rejects their node/edge changes and deletion.
- Generic ProjectNode/ProjectEdge/ProjectOutput writes are rejected for the write-guarded diagram types (every type except the two Creativity ones).
- `N8nCallbackController` is machine-only. Browser code must never call it or contain callback credentials.
- The Java source links, DTO field constraints, and enums are the source of truth for this checkout; none of these routes were called while writing this reference.

## AIInteractionController

Source: [AIInteractionController.java](../../src/main/java/com/example/sana/Controller/AIInteractionController.java).

| Method | Path | Handler parameters from source |
|---|---|---|
| GET | `/api/v1/ai-interaction/get` | `None` |
| GET | `/api/v1/ai-interaction/get/{aiInteractionId}` | `@PathVariable Integer aiInteractionId` |
| POST | `/api/v1/ai-interaction/add/{userId}/{projectId}` | `@PathVariable Integer userId, @PathVariable Integer projectId, @Valid @RequestBody AIInteraction aiInteraction` |
| PUT | `/api/v1/ai-interaction/update/{aiInteractionId}` | `@PathVariable Integer aiInteractionId, @Valid @RequestBody AIInteraction aiInteraction` |
| DELETE | `/api/v1/ai-interaction/delete/{aiInteractionId}` | `@PathVariable Integer aiInteractionId` |

## BusinessController

Source: [BusinessController.java](../../src/main/java/com/example/sana/Controller/BusinessController.java).

| Method | Path | Handler parameters from source |
|---|---|---|
| POST | `/api/business/tools` | `@Valid @RequestBody BusinessToolRequestDTO request` |

## ChangeProposalController

Source: [ChangeProposalController.java](../../src/main/java/com/example/sana/Controller/ChangeProposalController.java).

| Method | Path | Handler parameters from source |
|---|---|---|
| GET | `/api/v1/change-proposal/get` | `None` |
| GET | `/api/v1/change-proposal/get/{proposalId}` | `@PathVariable Integer proposalId` |
| POST | `/api/v1/change-proposal/add/{projectId}` | `@PathVariable Integer projectId, @RequestParam(required = false) Integer aiInteractionId, @Valid @RequestBody ChangeProposal changeProposal` |
| PUT | `/api/v1/change-proposal/update/{proposalId}` | `@PathVariable Integer proposalId, @Valid @RequestBody ChangeProposal changeProposal` |
| DELETE | `/api/v1/change-proposal/delete/{proposalId}` | `@PathVariable Integer proposalId` |

## ChangeProposalItemController

Source: [ChangeProposalItemController.java](../../src/main/java/com/example/sana/Controller/ChangeProposalItemController.java).

| Method | Path | Handler parameters from source |
|---|---|---|
| GET | `/api/v1/change-proposal-item/get` | `None` |
| GET | `/api/v1/change-proposal-item/get/{itemId}` | `@PathVariable Integer itemId` |
| POST | `/api/v1/change-proposal-item/add/{proposalId}` | `@PathVariable Integer proposalId, @Valid @RequestBody ChangeProposalItem changeProposalItem` |
| PUT | `/api/v1/change-proposal-item/update/{itemId}` | `@PathVariable Integer itemId, @Valid @RequestBody ChangeProposalItem changeProposalItem` |
| DELETE | `/api/v1/change-proposal-item/delete/{itemId}` | `@PathVariable Integer itemId` |

## BranchingNarrativeController

Source: [BranchingNarrativeController.java](../../src/main/java/com/example/sana/Controller/CreativityDomainController/BranchingNarrativeController.java).

| Method | Path | Handler parameters from source |
|---|---|---|
| GET | `/api/v1/branching/get/{outputId}/nodes` | `@PathVariable Integer outputId` |
| GET | `/api/v1/branching/get/{outputId}/nodes/{nodeId}` | `@PathVariable Integer outputId, @PathVariable Integer nodeId` |
| POST | `/api/v1/branching/add/{outputId}/nodes` | `@PathVariable Integer outputId, @RequestBody @Valid BranchingNodeDTO dto` |
| PUT | `/api/v1/branching/update/{outputId}/nodes/{nodeId}` | `@PathVariable Integer outputId, @PathVariable Integer nodeId, @RequestBody @Valid BranchingNodeDTO dto` |
| DELETE | `/api/v1/branching/delete/{outputId}/nodes/{nodeId}` | `@PathVariable Integer outputId, @PathVariable Integer nodeId` |
| POST | `/api/v1/branching/add/{outputId}/choices/{sourceNodeId}/{targetNodeId}` | `@PathVariable Integer outputId, @PathVariable Integer sourceNodeId, @PathVariable Integer targetNodeId, @RequestBody @Valid BranchingChoiceDTO dto` |
| PUT | `/api/v1/branching/update/{outputId}/choices/{choiceId}/to/{targetNodeId}` | `@PathVariable Integer outputId, @PathVariable Integer choiceId, @PathVariable Integer targetNodeId, @RequestBody @Valid BranchingChoiceDTO dto` |
| DELETE | `/api/v1/branching/delete/{outputId}/choices/{choiceId}` | `@PathVariable Integer outputId, @PathVariable Integer choiceId` |
| GET | `/api/v1/branching/play/{outputId}/start/{nodeId}` | `@PathVariable Integer outputId, @PathVariable Integer nodeId` |
| POST | `/api/v1/branching/play/{outputId}/{nodeId}/choose/{choiceId}` | `@PathVariable Integer outputId, @PathVariable Integer nodeId, @PathVariable Integer choiceId, @RequestBody @Valid BranchingPlayStateDTO dto` |
| GET | `/api/v1/branching/get/{outputId}/map` | `@PathVariable Integer outputId` |
| PUT | `/api/v1/branching/update/{outputId}/nodes/{nodeId}/position` | `@PathVariable Integer outputId, @PathVariable Integer nodeId, @RequestBody @Valid BranchingPositionDTO dto` |
| PUT | `/api/v1/branching/update/{outputId}/layout/nodes/{nodeIds}` | `@PathVariable Integer outputId, @PathVariable List<Integer> nodeIds, @RequestBody @Valid List<@Valid BranchingPositionDTO> positions` |
| PUT | `/api/v1/branching/link/{outputId}/nodes/{nodeId}/speaker/{characterId}` | `@PathVariable Integer outputId, @PathVariable Integer nodeId, @PathVariable Integer characterId` |
| DELETE | `/api/v1/branching/unlink/{outputId}/nodes/{nodeId}/speaker` | `@PathVariable Integer outputId, @PathVariable Integer nodeId` |
| PUT | `/api/v1/branching/link/{outputId}/nodes/{nodeId}/event/{eventId}` | `@PathVariable Integer outputId, @PathVariable Integer nodeId, @PathVariable Integer eventId` |
| DELETE | `/api/v1/branching/unlink/{outputId}/nodes/{nodeId}/event` | `@PathVariable Integer outputId, @PathVariable Integer nodeId` |
| GET | `/api/v1/branching/get/{outputId}/nodes/{nodeId}/references` | `@PathVariable Integer outputId, @PathVariable Integer nodeId` |
| GET | `/api/v1/branching/get/{outputId}/variables` | `@PathVariable Integer outputId` |
| POST | `/api/v1/branching/add/{outputId}/variables` | `@PathVariable Integer outputId, @RequestBody @Valid BranchingVariableDTO dto` |
| PUT | `/api/v1/branching/update/{outputId}/variables/{variableId}` | `@PathVariable Integer outputId, @PathVariable Integer variableId, @RequestBody @Valid BranchingVariableDTO dto` |
| DELETE | `/api/v1/branching/delete/{outputId}/variables/{variableId}` | `@PathVariable Integer outputId, @PathVariable Integer variableId` |
| PUT | `/api/v1/branching/set/{outputId}/choices/{choiceId}/condition/{variableId}` | `@PathVariable Integer outputId, @PathVariable Integer choiceId, @PathVariable Integer variableId, @RequestBody @Valid BranchingConditionDTO dto` |
| DELETE | `/api/v1/branching/remove/{outputId}/choices/{choiceId}/condition` | `@PathVariable Integer outputId, @PathVariable Integer choiceId` |
| PUT | `/api/v1/branching/set/{outputId}/choices/{choiceId}/effect/{variableId}` | `@PathVariable Integer outputId, @PathVariable Integer choiceId, @PathVariable Integer variableId, @RequestBody @Valid BranchingEffectDTO dto` |
| DELETE | `/api/v1/branching/remove/{outputId}/choices/{choiceId}/effect` | `@PathVariable Integer outputId, @PathVariable Integer choiceId` |
| GET | `/api/v1/branching/get/{outputId}/choices/{choiceId}/settings` | `@PathVariable Integer outputId, @PathVariable Integer choiceId` |

## CharacterBalanceController

Source: [CharacterBalanceController.java](../../src/main/java/com/example/sana/Controller/CreativityDomainController/CharacterBalanceController.java).

| Method | Path | Handler parameters from source |
|---|---|---|
| GET | `/api/v1/character-balance/get/{outputId}/characters` | `@PathVariable Integer outputId` |
| GET | `/api/v1/character-balance/{outputId}/characters/{characterId}` | `@PathVariable Integer outputId, @PathVariable Integer characterId` |
| POST | `/api/v1/character-balance/{outputId}/characters/add` | `@PathVariable Integer outputId, @RequestBody @Valid PlayableCharacterDTO dto` |
| PUT | `/api/v1/character-balance/{outputId}/characters/update/{characterId}` | `@PathVariable Integer outputId, @PathVariable Integer characterId, @RequestBody @Valid PlayableCharacterDTO dto` |
| DELETE | `/api/v1/character-balance/{outputId}/characters/delete/{characterId}` | `@PathVariable Integer outputId, @PathVariable Integer characterId` |
| PUT | `/api/v1/character-balance/{outputId}/characters/{characterId}/profile/{profileId}` | `@PathVariable Integer outputId, @PathVariable Integer characterId, @PathVariable Integer profileId` |
| DELETE | `/api/v1/character-balance/{outputId}/characters/{characterId}/profile` | `@PathVariable Integer outputId, @PathVariable Integer characterId` |
| GET | `/api/v1/character-balance/{outputId}/analyze` | `@PathVariable Integer outputId` |

## CharacterProfileController

Source: [CharacterProfileController.java](../../src/main/java/com/example/sana/Controller/CreativityDomainController/CharacterProfileController.java).

| Method | Path | Handler parameters from source |
|---|---|---|
| GET | `/api/v1/character-profile/get` | `None` |
| GET | `/api/v1/character-profile/get/by/{id}` | `@PathVariable Integer id` |
| POST | `/api/v1/character-profile/add/{outputId}` | `@PathVariable Integer outputId, @RequestBody @Valid CharacterProfileDTO dto` |
| PUT | `/api/v1/character-profile/update/{id}` | `@PathVariable Integer id, @RequestBody @Valid CharacterProfileDTO dto` |
| DELETE | `/api/v1/character-profile/delete/{id}` | `@PathVariable Integer id` |

## CreativityRelationController

Source: [CreativityRelationController.java](../../src/main/java/com/example/sana/Controller/CreativityDomainController/CreativityRelationController.java).

| Method | Path | Handler parameters from source |
|---|---|---|
| GET | `/api/v1/creativity/relations/get/{outputId}` | `@PathVariable Integer outputId` |
| GET | `/api/v1/creativity/relations/get/{outputId}/node/{nodeId}` | `@PathVariable Integer outputId, @PathVariable Integer nodeId` |
| GET | `/api/v1/creativity/relations/get/types/{outputId}/{sourceNodeId}/{targetNodeId}` | `@PathVariable Integer outputId, @PathVariable Integer sourceNodeId, @PathVariable Integer targetNodeId` |
| POST | `/api/v1/creativity/relations/add/{outputId}/{sourceNodeId}/{targetNodeId}` | `@PathVariable Integer outputId, @PathVariable Integer sourceNodeId, @PathVariable Integer targetNodeId, @RequestBody @Valid CreativityRelationCreateDTO dto` |
| GET | `/api/v1/creativity/relations/delete/preview/{outputId}/node/{nodeId}` | `@PathVariable Integer outputId, @PathVariable Integer nodeId` |

## CreativitySearchController

Source: [CreativitySearchController.java](../../src/main/java/com/example/sana/Controller/CreativityDomainController/CreativitySearchController.java).

| Method | Path | Handler parameters from source |
|---|---|---|
| GET | `/api/v1/creativity/search/{outputId}` | `@PathVariable Integer outputId, @RequestParam(required = false) String keyword, @RequestParam(required = false) String type` |

## LocationController

Source: [LocationController.java](../../src/main/java/com/example/sana/Controller/CreativityDomainController/LocationController.java).

| Method | Path | Handler parameters from source |
|---|---|---|
| GET | `/api/v1/location/get` | `None` |
| GET | `/api/v1/location/get/{id}` | `@PathVariable Integer id` |
| POST | `/api/v1/location/add/{outputId}` | `@PathVariable Integer outputId, @RequestBody @Valid LocationDTO dto` |
| POST | `/api/v1/location/add/{outputId}/world/{worldId}` | `@PathVariable Integer outputId, @PathVariable Integer worldId, @RequestBody @Valid LocationDTO dto` |
| POST | `/api/v1/location/add/{outputId}/parent/{parentLocationId}` | `@PathVariable Integer outputId, @PathVariable Integer parentLocationId, @RequestBody @Valid LocationDTO dto` |
| POST | `/api/v1/location/add/{outputId}/world/{worldId}/parent/{parentLocationId}` | `@PathVariable Integer outputId, @PathVariable Integer worldId, @PathVariable Integer parentLocationId, @RequestBody @Valid LocationDTO dto` |
| PUT | `/api/v1/location/update/{id}` | `@PathVariable Integer id, @RequestBody @Valid LocationDTO dto` |
| PUT | `/api/v1/location/update/{locationId}/world/{worldId}` | `@PathVariable Integer locationId, @PathVariable Integer worldId` |
| PUT | `/api/v1/location/update/{locationId}/parent/{parentLocationId}` | `@PathVariable Integer locationId, @PathVariable Integer parentLocationId` |
| DELETE | `/api/v1/location/delete/{id}` | `@PathVariable Integer id` |

## OpenLibraryController

Source: [OpenLibraryController.java](../../src/main/java/com/example/sana/Controller/CreativityDomainController/OpenLibraryController.java).

| Method | Path | Handler parameters from source |
|---|---|---|
| GET | `/api/v1/creativity/books/search` | `@RequestParam String keyword, @RequestParam(required = false) Integer maxResults, @RequestParam(required = false) Integer startIndex` |
| GET | `/api/v1/creativity/books/get/{workId}` | `@PathVariable String workId` |

## StoryEventController

Source: [StoryEventController.java](../../src/main/java/com/example/sana/Controller/CreativityDomainController/StoryEventController.java).

| Method | Path | Handler parameters from source |
|---|---|---|
| GET | `/api/v1/story-event/get` | `None` |
| GET | `/api/v1/story-event/get/{id}` | `@PathVariable Integer id` |
| POST | `/api/v1/story-event/add/{outputId}` | `@PathVariable Integer outputId, @RequestBody @Valid StoryEventDTO dto` |
| PUT | `/api/v1/story-event/update/{id}` | `@PathVariable Integer id, @RequestBody @Valid StoryEventDTO dto` |
| DELETE | `/api/v1/story-event/delete/{id}` | `@PathVariable Integer id` |
| GET | `/api/v1/story-event/timeline/presentation/{outputId}` | `@PathVariable Integer outputId` |
| GET | `/api/v1/story-event/timeline/chronological/{outputId}` | `@PathVariable Integer outputId` |

## WorldController

Source: [WorldController.java](../../src/main/java/com/example/sana/Controller/CreativityDomainController/WorldController.java).

| Method | Path | Handler parameters from source |
|---|---|---|
| GET | `/api/v1/world/get` | `None` |
| GET | `/api/v1/world/get/{id}` | `@PathVariable Integer id` |
| POST | `/api/v1/world/add/{outputId}` | `@PathVariable Integer outputId, @RequestBody @Valid WorldDTO dto` |
| PUT | `/api/v1/world/update/{id}` | `@PathVariable Integer id, @RequestBody @Valid WorldDTO dto` |
| DELETE | `/api/v1/world/delete/{id}` | `@PathVariable Integer id` |

## WorldRuleController

Source: [WorldRuleController.java](../../src/main/java/com/example/sana/Controller/CreativityDomainController/WorldRuleController.java).

| Method | Path | Handler parameters from source |
|---|---|---|
| GET | `/api/v1/world-rule/get` | `None` |
| GET | `/api/v1/world-rule/get/{id}` | `@PathVariable Integer id` |
| POST | `/api/v1/world-rule/add/{outputId}` | `@PathVariable Integer outputId, @RequestBody @Valid WorldRuleDTO dto` |
| POST | `/api/v1/world-rule/add/{outputId}/world/{worldId}` | `@PathVariable Integer outputId, @PathVariable Integer worldId, @RequestBody @Valid WorldRuleDTO dto` |
| PUT | `/api/v1/world-rule/update/{id}` | `@PathVariable Integer id, @RequestBody @Valid WorldRuleDTO dto` |
| PUT | `/api/v1/world-rule/update/{ruleId}/world/{worldId}` | `@PathVariable Integer ruleId, @PathVariable Integer worldId` |
| DELETE | `/api/v1/world-rule/delete/{id}` | `@PathVariable Integer id` |

## YarnExportController

Source: [YarnExportController.java](../../src/main/java/com/example/sana/Controller/CreativityDomainController/YarnExportController.java).

| Method | Path | Handler parameters from source |
|---|---|---|
| GET | `/api/v1/branching/export/yarn/{outputId}` | `@PathVariable Integer outputId` |
| GET | `/api/v1/branching/export/yarn/{outputId}/start/{startNodeId}` | `@PathVariable Integer outputId, @PathVariable Integer startNodeId` |

## N8nCallbackController

Source: [N8nCallbackController.java](../../src/main/java/com/example/sana/Controller/IntegrationControllers/N8nCallbackController.java).

| Method | Path | Handler parameters from source |
|---|---|---|
| POST | `/api/integrations/n8n/ai/callback` | `@RequestBody String body` (machine-only JSON contract; requires X-Sana-Callback-Key) |

## ProjectController

Source: [ProjectController.java](../../src/main/java/com/example/sana/Controller/ProjectController.java).

| Method | Path | Handler parameters from source |
|---|---|---|
| GET | `/api/v1/project/get` | `None` |
| GET | `/api/v1/project/get/{projectId}` | `@PathVariable Integer projectId` |
| GET | `/api/v1/project/get/user/{userId}` | `@PathVariable Integer userId` |
| POST | `/api/v1/project/add/{userId}` | `@PathVariable Integer userId, @Valid @RequestBody Project project` |
| PUT | `/api/v1/project/update/{projectId}` | `@PathVariable Integer projectId, @Valid @RequestBody Project project` |
| DELETE | `/api/v1/project/delete/{projectId}` | `@PathVariable Integer projectId` |

## ProjectEdgeController

Source: [ProjectEdgeController.java](../../src/main/java/com/example/sana/Controller/ProjectEdgeController.java).

| Method | Path | Handler parameters from source |
|---|---|---|
| GET | `/api/v1/edge/get` | `None` |
| POST | `/api/v1/edge/add/{outputId}/{sourceNodeId}/{targetNodeId}` | `@PathVariable Integer outputId, @PathVariable Integer sourceNodeId, @PathVariable Integer targetNodeId, @Valid @RequestBody ProjectEdge projectEdge` |
| PUT | `/api/v1/edge/update/{projectEdgeId}` | `@PathVariable Integer projectEdgeId, @Valid @RequestBody ProjectEdge projectEdge` |
| DELETE | `/api/v1/edge/delete/{edgeId}` | `@PathVariable Integer edgeId` |

## ProjectNodeController

Source: [ProjectNodeController.java](../../src/main/java/com/example/sana/Controller/ProjectNodeController.java).

| Method | Path | Handler parameters from source |
|---|---|---|
| GET | `/api/v1/node/get` | `None` |
| POST | `/api/v1/node/add/{outputId}` | `@PathVariable Integer outputId, @Valid @RequestBody ProjectNode projectNode` |
| PUT | `/api/v1/node/update/{projectNodeId}` | `@PathVariable Integer projectNodeId, @Valid @RequestBody ProjectNode projectNode` |
| DELETE | `/api/v1/node/delete/{nodeId}` | `@PathVariable Integer nodeId` |

## ProjectOutputController

Source: [ProjectOutputController.java](../../src/main/java/com/example/sana/Controller/ProjectOutputController.java).

| Method | Path | Handler parameters from source |
|---|---|---|
| GET | `/api/v1/project-output/get` | `None` |
| GET | `/api/v1/project-output/get/{outputId}` | `@PathVariable Integer outputId` |
| GET | `/api/v1/project-output/get/project/{projectId}` | `@PathVariable Integer projectId` |
| POST | `/api/v1/project-output/add/{projectId}` | `@PathVariable Integer projectId, @Valid @RequestBody ProjectOutput projectOutput` |
| PUT | `/api/v1/project-output/update/{outputId}` | `@PathVariable Integer outputId, @Valid @RequestBody ProjectOutput projectOutput` |
| DELETE | `/api/v1/project-output/delete/{outputId}` | `@PathVariable Integer outputId` |

## ProjectRecordController

Source: [ProjectRecordController.java](../../src/main/java/com/example/sana/Controller/ProjectRecordController.java).

| Method | Path | Handler parameters from source |
|---|---|---|
| GET | `/api/v1/record/get` | `None` |
| POST | `/api/v1/record/add/{projectId}` | `@PathVariable Integer projectId, @Valid @RequestBody ProjectRecord projectRecord` |
| PUT | `/api/v1/record/update/{projectRecordId}` | `@PathVariable Integer projectRecordId, @Valid @RequestBody ProjectRecord projectRecord` |
| DELETE | `/api/v1/record/delete/{projectRecordId}` | `@PathVariable Integer projectRecordId` |

## ProjectUnderstandingController

Source: [ProjectUnderstandingController.java](../../src/main/java/com/example/sana/Controller/ProjectUnderstandingController.java).

| Method | Path | Handler parameters from source |
|---|---|---|
| GET | `/api/v1/project-understanding/get` | `None` |
| GET | `/api/v1/project-understanding/get/{id}` | `@PathVariable Integer id` |
| GET | `/api/v1/project-understanding/get/project/{projectId}` | `@PathVariable Integer projectId` |
| POST | `/api/v1/project-understanding/add/{projectId}` | `@PathVariable Integer projectId, @Valid @RequestBody ProjectUnderstanding projectUnderstanding` |
| PUT | `/api/v1/project-understanding/update/{id}` | `@PathVariable Integer id, @Valid @RequestBody ProjectUnderstanding projectUnderstanding` |
| DELETE | `/api/v1/project-understanding/delete/{id}` | `@PathVariable Integer id` |

## AIRequestController

Source: [AIRequestController.java](../../src/main/java/com/example/sana/Controller/AIRequestController.java).

| Method | Path | Handler parameters from source |
|---|---|---|
| POST | `/api/projects/{projectId}/ai/requests` | `@PathVariable Integer projectId, @Valid @RequestBody AIRequestDTO request` |
| GET | `/api/projects/{projectId}/ai/requests/{requestId}` | `@PathVariable Integer projectId, @PathVariable UUID requestId` |
| GET | `/api/projects/{projectId}/ai/proposals/{proposalId}` | `@PathVariable Integer projectId, @PathVariable Integer proposalId` |
| POST | `/api/projects/{projectId}/ai/proposals/{proposalId}/accept` | `@PathVariable Integer projectId, @PathVariable Integer proposalId` |
| POST | `/api/projects/{projectId}/ai/proposals/{proposalId}/reject` | `@PathVariable Integer projectId, @PathVariable Integer proposalId` |

## DiagramController

Source: [DiagramController.java](../../src/main/java/com/example/sana/Controller/DiagramController.java).

| Method | Path | Handler parameters from source |
|---|---|---|
| GET | `/api/v1/diagram/types` | `None` |
| GET | `/api/v1/diagram/get/project/{projectId}` | `@PathVariable Integer projectId, @RequestParam(required = false) String diagramType` |
| GET | `/api/v1/diagram/get/{outputId}` | `@PathVariable Integer outputId` |
| GET | `/api/v1/diagram/graph/get/{outputId}` | `@PathVariable Integer outputId` |
| POST | `/api/v1/diagram/add/{projectId}` | `@PathVariable Integer projectId, @Valid @RequestBody DiagramRequestDTO dto` |
| PUT | `/api/v1/diagram/update/{outputId}` | `@PathVariable Integer outputId, @Valid @RequestBody DiagramRequestDTO dto, @RequestParam Integer expectedRevision` |
| DELETE | `/api/v1/diagram/delete/{outputId}` | `@PathVariable Integer outputId, @RequestParam Integer expectedRevision` |
| GET | `/api/v1/diagram/node/get/{outputId}/{nodeId}` | `@PathVariable Integer outputId, @PathVariable Integer nodeId` |
| POST | `/api/v1/diagram/node/add/{outputId}` | `@PathVariable Integer outputId, @Valid @RequestBody DiagramNodeRequestDTO dto, @RequestParam Integer expectedRevision` |
| PUT | `/api/v1/diagram/node/update/{outputId}/{nodeId}` | `@PathVariable Integer outputId, @PathVariable Integer nodeId, @Valid @RequestBody DiagramNodeRequestDTO dto, @RequestParam Integer expectedRevision` |
| DELETE | `/api/v1/diagram/node/delete/{outputId}/{nodeId}` | `@PathVariable Integer outputId, @PathVariable Integer nodeId, @RequestParam Integer expectedRevision` |
| GET | `/api/v1/diagram/edge/get/{outputId}/{edgeId}` | `@PathVariable Integer outputId, @PathVariable Integer edgeId` |
| POST | `/api/v1/diagram/edge/add/{outputId}` | `@PathVariable Integer outputId, @Valid @RequestBody DiagramEdgeRequestDTO dto, @RequestParam Integer expectedRevision` |
| PUT | `/api/v1/diagram/edge/update/{outputId}/{edgeId}` | `@PathVariable Integer outputId, @PathVariable Integer edgeId, @Valid @RequestBody DiagramEdgeRequestDTO dto, @RequestParam Integer expectedRevision` |
| DELETE | `/api/v1/diagram/edge/delete/{outputId}/{edgeId}` | `@PathVariable Integer outputId, @PathVariable Integer edgeId, @RequestParam Integer expectedRevision` |

`GET /graph/get/{outputId}` returns `{outputId, graphRevision, graph}`; send that `graphRevision` as `expectedRevision` on the next write. A stale revision returns 409. For Creativity diagrams the graph's node properties are the profile/scene fields.

## ArchitectureDiagramController

Source: [ArchitectureDiagramController.java](../../src/main/java/com/example/sana/Controller/TechDomainControllers/ArchitectureDiagramController.java). Kept for existing clients; same behavior as `/api/v1/diagram` limited to ARCHITECTURE outputs.

| Method | Path | Handler parameters from source |
|---|---|---|
| GET | `/api/v1/tech/architecture/graph/get/{outputId}` | `@PathVariable Integer outputId` |
| GET | `/api/v1/tech/architecture/get` | `None` |
| GET | `/api/v1/tech/architecture/get/{outputId}` | `@PathVariable Integer outputId` |
| GET | `/api/v1/tech/architecture/get/project/{projectId}` | `@PathVariable Integer projectId` |
| POST | `/api/v1/tech/architecture/add/{projectId}` | `@PathVariable Integer projectId, @Valid @RequestBody DiagramRequestDTO dto` |
| PUT | `/api/v1/tech/architecture/update/{outputId}` | `@PathVariable Integer outputId, @Valid @RequestBody DiagramRequestDTO dto, @RequestParam Integer expectedRevision` |
| DELETE | `/api/v1/tech/architecture/delete/{outputId}` | `@PathVariable Integer outputId, @RequestParam Integer expectedRevision` |
| GET | `/api/v1/tech/architecture/node/get/{outputId}/{nodeId}` | `@PathVariable Integer outputId, @PathVariable Integer nodeId` |
| POST | `/api/v1/tech/architecture/node/add/{outputId}` | `@PathVariable Integer outputId, @Valid @RequestBody DiagramNodeRequestDTO dto, @RequestParam Integer expectedRevision` |
| PUT | `/api/v1/tech/architecture/node/update/{outputId}/{nodeId}` | `@PathVariable Integer outputId, @PathVariable Integer nodeId, @Valid @RequestBody DiagramNodeRequestDTO dto, @RequestParam Integer expectedRevision` |
| DELETE | `/api/v1/tech/architecture/node/delete/{outputId}/{nodeId}` | `@PathVariable Integer outputId, @PathVariable Integer nodeId, @RequestParam Integer expectedRevision` |
| GET | `/api/v1/tech/architecture/edge/get/{outputId}/{edgeId}` | `@PathVariable Integer outputId, @PathVariable Integer edgeId` |
| POST | `/api/v1/tech/architecture/edge/add/{outputId}` | `@PathVariable Integer outputId, @Valid @RequestBody DiagramEdgeRequestDTO dto, @RequestParam Integer expectedRevision` |
| PUT | `/api/v1/tech/architecture/edge/update/{outputId}/{edgeId}` | `@PathVariable Integer outputId, @PathVariable Integer edgeId, @Valid @RequestBody DiagramEdgeRequestDTO dto, @RequestParam Integer expectedRevision` |
| DELETE | `/api/v1/tech/architecture/edge/delete/{outputId}/{edgeId}` | `@PathVariable Integer outputId, @PathVariable Integer edgeId, @RequestParam Integer expectedRevision` |

## ToolRunController

Source: [ToolRunController.java](../../src/main/java/com/example/sana/Controller/ToolRunController.java).

| Method | Path | Handler parameters from source |
|---|---|---|
| GET | `/api/v1/tool-run/get` | `None` |
| GET | `/api/v1/tool-run/get/{toolRunId}` | `@PathVariable Integer toolRunId` |
| POST | `/api/v1/tool-run/add/{userId}/{projectId}` | `@PathVariable Integer userId, @PathVariable Integer projectId, @Valid @RequestBody ToolRun toolRun` |
| PUT | `/api/v1/tool-run/update/{toolRunId}` | `@PathVariable Integer toolRunId, @Valid @RequestBody ToolRun toolRun` |
| DELETE | `/api/v1/tool-run/delete/{toolRunId}` | `@PathVariable Integer toolRunId` |

## UserController

Source: [UserController.java](../../src/main/java/com/example/sana/Controller/UserController.java).

| Method | Path | Handler parameters from source |
|---|---|---|
| GET | `/api/v1/user/get` | `None` |
| POST | `/api/v1/user/add` | `@Valid @RequestBody User user` |
| PUT | `/api/v1/user/update/{userId}` | `@PathVariable Integer userId, @Valid @RequestBody User user` |
| DELETE | `/api/v1/user/delete/{userId}` | `@PathVariable Integer userId` |

## Request DTO field reference

These are source fields, not response objects. Validation annotations are reproduced as declared. No annotation on a field does not guarantee unrestricted values: service/domain validation and database columns may impose additional rules. Lists/sets, entity responses, and record DTOs require the specific source contracts linked below.

### AIRequestDTO

[AIRequestDTO.java](../../src/main/java/com/example/sana/DTO/AIDTOs/AIRequestDTO.java). Body of `POST /api/projects/{projectId}/ai/requests` for every domain.

| Field | Java type | Declared validation |
|---|---|---|
| `task` | `AITask` | `@NotNull(message = "AI task is required")`; CREATE, EDIT or ADVISE |
| `prompt` | `String` | `@NotBlank(message = "AI prompt is required")`; `@Size(max = 10000, message = "AI prompt must not exceed 10000 characters")` |
| `diagramType` | `String` | `@Size(max = 50, message = "Diagram type must not exceed 50 characters")`. Required for CREATE except in TECH projects, where it defaults to ARCHITECTURE. Must be a type of the project's domain (see [Diagram types](#diagram-types)). For EDIT/ADVISE it is optional and, if sent, must match the output |
| `outputId` | `Integer` | `@Positive(message = "Output ID must be greater than zero")`; required for EDIT and ADVISE, forbidden for CREATE |
| `baseRevision` | `Integer` | `@PositiveOrZero(message = "Base revision cannot be negative")`; optional; if sent it must equal the output's current graphRevision |
| `selectedElementIds` | `List<String>` | At most 100 entries; each must be nonblank and use node:<positive ID> or edge:<positive ID>; service also rejects duplicates. Forbidden for CREATE |

The domain always comes from the stored project. CREATE and EDIT need a diagram type with rules; ADVISE works on any graph output.

### DiagramRequestDTO

[DiagramRequestDTO.java](../../src/main/java/com/example/sana/DTO/DiagramDTOs/DiagramRequestDTO.java). Used by `/api/v1/diagram` and the Architecture routes.

| Field | Java type | Declared validation |
|---|---|---|
| `title` | `String` | `@NotBlank(message = "Diagram title is required")`; `@Size(max = 255, message = "Diagram title must not exceed 255 characters")` |
| `diagramType` | `String` | `@Size(max = 50, message = "Diagram type must not exceed 50 characters")`. Required by `POST /api/v1/diagram/add/{projectId}`; the Architecture routes set it themselves; ignored on update |

### DiagramNodeRequestDTO

[DiagramNodeRequestDTO.java](../../src/main/java/com/example/sana/DTO/DiagramDTOs/DiagramNodeRequestDTO.java)

| Field | Java type | Declared validation |
|---|---|---|
| `type` | `String` | `@NotBlank(message = "Node type is required")`; `@Size(max = 50, message = "Node type must not exceed 50 characters")`; must be a node type of the diagram |
| `label` | `String` | `@NotBlank(message = "Node label is required")`; `@Size(max = 255, message = "Node label must not exceed 255 characters")` |
| `positionX` | `Double` | `@NotNull(message = "Node position X is required")` |
| `positionY` | `Double` | `@NotNull(message = "Node position Y is required")` |
| `data` | `Map<String, Object>` | Extra node properties, for example `description`, ERD `attributes` or class `methods`. Checked by the diagram's rules |

### DiagramEdgeRequestDTO

[DiagramEdgeRequestDTO.java](../../src/main/java/com/example/sana/DTO/DiagramDTOs/DiagramEdgeRequestDTO.java)

| Field | Java type | Declared validation |
|---|---|---|
| `type` | `String` | `@NotBlank(message = "Edge type is required")`; `@Size(max = 50, message = "Edge type must not exceed 50 characters")`; must be an edge type of the diagram |
| `label` | `String` | `@Size(max = 50, message = "Edge label must not exceed 50 characters")`. Optional; the edge type is used when it is empty |
| `sourceNodeId` | `Integer` | `@NotNull(message = "Source node ID is required")`; `@Positive(message = "Source node ID must be greater than zero")` |
| `targetNodeId` | `Integer` | `@NotNull(message = "Target node ID is required")`; `@Positive(message = "Target node ID must be greater than zero")` |
| `data` | `Map<String, Object>` | Extra edge properties such as `description` or class-diagram `multiplicity` |

After every manual node or edge change, the whole diagram is re-checked against its rules. A change that would leave the diagram invalid is rejected with 400 and nothing is saved.

### CharacterProfileDTO

[CharacterProfileDTO.java](../../src/main/java/com/example/sana/DTO/CreativityDomainDTOs/CharacterProfileDTO.java)

| Field | Java type | Declared validation |
|---|---|---|
| `positionX` | `Double` | `@NotNull(message = "Position X cannot be null")` |
| `positionY` | `Double` | `@NotNull(message = "Position Y cannot be null")` |
| `name` | `String` | `@NotEmpty(message = "Name cannot be empty")` |
| `shortDescription` | `String` | `@NotEmpty(message = "Description cannot be empty")` |
| `role` | `String` | `@NotEmpty(message = "Role cannot be empty")` |
| `aliases` | `String` | See service rules |
| `age` | `Integer` | `@NotNull(message = "Age cannot be null")`; `@Positive(message = "Age must be positive")` |
| `appearance` | `String` | See service rules |
| `traits` | `String` | See service rules |
| `strengths` | `String` | See service rules |
| `weaknesses` | `String` | See service rules |
| `skills` | `String` | See service rules |
| `history` | `String` | See service rules |
| `goals` | `String` | See service rules |
| `fears` | `String` | See service rules |
| `innerConflict` | `String` | See service rules |
| `secrets` | `String` | See service rules |
| `characterValues` | `String` | See service rules |
| `characterArc` | `String` | See service rules |
| `writerNotes` | `String` | See service rules |

### WorldDTO

[WorldDTO.java](../../src/main/java/com/example/sana/DTO/CreativityDomainDTOs/WorldDTO.java)

| Field | Java type | Declared validation |
|---|---|---|
| `positionX` | `Double` | `@NotNull(message = "Position X cannot be null")` |
| `positionY` | `Double` | `@NotNull(message = "Position Y cannot be null")` |
| `name` | `String` | `@NotEmpty(message = "Name cannot be empty")` |
| `description` | `String` | `@NotEmpty(message = "Description cannot be empty")` |
| `history` | `String` | See service rules |
| `lore` | `String` | See service rules |
| `atmosphere` | `String` | See service rules |
| `notes` | `String` | See service rules |

### WorldRuleDTO

[WorldRuleDTO.java](../../src/main/java/com/example/sana/DTO/CreativityDomainDTOs/WorldRuleDTO.java)

| Field | Java type | Declared validation |
|---|---|---|
| `positionX` | `Double` | `@NotNull(message = "Position X cannot be null")` |
| `positionY` | `Double` | `@NotNull(message = "Position Y cannot be null")` |
| `name` | `String` | `@NotEmpty(message = "Name cannot be empty")` |
| `description` | `String` | `@NotEmpty(message = "Description cannot be empty")` |
| `scope` | `String` | See service rules |
| `conditions` | `String` | See service rules |
| `exceptions` | `String` | See service rules |
| `consequences` | `String` | See service rules |
| `notes` | `String` | See service rules |

### LocationDTO

[LocationDTO.java](../../src/main/java/com/example/sana/DTO/CreativityDomainDTOs/LocationDTO.java)

| Field | Java type | Declared validation |
|---|---|---|
| `positionX` | `Double` | `@NotNull(message = "Position X cannot be null")` |
| `positionY` | `Double` | `@NotNull(message = "Position Y cannot be null")` |
| `name` | `String` | `@NotEmpty(message = "Name cannot be empty")` |
| `locationType` | `String` | See service rules |
| `narrativeSignificance` | `String` | See service rules |
| `notes` | `String` | See service rules |

### StoryEventDTO

[StoryEventDTO.java](../../src/main/java/com/example/sana/DTO/CreativityDomainDTOs/StoryEventDTO.java)

| Field | Java type | Declared validation |
|---|---|---|
| `positionX` | `Double` | `@NotNull(message = "Position X cannot be null")` |
| `positionY` | `Double` | `@NotNull(message = "Position Y cannot be null")` |
| `title` | `String` | `@NotEmpty(message = "Title cannot be empty")` |
| `description` | `String` | `@NotEmpty(message = "Description cannot be empty")` |
| `eventType` | `String` | See service rules |
| `presentationOrder` | `Integer` | `@NotNull(message = "Presentation order cannot be null")` |
| `chronologicalOrder` | `Integer` | `@Positive(message = "Chronological order must be positive")` |
| `inWorldTime` | `String` | See service rules |
| `notes` | `String` | See service rules |

### CreativityRelationCreateDTO

[CreativityRelationCreateDTO.java](../../src/main/java/com/example/sana/DTO/CreativityDomainDTOs/CreativityRelationCreateDTO.java)

| Field | Java type | Declared validation |
|---|---|---|
| `type` | `CreativityRelationType` | `@NotNull(message = "Relationship type cannot be null")` |
| `label` | `String` | `@Size(max = 50, message = "Label cannot exceed 50 characters")` |

### BranchingNodeDTO

[BranchingNodeDTO.java](../../src/main/java/com/example/sana/DTO/CreativityDomainDTOs/BranchingNodeDTO.java)

| Field | Java type | Declared validation |
|---|---|---|
| `title` | `String` | `@NotBlank(message = "Title cannot be empty")` |
| `content` | `String` | `@NotBlank(message = "Content cannot be empty")` |
| `type` | `BranchingNodeType` | `@NotNull(message = "Node type cannot be null")` |
| `positionX` | `Double` | `@NotNull(message = "Position X cannot be null")` |
| `positionY` | `Double` | `@NotNull(message = "Position Y cannot be null")` |

### BranchingChoiceDTO

[BranchingChoiceDTO.java](../../src/main/java/com/example/sana/DTO/CreativityDomainDTOs/BranchingChoiceDTO.java)

| Field | Java type | Declared validation |
|---|---|---|
| `text` | `String` | `@NotBlank(message = "Choice text cannot be empty")` |
| `displayOrder` | `Integer` | `@NotNull(message = "Display order cannot be null")`; `@PositiveOrZero` |

### BranchingPositionDTO

[BranchingPositionDTO.java](../../src/main/java/com/example/sana/DTO/CreativityDomainDTOs/BranchingPositionDTO.java)

| Field | Java type | Declared validation |
|---|---|---|
| `positionX` | `Double` | `@NotNull(message = "Position X cannot be null")` |
| `positionY` | `Double` | `@NotNull(message = "Position Y cannot be null")` |

### BranchingVariableDTO

[BranchingVariableDTO.java](../../src/main/java/com/example/sana/DTO/CreativityDomainDTOs/BranchingVariableDTO.java)

| Field | Java type | Declared validation |
|---|---|---|
| `name` | `String` | `@Pattern( regexp = "[A-Za-z][A-Za-z0-9_]{0,39}", message = "Variable name must start with a letter" )`; `@NotNull` |
| `initialValue` | `Integer` | `@NotNull` |

### BranchingPlayStateDTO

[BranchingPlayStateDTO.java](../../src/main/java/com/example/sana/DTO/CreativityDomainDTOs/BranchingPlayStateDTO.java)

| Field | Java type | Declared validation |
|---|---|---|
| `variables` | `Map<String, Integer>` | `@NotNull` |

### BranchingConditionDTO

[BranchingConditionDTO.java](../../src/main/java/com/example/sana/DTO/CreativityDomainDTOs/BranchingConditionDTO.java)

| Field | Java type | Declared validation |
|---|---|---|
| `operator` | `BranchingComparisonOperator` | `@NotNull` |
| `value` | `Integer` | `@NotNull` |

### BranchingEffectDTO

[BranchingEffectDTO.java](../../src/main/java/com/example/sana/DTO/CreativityDomainDTOs/BranchingEffectDTO.java)

| Field | Java type | Declared validation |
|---|---|---|
| `operator` | `BranchingEffectOperator` | `@NotNull` |
| `value` | `Integer` | `@NotNull` |

### PlayableCharacterDTO

[PlayableCharacterDTO.java](../../src/main/java/com/example/sana/DTO/CreativityDomainDTOs/PlayableCharacterDTO.java)

| Field | Java type | Declared validation |
|---|---|---|
| `name` | `String` | `@NotBlank(message = "Character name cannot be empty")`; `@Size(max = 100)` |
| `role` | `String` | `@Size(max = 100)` |
| `weaponName` | `String` | `@Size(max = 100)` |
| `weaponType` | `String` | `@Size(max = 100)` |
| `strengths` | `String` | See service rules |
| `weaknesses` | `String` | See service rules |
| `attack` | `Integer` | `@Min(0)`; `@Max(10)` |
| `defense` | `Integer` | `@Min(0)`; `@Max(10)` |
| `speed` | `Integer` | `@Min(0)`; `@Max(10)` |
| `rangeStat` | `Integer` | `@Min(0)`; `@Max(10)` |
| `stamina` | `Integer` | `@Min(0)`; `@Max(10)` |
| `special` | `Integer` | `@Min(0)`; `@Max(10)` |

### Other request contracts

- [BusinessToolRequestDTO](../../src/main/java/com/example/sana/DTO/Business/BusinessToolRequestDTO.java): projectId/outputId are required positive integers; toolType required; inputJson is an optional JSON-encoded string, max 20000; baseRevision optional and non-negative. Business workflow is disabled by default.
- [Project](../../src/main/java/com/example/sana/Model/Project.java), [ProjectOutput](../../src/main/java/com/example/sana/Model/ProjectOutput.java), [ProjectUnderstanding](../../src/main/java/com/example/sana/Model/ProjectUnderstanding.java), and [ProjectRecord](../../src/main/java/com/example/sana/Model/ProjectRecord.java) currently serve as generic request bodies. Send only editable scalar fields described in the handoff; omit relationships, IDs, and server-managed fields.
- [ProjectNode](../../src/main/java/com/example/sana/Model/ProjectNode.java) and [ProjectEdge](../../src/main/java/com/example/sana/Model/ProjectEdge.java) are generic graph storage shapes. dataJson is a string. Write-guarded diagram types use the diagram DTOs above and cannot be mutated through these unversioned generic APIs.
- When creating a ProjectOutput through the generic API with a diagram outputType, the project's domain must match that type and representationType must be GRAPH.

## Response contracts to map explicitly

- [GraphDefinitionDTO](../../src/main/java/com/example/sana/DTO/GraphDefinitionDTO.java): `{nodes:[{key,id,type,properties,positionX,positionY}], edges:[{key,id,type,label,sourceKey,targetKey,properties}], properties:{title,...}}`
- [DiagramGraphResponseDTO](../../src/main/java/com/example/sana/DTO/DiagramDTOs/DiagramGraphResponseDTO.java): `{outputId, graphRevision, graph}`
- [DiagramTypeDTO](../../src/main/java/com/example/sana/DTO/DiagramDTOs/DiagramTypeDTO.java): `{domain, diagramType, nodeTypes, edgeTypes, rulesSummary}`
- [AIRunResponseDTO](../../src/main/java/com/example/sana/DTO/AIDTOs/AIRunResponseDTO.java)
- [AIProposalResponseDTO](../../src/main/java/com/example/sana/DTO/AIDTOs/AIProposalResponseDTO.java)
- [AIProposalAcceptanceDTO](../../src/main/java/com/example/sana/DTO/AIDTOs/AIProposalAcceptanceDTO.java)
- [CreativitySearchResultDTO](../../src/main/java/com/example/sana/DTO/CreativityDomainDTOs/CreativitySearchResultDTO.java)
- [CreativityRelationDTO](../../src/main/java/com/example/sana/DTO/CreativityDomainDTOs/CreativityRelationDTO.java)
- [CreativityDeletePreviewDTO](../../src/main/java/com/example/sana/DTO/CreativityDomainDTOs/CreativityDeletePreviewDTO.java)
- [StoryEventTimelineDTO](../../src/main/java/com/example/sana/DTO/CreativityDomainDTOs/StoryEventTimelineDTO.java)
- [BranchingMapDTO](../../src/main/java/com/example/sana/DTO/CreativityDomainDTOs/BranchingMapDTO.java)
- [BranchingPlayDTO](../../src/main/java/com/example/sana/DTO/CreativityDomainDTOs/BranchingPlayDTO.java)
- [BranchingGamePreviewDTO](../../src/main/java/com/example/sana/DTO/CreativityDomainDTOs/BranchingGamePreviewDTO.java)
- [BranchingReferenceDTO](../../src/main/java/com/example/sana/DTO/CreativityDomainDTOs/BranchingReferenceDTO.java)
- [BranchingChoiceSettingsDTO](../../src/main/java/com/example/sana/DTO/CreativityDomainDTOs/BranchingChoiceSettingsDTO.java)
- [PlayableCharacterResponseDTO](../../src/main/java/com/example/sana/DTO/CreativityDomainDTOs/PlayableCharacterResponseDTO.java)
- [CharacterBalanceReportDTO](../../src/main/java/com/example/sana/DTO/CreativityDomainDTOs/CharacterBalanceReportDTO.java)
- [OpenLibraryResponseDTO](../../src/main/java/com/example/sana/DTO/CreativityDomainDTOs/OpenLibraryResponseDTO.java)
- [N8nCallbackDTO](../../src/main/java/com/example/sana/DTO/IntegrationDTOs/N8nCallbackDTO.java)

N8nCallbackDTO.Result/Clarification/Proposal/Failure are nested inside the public run-status DTO. The callback envelope itself is machine-only.

## Diagram types

Every diagram is a ProjectOutput with representationType GRAPH and one of these outputType values. The project's domain must match. `GET /api/v1/diagram/types` returns this list from the running backend, with each type's rules text; prefer it over hard-coding.

| Domain | Diagram type | Node types | Edge types | Manual editing |
|---|---|---|---|---|
| TECH | `ARCHITECTURE` | CLIENT, APPLICATION, SERVICE, DATABASE, QUEUE, COMPONENT, EXTERNAL_SYSTEM | DEPENDENCY, CONNECTS_TO, DATA_FLOW | `/api/v1/diagram` or `/api/v1/tech/architecture` |
| TECH | `ERD` | ENTITY | ONE_TO_ONE, ONE_TO_MANY, MANY_TO_MANY | `/api/v1/diagram` |
| TECH | `CLASS_DIAGRAM` | CLASS, ABSTRACT_CLASS, INTERFACE, ENUM | INHERITANCE, IMPLEMENTATION, ASSOCIATION, AGGREGATION, COMPOSITION, DEPENDENCY | `/api/v1/diagram` |
| TECH | `USE_CASE` | ACTOR, USE_CASE | ASSOCIATION, INCLUDE, EXTEND, GENERALIZATION | `/api/v1/diagram` |
| BUSINESS | `BUSINESS_MODEL_CANVAS` | the 9 canvas blocks of BusinessNodeType | SERVES, DELIVERS, REACHES, BUILDS_RELATIONSHIP_WITH, GENERATES_REVENUE, REQUIRES, PERFORMED_BY, PARTNERS_WITH, INCURS_COST | `/api/v1/diagram` |
| BUSINESS | `TARGET_AUDIENCE` | TARGET_AUDIENCE, CUSTOMER_PERSONA | BELONGS_TO_AUDIENCE, HAS_NEED, HAS_PREFERENCE | `/api/v1/diagram` |
| BUSINESS | `SWOT` | SWOT_STRENGTH, SWOT_WEAKNESS, SWOT_OPPORTUNITY, SWOT_THREAT | SUPPORTS, LIMITS, ENABLES, THREATENS | `/api/v1/diagram` |
| BUSINESS | `COMPETITOR_ANALYSIS` | COMPETITOR, COMPETITOR_PRODUCT, COMPETITOR_FEATURE | COMPETES_WITH, OFFERS, DIFFERENTIATES_FROM | `/api/v1/diagram` |
| BUSINESS | `RISK_ANALYSIS` | RISK, RISK_CAUSE, RISK_MITIGATION | CAUSED_BY, AFFECTS, MITIGATES | `/api/v1/diagram` |
| EDUCATION | `MIND_MAP` | CENTRAL_TOPIC, TOPIC, SUBTOPIC, NOTE | BRANCH, RELATED | `/api/v1/diagram` |
| CREATIVITY | `STORY_MAP` | CHARACTER, WORLD, LOCATION, WORLD_RULE, STORY_EVENT | all CreativityRelationType values | Creativity controllers |
| CREATIVITY | `BRANCHING_STORY` | DIALOGUE, DECISION, EVENT, ENDING | BRANCH_CHOICE | BranchingNarrativeController |

Node and edge property rules for each type are in [the n8n integration guide](../N8n-Backend-Integration.md#diagram-types-and-rules).

## Enum and vocabulary reference

Send uppercase enum names, never translated labels. Domain is a String on Project: use EDUCATION, BUSINESS, TECH, CREATIVITY exactly; AI requests reject any other value.

| Enum | Values | Source |
|---|---|---|
| AIDispatchStatus | `QUEUED`, `IN_FLIGHT`, `ACKNOWLEDGED`, `UNCERTAIN`, `TERMINAL` | [AIDispatchStatus.java](../../src/main/java/com/example/sana/Enums/AIDispatchStatus.java) |
| BusinessEdgeType | `SERVES`, `DELIVERS`, `REACHES`, `BUILDS_RELATIONSHIP_WITH`, `GENERATES_REVENUE`, `REQUIRES`, `PERFORMED_BY`, `PARTNERS_WITH`, `INCURS_COST`, `BELONGS_TO_AUDIENCE`, `HAS_NEED`, `HAS_PREFERENCE`, `SUPPORTS`, `LIMITS`, `ENABLES`, `THREATENS`, `COMPETES_WITH`, `OFFERS`, `DIFFERENTIATES_FROM`, `CAUSED_BY`, `AFFECTS`, `MITIGATES` | [BusinessEdgeType.java](../../src/main/java/com/example/sana/Enums/BusinessEdgeType.java) |
| BusinessNodeType | `CUSTOMER_SEGMENT`, `VALUE_PROPOSITION`, `CHANNELS`, `CUSTOMER_RELATIONSHIPS`, `REVENUE_STREAMS`, `KEY_RESOURCES`, `KEY_ACTIVITIES`, `KEY_PARTNERSHIPS`, `COST_STRUCTURE`, `TARGET_AUDIENCE`, `CUSTOMER_PERSONA`, `SWOT_STRENGTH`, `SWOT_WEAKNESS`, `SWOT_OPPORTUNITY`, `SWOT_THREAT`, `COMPETITOR`, `COMPETITOR_PRODUCT`, `COMPETITOR_FEATURE`, `RISK`, `RISK_CAUSE`, `RISK_MITIGATION` | [BusinessNodeType.java](../../src/main/java/com/example/sana/Enums/BusinessNodeType.java) |
| BusinessToolType | `BUSINESS_MODEL_BUILDER`, `TARGET_AUDIENCE_BUILDER`, `SWOT_ANALYZER`, `COMPETITOR_ANALYSIS`, `RISK_ANALYZER` | [BusinessToolType.java](../../src/main/java/com/example/sana/Enums/BusinessToolType.java) |
| ChangeOperation | `CREATE`, `UPDATE`, `DELETE` | [ChangeOperation.java](../../src/main/java/com/example/sana/Enums/ChangeOperation.java) |
| ChangeTargetType | `NODE`, `EDGE`, `OUTPUT`, `RECORD` | [ChangeTargetType.java](../../src/main/java/com/example/sana/Enums/ChangeTargetType.java) |
| BranchingComparisonOperator | `EQ`, `NE`, `GT`, `GTE`, `LT`, `LTE` | [BranchingComparisonOperator.java](../../src/main/java/com/example/sana/Enums/CreativityDomainEnums/BranchingComparisonOperator.java) |
| BranchingEffectOperator | `SET`, `ADD` | [BranchingEffectOperator.java](../../src/main/java/com/example/sana/Enums/CreativityDomainEnums/BranchingEffectOperator.java) |
| BranchingNodeType | `DIALOGUE`, `DECISION`, `EVENT`, `ENDING` | [BranchingNodeType.java](../../src/main/java/com/example/sana/Enums/CreativityDomainEnums/BranchingNodeType.java) |
| CreativityRelationType | `FAMILY`, `FRIENDSHIP`, `RIVALRY`, `WORK`, `ROMANTIC`, `PARTICIPATES_IN`, `CAUSES`, `AFFECTED_BY`, `LIVES_IN`, `BELONGS_TO`, `VISITS`, `LEADS_TO`, `PRECEDES`, `SIMULTANEOUS_WITH`, `NARRATIVE_LINK`, `INSIDE`, `CONNECTED_TO`, `NEAR`, `APPLIES_TO`, `RESTRICTS`, `AFFECTS`, `CUSTOM` | [CreativityRelationType.java](../../src/main/java/com/example/sana/Enums/CreativityDomainEnums/CreativityRelationType.java) |
| CreativitySearchType | `ALL`, `CHARACTER`, `WORLD`, `LOCATION`, `WORLD_RULE`, `STORY_EVENT` | [CreativitySearchType.java](../../src/main/java/com/example/sana/Enums/CreativityDomainEnums/CreativitySearchType.java) |
| ExecutionStatus | `PENDING`, `RUNNING`, `COMPLETED`, `TIMED_OUT`, `FAILED` | [ExecutionStatus.java](../../src/main/java/com/example/sana/Enums/ExecutionStatus.java) |
| ProjectStatus | `DRAFT`, `ACTIVE`, `ARCHIVED` | [ProjectStatus.java](../../src/main/java/com/example/sana/Enums/ProjectStatus.java) |
| ProposalStatus | `PENDING`, `APPROVED`, `REJECTED` | [ProposalStatus.java](../../src/main/java/com/example/sana/Enums/ProposalStatus.java) |
| RecordStatus | `DRAFT`, `CONFIRMED`, `ARCHIVED` | [RecordStatus.java](../../src/main/java/com/example/sana/Enums/RecordStatus.java) |
| RepresentationType | `GRAPH`, `IMAGE` | [RepresentationType.java](../../src/main/java/com/example/sana/Enums/RepresentationType.java) |
| AITask | `CREATE`, `EDIT`, `ADVISE` | [AITask.java](../../src/main/java/com/example/sana/Enums/AITask.java) |
| TechnicalOutputType | `ARCHITECTURE`, `ERD`, `USE_CASE`, `CLASS_DIAGRAM` | [TechnicalOutputType.java](../../src/main/java/com/example/sana/Enums/TechDomainEnums/TechnicalOutputType.java) |
| BusinessOutputType | `BUSINESS_MODEL_CANVAS`, `TARGET_AUDIENCE`, `SWOT`, `COMPETITOR_ANALYSIS`, `RISK_ANALYSIS` | [BusinessOutputType.java](../../src/main/java/com/example/sana/Enums/BusinessOutputType.java) |
| EducationOutputType | `MIND_MAP` | [EducationOutputType.java](../../src/main/java/com/example/sana/Enums/EducationDomainEnums/EducationOutputType.java) |
| CreativityOutputType | `STORY_MAP`, `BRANCHING_STORY` | [CreativityOutputType.java](../../src/main/java/com/example/sana/Enums/CreativityDomainEnums/CreativityOutputType.java) |

Node and edge type strings for each diagram are defined by its rules class (see [Diagram types](#diagram-types)), not by enum classes, except where the rules reuse BusinessNodeType, BusinessEdgeType, BranchingNodeType and CreativityRelationType.

ProposalStatus is the generic persistence enum (APPROVED); the AI review API maps it to ACCEPTED. ChangeTargetType.RECORD exists generically but is not a diagram proposal target to advertise.

## Scope and verification

This inventory contains 170 route declarations from 27 controller source files, counted from the sources on 25 October 2026. No endpoints were called and no build or test was run.
