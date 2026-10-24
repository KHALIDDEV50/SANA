# Architecture AI requests

The current implementation supports durable n8n dispatch, callbacks, proposal preview, and acceptance or rejection. See [the n8n integration guide](N8n-Backend-Integration.md) for configuration, contracts, limits, and known gaps.

## Start a request

`POST /api/projects/{projectId}/ai/requests`

```json
{
  "task": "EDIT",
  "prompt": "Add an email notification service",
  "outputId": 20,
  "baseRevision": 3,
  "selectedElementIds": ["node:101"]
}
```

For CREATE, omit outputId and baseRevision and leave selectedElementIds empty or absent. For EDIT and ADVISE, supply an existing outputId; baseRevision is optional, but a supplied revision must match the saved diagram. Save or discard local edits first.

The backend generates requestId and captures canonical project/graph context. HTTP 202 means the request was persisted for processing, not that AI execution finished.

## Retrieve status and review a proposal

- `GET /api/projects/{projectId}/ai/requests/{requestId}`
- `GET /api/projects/{projectId}/ai/proposals/{proposalId}`
- `POST /api/projects/{projectId}/ai/proposals/{proposalId}/accept`
- `POST /api/projects/{projectId}/ai/proposals/{proposalId}/reject`

Processing states are PENDING, RUNNING, COMPLETED, FAILED, and TIMED_OUT. Proposal review states are separate: PENDING, ACCEPTED, and REJECTED. Approved graph data changes only on acceptance.

User authentication is currently removed for development. The callback still requires X-Sana-Callback-Key. Older Architecture-specific request/status aliases have been removed; use the project-scoped routes above.
