// http-common.js
import axios from "axios";
import keycloak from "./services/keycloak";

const api = axios.create({
  baseURL: "http://localhost:8090", // tu backend
});

// Interceptor para incluir el token en cada request
api.interceptors.request.use((config) => {
  if (keycloak && keycloak.token) {
    config.headers.Authorization = `Bearer ${keycloak.token}`;
  }
  return config;
});

export default api;
