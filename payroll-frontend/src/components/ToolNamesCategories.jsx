import { useEffect, useMemo, useState } from "react";
import { useKeycloak } from "@react-keycloak/web";
import { getAvailableTools, getToolsByState, adminUpdateTool } from "../services/tool.service";
import { getMe } from "../services/auth.service";
import {
  Box, CircularProgress, Typography, Paper, Table, TableHead, TableRow, TableCell,
  TableBody, IconButton, TextField, Tooltip, Stack, Button, Snackbar, Alert, Dialog,
  DialogTitle, DialogContent, DialogActions, Tabs, Tab
} from "@mui/material";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
import EditIcon from "@mui/icons-material/Edit";
import SaveIcon from "@mui/icons-material/Save";
import CloseIcon from "@mui/icons-material/Close";
import BuildIcon from "@mui/icons-material/Build"; // ícono para “reparada” → disponible

const CLP = new Intl.NumberFormat("es-CL", { style: "currency", currency: "CLP", maximumFractionDigits: 0 });

export default function ToolNamesCategories() {
  const { keycloak } = useKeycloak();
  const rolesRaw = keycloak?.tokenParsed?.realm_access?.roles || [];
  const isAdmin = rolesRaw.map(String).map(r => r.toUpperCase()).includes("ADMIN");


  const [me, setMe] = useState(null);

  // pestaña: 0 = Disponibles, 1 = En reparación
  const [tab, setTab] = useState(0);

  // buckets crudos
  const [available, setAvailable] = useState([]);   // ToolEntity[] (state=Disponible)
  const [repairing, setRepairing] = useState([]);   // ToolEntity[] (state=En reparación)

  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState(null);

  // edición de valor de reposición (en “Disponible”)
  const [editingId, setEditingId] = useState(null);
  const [newRepoValue, setNewRepoValue] = useState("");

  // confirmación de baja (desde Disponible)
  const [confirmId, setConfirmId] = useState(null);

  // toast
  const [toast, setToast] = useState({ open: false, msg: "", sev: "success" });

  const loadAll = async () => {
    setLoading(true);
    setErr(null);
    try {
      const [u, disp] = await Promise.all([
        getMe(),
        getAvailableTools()
      ]);
      setMe(u);

      setAvailable((disp || []).filter(t => (t.initialState || "").toLowerCase() === "disponible"));

      if (isAdmin) {
        const rep = await getToolsByState("En reparación");
        setRepairing((rep || []).filter(t => (t.initialState || "").toLowerCase() === "en reparación"));
      } else {
        setRepairing([]); // no intentes cargar, evita el 403
      }
    } catch (e) {
      setErr(e?.response?.data || e.message);
    } finally {
      setLoading(false);
    }
  };


  useEffect(() => { loadAll(); }, []);

  // Agrupa por name+category (para mostrar una fila consolidada y usar el id de un bucket representativo)
  const group = (rows) => {
    const map = new Map();
    for (const t of rows) {
      const key = `${t.name}||${t.category}`;
      const item = map.get(key) || { name: t.name, category: t.category, id: t.id, amount: 0, repositionValue: t.repositionValue };
      item.amount += t.amount ?? 0;
      item.repositionValue = t.repositionValue;
      item.id = t.id; // id de uno de los buckets en ese estado (para operar)
      map.set(key, item);
    }
    return [...map.values()];
  };

  const availableGrouped = useMemo(() => group(available), [available]);
  const repairingGrouped = useMemo(() => group(repairing), [repairing]);

  // ----- Edición reposición (Disponible) -----
  const startEdit = (row) => {
    setEditingId(row.id);
    setNewRepoValue(String(row.repositionValue ?? ""));
  };
  const cancelEdit = () => {
    setEditingId(null);
    setNewRepoValue("");
  };
  const saveEdit = async (row) => {
    try {
      const val = parseInt(newRepoValue, 10);
      if (Number.isNaN(val) || val < 0) throw new Error("Valor de reposición inválido.");
      await adminUpdateTool({ id: row.id, repositionValue: val, rutUser: me?.rut });
      setAvailable(prev => prev.map(t => (t.id === row.id ? { ...t, repositionValue: val } : t)));
      setToast({ open: true, msg: "Valor de reposición actualizado.", sev: "success" });
      cancelEdit();
    } catch (e) {
      setToast({ open: true, msg: e?.response?.data || e.message, sev: "error" });
    }
  };

  // ----- Baja 1 unidad (Disponible -> Dada de baja) -----
  const decommissionOne = async (row) => {
    try {
      await adminUpdateTool({ id: row.id, state: "Dada de baja", rutUser: me?.rut });
      // resta stock local en “Disponible”
      setAvailable(prev => prev.map(t => (t.id === row.id ? { ...t, amount: Math.max(0, (t.amount ?? 0) - 1) } : t)));
      setToast({ open: true, msg: "Herramienta dada de baja (1 unidad).", sev: "success" });
    } catch (e) {
      setToast({ open: true, msg: e?.response?.data || e.message, sev: "error" });
    } finally {
      setConfirmId(null);
    }
  };

  // ----- Reparada: mover 1 unidad de “En reparación” -> “Disponible” -----
  const markRepairedOne = async (row) => {
    try {
      await adminUpdateTool({ id: row.id, state: "Disponible", rutUser: me?.rut });
      // actualiza ambas listas localmente
      setRepairing(prev => prev.map(t => (t.id === row.id ? { ...t, amount: Math.max(0, (t.amount ?? 0) - 1) } : t)));
      // como no sabemos qué bucket disponible exacto creció, lo más confiable es recargar
      await loadAll();
      setToast({ open: true, msg: "Unidad movida a 'Disponible'.", sev: "success" });
    } catch (e) {
      setToast({ open: true, msg: e?.response?.data || e.message, sev: "error" });
    }
  };

  if (loading) {
    return (
      <Box sx={{ p: 3, display: "flex", alignItems: "center", gap: 2 }}>
        <CircularProgress size={24} /> <Typography>Cargando herramientas…</Typography>
      </Box>
    );
  }
  if (err) {
    return <Box sx={{ p: 3 }}><Typography color="error">Error: {String(err)}</Typography></Box>;
  }

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h5" sx={{ mb: 1 }}>Inventario por estado</Typography>
      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 2 }}>
        <Tab label="Disponibles" />
        {isAdmin && <Tab label="En reparación" />}
      </Tabs>

      {/* TAB: Disponibles */}
      {tab === 0 && (
        <Paper>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell><b>Nombre</b></TableCell>
                <TableCell><b>Categoría</b></TableCell>
                <TableCell align="right"><b>Stock</b></TableCell>
                <TableCell align="right"><b>Valor reposición</b></TableCell>
                {isAdmin && <TableCell align="right"><b>Acciones</b></TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {availableGrouped.map((r, i) => (
                <TableRow key={i} hover>
                  <TableCell>{r.name}</TableCell>
                  <TableCell>{r.category}</TableCell>
                  <TableCell align="right">{r.amount ?? 0}</TableCell>

                  <TableCell align="right">
                    {isAdmin && editingId === r.id ? (
                      <Stack direction="row" spacing={1} justifyContent="flex-end">
                        <TextField
                          size="small" type="number" inputProps={{ min: 0 }}
                          value={newRepoValue} onChange={e => setNewRepoValue(e.target.value)}
                          sx={{ width: 140 }}
                        />
                        <Tooltip title="Guardar"><IconButton onClick={() => saveEdit(r)}><SaveIcon /></IconButton></Tooltip>
                        <Tooltip title="Cancelar"><IconButton onClick={cancelEdit}><CloseIcon /></IconButton></Tooltip>
                      </Stack>
                    ) : (
                      <Stack direction="row" spacing={1} justifyContent="flex-end" alignItems="center">
                        <span>{CLP.format(r.repositionValue ?? 0)}</span>
                        {isAdmin && (
                          <Tooltip title="Editar valor">
                            <IconButton size="small" onClick={() => startEdit(r)}><EditIcon fontSize="small" /></IconButton>
                          </Tooltip>
                        )}
                      </Stack>
                    )}
                  </TableCell>

                  {isAdmin && (
                    <TableCell align="right">
                      <Tooltip title="Dar de baja 1 unidad">
                        <span>
                          <IconButton
                            color="error"
                            disabled={(r.amount ?? 0) <= 0}
                            onClick={() => setConfirmId(r.id)}
                          >
                            <DeleteOutlineIcon />
                          </IconButton>
                        </span>
                      </Tooltip>
                    </TableCell>
                  )}
                </TableRow>
              ))}
              {availableGrouped.length === 0 && (
                <TableRow><TableCell colSpan={isAdmin ? 5 : 4}>Sin datos.</TableCell></TableRow>
              )}
            </TableBody>
          </Table>
        </Paper>
      )}

      {/* TAB: En reparación */}
      {isAdmin && tab === 1 && (
        <Paper>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell><b>Nombre</b></TableCell>
                <TableCell><b>Categoría</b></TableCell>
                <TableCell align="right"><b>En reparación</b></TableCell>
                {isAdmin && <TableCell align="right"><b>Acciones</b></TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {repairingGrouped.map((r, i) => (
                <TableRow key={i} hover>
                  <TableCell>{r.name}</TableCell>
                  <TableCell>{r.category}</TableCell>
                  <TableCell align="right">{r.amount ?? 0}</TableCell>
                  {isAdmin && (
                    <TableCell align="right">
                      <Tooltip title="Marcar reparada (mover 1 a Disponible)">
                        <span>
                          <IconButton
                            color="primary"
                            disabled={(r.amount ?? 0) <= 0}
                            onClick={() => markRepairedOne(r)}
                          >
                            <BuildIcon />
                          </IconButton>
                        </span>
                      </Tooltip>
                    </TableCell>
                  )}
                </TableRow>
              ))}
              {repairingGrouped.length === 0 && (
                <TableRow><TableCell colSpan={isAdmin ? 4 : 3}>Sin datos.</TableCell></TableRow>
              )}
            </TableBody>
          </Table>
        </Paper>
      )}

      {/* Confirmación de baja (tab Disponibles) */}
      <Dialog open={!!confirmId} onClose={() => setConfirmId(null)}>
        <DialogTitle>Dar de baja</DialogTitle>
        <DialogContent>¿Deseas dar de baja 1 unidad de esta herramienta?</DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmId(null)}>Cancelar</Button>
          <Button color="error" variant="contained"
                  onClick={() => decommissionOne(availableGrouped.find(g => g.id === confirmId))}>
            Confirmar
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={toast.open}
        autoHideDuration={3000}
        onClose={() => setToast({ ...toast, open: false })}
        anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
      >
        <Alert onClose={() => setToast({ ...toast, open: false })} severity={toast.sev}>
          {toast.msg}
        </Alert>
      </Snackbar>
    </Box>
  );
}
