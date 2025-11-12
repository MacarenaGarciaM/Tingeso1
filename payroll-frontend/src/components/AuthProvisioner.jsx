import { useEffect, useRef } from "react";
import { useKeycloak } from "@react-keycloak/web";
import api from "../services/api";

export default function AuthProvisioner() {
  const { keycloak } = useKeycloak();
  const provisionedOnce = useRef(false);

  useEffect(() => {
    if (!keycloak?.authenticated) return;

    // Llamada inicial al entrar autenticado
    const provision = async () => {
      try {
        await api.get("/auth/me");
        provisionedOnce.current = true;
      } catch (e) {
        console.error("[auth/me] failed", e);
      }
    };
    provision();

    // Si el token se refresca, opcionalmente re-sincroniza (una vez por sesión)
    const onToken = () => {
      if (keycloak.authenticated && !provisionedOnce.current) {
        provision();
      }
    };

    keycloak.onAuthRefreshSuccess = onToken;
    keycloak.onAuthSuccess = onToken;

    return () => {
      // limpia handlers (evita duplicados en HMR)
      keycloak.onAuthRefreshSuccess = undefined;
      keycloak.onAuthSuccess = undefined;
    };
  }, [keycloak?.authenticated]);

  return null; // no renderiza UI
}
