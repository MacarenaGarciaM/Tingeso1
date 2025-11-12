import api from "./api";

export function getMe() {
  return api.get("/auth/me").then((r) => r.data);
}

