import api from "./api";

export function getKardex(params = {}) {
  return api.get("/kardex", { params }).then(r => r.data); 
}
