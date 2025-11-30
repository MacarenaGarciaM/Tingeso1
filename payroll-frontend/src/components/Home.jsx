import { useEffect, useState } from "react";
import { useKeycloak } from "@react-keycloak/web";
import { getDailyRate, setDailyRate } from "../services/setting.service";
import { Box, Stack, TextField, Button, Snackbar, Alert, Typography } from "@mui/material";

const CLP = new Intl.NumberFormat("es-CL", { style: "currency", currency: "CLP", maximumFractionDigits: 0 });

export default function Home() {
  const { keycloak, initialized } = useKeycloak();
  const roles = keycloak?.tokenParsed?.realm_access?.roles || [];
  const isAdmin = roles.map(r => String(r).toUpperCase()).includes("ADMIN");

  const [rate, setRate]   = useState(0);
  const [edit, setEdit]   = useState("");
  const [toast, setToast] = useState({ open:false, msg:"", sev:"success" });

  useEffect(() => {
    // 1) Esperar a que Keycloak esté listo
    if (!initialized) return;

    // 2) Si NO está autenticado, no llamamos al backend
    if (!keycloak?.authenticated) {
      setRate(0);
      setEdit("");
      return;
    }

    // 3) Usuario autenticado,  pedir la tarifa
    (async () => {
      try {
        const v = await getDailyRate();
        setRate(v);
        setEdit(String(v));
      } catch (e) {
        setToast({
          open: true,
          msg: e?.response?.data || e.message,
          sev: "error",
        });
      }
    })();
  }, [initialized, keycloak?.authenticated]); //se recarga cuando cambia el estado de login

  const onSave = async () => {
    try {
      const v = parseInt(edit, 10);
      if (Number.isNaN(v) || v < 0) throw new Error("Tarifa inválida");
      const saved = await setDailyRate(v);
      setRate(saved);
      setToast({ open:true, msg:"Tarifa actualizada", sev:"success" });
    } catch (e) {
      setToast({ open:true, msg: e?.response?.data || e.message, sev:"error" });
    }
  };

  return (
    <Box sx={{ textAlign:"center", p:4 }}>
      <Typography variant="h4" sx={{ mb:1 }}>
        ToolRent: Sistema de arriendo de herramientas!
      </Typography>
      <Typography sx={{ mb:3 }}>Bienvenid@ a ToolRent.</Typography>
      <Typography>Inicia sesión y navega por el menú para explorar nuestras opciones.</Typography>

      <img src="/hammer.png" alt="Martillo" style={{ width: 200, marginTop: 20 }} />

      {keycloak?.authenticated && (
        <Stack spacing={1} sx={{ mt:4, alignItems:"center" }}>
          <Typography variant="h6">
            Nuestra tarifa diaria de arriendo es: {CLP.format(rate)}
          </Typography>

          {isAdmin && (
            <Stack direction={{ xs:"column", sm:"row" }} spacing={1} sx={{ mt:1 }}>
              <TextField
                size="small"
                type="number"
                label="Nueva tarifa diaria"
                inputProps={{ min:0 }}
                value={edit}
                onChange={e => setEdit(e.target.value)}
              />
              <Button variant="contained" onClick={onSave}>Guardar</Button>
            </Stack>
          )}
        </Stack>
      )}

      <Snackbar
        open={toast.open}
        autoHideDuration={3000}
        onClose={() => setToast({ ...toast, open:false })}
      >
        <Alert onClose={() => setToast({ ...toast, open:false })} severity={toast.sev}>
          {toast.msg}
        </Alert>
      </Snackbar>
    </Box>
  );
}
