import axios from "axios";
import keycloak from "./keycloak"; // tu instancia existente

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "http://localhost:8090",
});

// agrega el Bearer token automáticamente
api.interceptors.request.use((config) => {
  const token = keycloak?.token;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.request.use(cfg => {
  const t = keycloak?.token;
  if (t) cfg.headers.Authorization = `Bearer ${t}`;
  return cfg;
});

export default api;
