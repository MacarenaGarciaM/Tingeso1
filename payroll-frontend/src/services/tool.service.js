import api from "./api"; // ya agrega Authorization: Bearer

export function getToolNamesWithCategory() {
  return api.get("/tool/names-categories").then(r => r.data);
}


export function createToolWithUser(tool, rutUser) {
  return api.post("/tool", tool, { params: { rutUser } }).then(r => r.data);
}


export function getAvailableTools() {
  return api.get("/tool/available").then(r => r.data);
}

export function adminUpdateTool({ id, rutUser, state, amount, repositionValue }) {
  return api.put(`/tool/${id}`, null, {
    params: { rutUser, state, amount, repositionValue }
  }).then(r => r.data);
}


export function getToolsByState(state) {
  return api.get("/tool/by-state", { params: { state } }).then(r => r.data);
}
