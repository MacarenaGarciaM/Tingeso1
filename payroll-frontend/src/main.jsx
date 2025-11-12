// src/main.jsx
import React from "react";
import { createRoot } from "react-dom/client";
import App from "./App.jsx";
import "./index.css";

import { ReactKeycloakProvider } from "@react-keycloak/web";
import keycloak from "./services/keycloak";


const root = createRoot(document.getElementById("root"));

root.render(
  <ReactKeycloakProvider
    authClient={keycloak}
    initOptions={{
      onLoad: "check-sso",                      // <-- antes: "login-required"
      pkceMethod: "S256",
      silentCheckSsoRedirectUri: window.location.origin + "/silent-check-sso.html",
    }}
    autoRefreshToken
    LoadingComponent={<div>Cargando login…</div>}
  >
    <App />
  </ReactKeycloakProvider>
);

if (import.meta.env.DEV) window.kc = keycloak;


root.render(
  <ReactKeycloakProvider authClient={keycloak} /* ... */>
    <App />
  </ReactKeycloakProvider>
);
