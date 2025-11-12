import { useEffect, useState } from "react";
import { Box, TextField, Button, Typography, Paper, Stack, MenuItem, Switch, FormControlLabel, Alert } from "@mui/material";
import { useNavigate } from "react-router-dom";
import { getMe } from "../services/auth.service";
import { createToolWithUser } from "../services/tool.service";

const ESTADOS = ["Disponible", "Prestada", "En reparación", "Dada de baja"]; // debe coincidir con validState del backend

export default function ToolCreate() {
  const navigate = useNavigate();
  const [me, setMe] = useState(null);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  const [form, setForm] = useState({
    name: "",
    category: "",
    initialState: "",
    repositionValue: "",
    amount: "",
    available: true,
  });

  useEffect(() => {
    // Cargar el usuario autenticado (se usará como user en el POST)
    getMe()
      .then((u) => setMe(u))
      .catch((e) => setErr(e?.response?.data || e.message))
      .finally(() => setLoading(false));
  }, []);

  const onChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }));
  const onToggle = (e) => setForm((f) => ({ ...f, available: e.target.checked }));

  const onSubmit = async (e) => {
    e.preventDefault();
    setErr("");

    try {
      if (!me) throw new Error("No se pudo cargar el usuario.");
      if (!me.admin) throw new Error("Solo un administrador puede crear herramientas.");
      if (!me.rut) throw new Error("Tu RUT no está registrado. Complétalo antes de crear herramientas.");

      const tool = {
        name: form.name.trim(),
        category: form.category.trim(),
        initialState: form.initialState,
        repositionValue: Number(form.repositionValue),
        amount: Number(form.amount),
        available: Boolean(form.available),
      };

      await createToolWithUser(tool, me.rut); // <- envía { tool, user } al backend
      navigate("/tools/names");           // vuelve a la lista (ajusta si quieres)
    } catch (e2) {
      setErr(e2?.response?.data || e2.message);
    }
  };

  if (loading) return <Box sx={{ p: 3 }}>Cargando…</Box>;

  return (
    <Box sx={{ p: 3, maxWidth: 640, mx: "auto" }} component="form" onSubmit={onSubmit}>
      <Typography variant="h5" sx={{ mb: 2 }}>Crear herramienta</Typography>
      {!me?.admin && <Alert severity="warning" sx={{ mb: 2 }}>Esta página es solo para administradores.</Alert>}
      {err && <Alert severity="error" sx={{ mb: 2 }}>{String(err)}</Alert>}

      <Paper sx={{ p: 2 }}>
        <Stack spacing={2}>
          <TextField label="Nombre" name="name" value={form.name} onChange={onChange} required />
          <TextField label="Categoría" name="category" value={form.category} onChange={onChange} required />
          <TextField select label="Estado inicial" name="initialState" value={form.initialState} onChange={onChange} required>
            {ESTADOS.map((e) => <MenuItem key={e} value={e}>{e}</MenuItem>)}
          </TextField>
          <TextField label="Valor de reposición" name="repositionValue" type="number" inputProps={{ min: 1 }} value={form.repositionValue} onChange={onChange} required />
          <TextField label="Cantidad a ingresar" name="amount" type="number" inputProps={{ min: 1 }} value={form.amount} onChange={onChange} required />
          <FormControlLabel control={<Switch checked={form.available} onChange={onToggle} />} label="Disponible" />

          <Stack direction="row" spacing={2} justifyContent="flex-end">
            <Button variant="outlined" onClick={() => navigate(-1)}>Cancelar</Button>
            <Button type="submit" variant="contained" disabled={!me?.admin}>Guardar</Button>
          </Stack>
        </Stack>
      </Paper>
    </Box>
  );
}
