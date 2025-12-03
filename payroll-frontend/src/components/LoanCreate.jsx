import { useEffect, useState } from "react";
import {
  Box, Paper, Stack, TextField, Button, Typography, Alert, CircularProgress,
  Snackbar, Tooltip
} from "@mui/material";
import Autocomplete from "@mui/material/Autocomplete";
import { useNavigate } from "react-router-dom";
import { getMe } from "../services/auth.service";
import { getAvailableTools } from "../services/tool.service";
import { createLoan } from "../services/loan.service";

const todayISO = () => new Date().toISOString().slice(0, 10); //fecha actual en formato YYYY-MM-DD
const DAILY_RENT_PRICE = 2500;


//calcula la diferencia en días entre dos fechas (YYYY-MM-DD), mínimo 1 día
const diffDaysMin1 = (start, end) => {
  const s = new Date(start + "T00:00:00");
  const e = new Date(end + "T00:00:00");
  const ms = e - s;
  const days = Math.ceil(ms / (1000 * 60 * 60 * 24));
  return Math.max(1, days);
};

//CLP currency
const CLP = new Intl.NumberFormat("es-CL", { 
  style: "currency", 
  currency: "CLP", 
  maximumFractionDigits: 0 
});

export default function LoanCreate() {
  const navigate = useNavigate();
  const [me, setMe] = useState(null);
  const [options, setOptions] = useState([]);   // ToolEntity[] disponibles
  const [selected, setSelected] = useState([]); // ToolEntity[] seleccionadas
  const [reservationDate, setReservationDate] = useState(todayISO());
  const [returnDate, setReturnDate] = useState(todayISO());
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");
  const [toastOpen, setToastOpen] = useState(false); //controla Snackbar de confirmación está abierto.
  const [receipt, setReceipt] = useState(null);
  const [restricted, setRestricted] = useState(false);
  const [warnOpen, setWarnOpen] = useState(false);


  //Carga inicial de datos: usuario y herramientas disponibles
  useEffect(() => {
    const load = async () => {
      try {
        const [u, tools] = await Promise.all([getMe(), getAvailableTools()]);
        setMe(u);
        setOptions(tools || []);
        console.log("[LoanCreate] me:", u);
        console.log("[LoanCreate] available tools:", tools);
         if (u && u.active === false) {
         setRestricted(true);
         setWarnOpen(true);  // muestra snackbar de advertencia
       } else {
         setRestricted(false);
       }
      } catch (e) {
        const msg = e?.response?.data || e.message;
        console.error("[LoanCreate] load error:", e);
        setErr(msg);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);


  // Maneja el envío del formulario de creación de préstamo
  const onSubmit = async (e) => {
    e.preventDefault();
    setErr("");

    try {
      if (!me) throw new Error("No se pudo obtener el usuario (auth/me).");
      if (!me.rut) throw new Error("Tu RUT no está registrado. Complétalo antes de solicitar un préstamo.");
      if (!reservationDate || !returnDate) throw new Error("Debes ingresar fechas.");
      if (returnDate < reservationDate) throw new Error("La fecha de devolución no puede ser anterior a la de reserva.");
      if (!selected.length) throw new Error("Debes seleccionar al menos una herramienta.");

      const items = selected.map(t => ({ toolId: t.id, quantity: 1 }));

      // 1) backend
      await createLoan(me.rut, reservationDate, returnDate, items);

      // 2) Calculate local
      const days = diffDaysMin1(reservationDate, returnDate);
      const rentTotal = days * DAILY_RENT_PRICE;
      const reposTotal = selected.reduce((acc, t) => acc + (t.repositionValue || 0), 0);
      const itemsDetail = selected.map(t => ({
        name: t.name,
        category: t.category,
        repositionValue: t.repositionValue || 0,
      }));

      setReceipt({ days, rentTotal, reposTotal, items: itemsDetail });
      setToastOpen(true);

      // 3) (opcional) navegar después de 2.5s
      //setTimeout(() => navigate("/tools/names"), 2500);
    } catch (e2) {
      const msg = e2?.response?.data || e2.message;
      console.error("[LoanCreate] submit error:", e2);
      setErr(msg);
    }
  };

  const handleToastClose = () => {
    setToastOpen(false);
  };
  const handleOkay = () => {
  setToastOpen(false);
  navigate("/home");     // o "/" si tu Home vive en la raíz
};

  return (
    <Box sx={{ p: 3, maxWidth: 800, mx: "auto" }}>
      <Typography variant="h5" sx={{ mb: 2 }}>Crear préstamo</Typography>

      {loading && (
        <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 2 }}>
          <CircularProgress size={20} /> <span>Cargando…</span>
        </Box>
      )}

      {err && <Alert severity="error" sx={{ mb: 2 }}>{String(err)}</Alert>}
           {restricted && (
       <Alert severity="warning" sx={{ mb: 2 }}>
         Tu cuenta está temporalmente restringida para crear préstamos. 
         Revisa si tienes <b>préstamos vencidos</b> o <b>multas/penalizaciones impagas</b>.
       </Alert>
     )}

      <form onSubmit={onSubmit}>
        <Paper sx={{ p: 2, mb: 2 }}>
          <Stack spacing={2} direction={{ xs: "column", sm: "row" }}>
            <TextField
              label="Fecha de reserva"
              type="date"
              value={reservationDate}
              onChange={e => setReservationDate(e.target.value)}
              InputLabelProps={{ shrink: true }}
              fullWidth
              required
            />
            <TextField
              label="Fecha de devolución"
              type="date"
              value={returnDate}
              onChange={e => setReturnDate(e.target.value)}
              InputLabelProps={{ shrink: true }}
              fullWidth
              required
            />
          </Stack>
        </Paper>

        <Paper sx={{ p: 2 }}>
          <Stack spacing={2}>
            <Typography variant="subtitle1">Selecciona herramientas (Disponibles)</Typography>

            <Autocomplete
              multiple
              options={options}
              value={selected}
              onChange={(_, val) => setSelected(val)}
              getOptionLabel={(o) => (o?.name && o?.category) ? `${o.name} (${o.category})` : `${o?.name ?? ""}`}
              isOptionEqualToValue={(o, v) => o?.id === v?.id}
              renderInput={(params) => (
                <TextField {...params} label="Herramientas" placeholder="Buscar..." />
              )}
              noOptionsText={loading ? "Cargando…" : "Sin herramientas disponibles"}
            />

            <Stack direction="row" justifyContent="flex-end" spacing={2}>
              <Button variant="outlined" onClick={() => navigate(-1)}>Cancelar</Button>
              <Tooltip
               title={
                 restricted
                   ? "No puedes crear préstamos: cuenta restringida por atraso o deudas impagas."
                   : (!selected.length ? "Selecciona al menos una herramienta" : "")
               }
               disableHoverListener={!restricted && !!selected.length}
             >
               <span>
                 <Button
                   type="submit"
                   variant="contained"
                   disabled={restricted || !selected.length}
                 >
                   Crear préstamo
                 </Button>
               </span>
             </Tooltip>
            </Stack>
          </Stack>
        </Paper>
      </form>

            <Snackbar
        open={toastOpen}
        autoHideDuration={null}
        onClose={handleToastClose}
        anchorOrigin={{ vertical: "top", horizontal: "center" }}
        sx={{
          '& .MuiSnackbar-root': {
            minWidth: '500px',
          }
        }}
      >
        <Alert
          onClose={handleToastClose}
          severity="success"
          variant="filled"
          sx={{ 
            width: '100%',
            minWidth: '450px', // Ancho mínimo más grande
            minHeight: '120px', // Alto mínimo más grande
            fontSize: '16px', // Texto más grande
            '& .MuiAlert-message': {
              width: '100%',
              padding: '8px 0', // Más padding interno
              fontSize: 'inherit', // Hereda el tamaño de fuente del padre
            }
          }}
        >
          {receipt ? (
  <Box sx={{ width: '100%', '& > div': { mb: '4px', fontSize: '16px' }, '& ul': { fontSize: '14px' } }}>
    <div><strong>¡Préstamo creado exitosamente!</strong></div>
    <div>Días de arriendo: <strong>{receipt.days}</strong></div>
    <div>Valor arriendo: <strong>{CLP.format(receipt.rentTotal)}</strong></div>
    <div>Total reposición: <strong>{CLP.format(receipt.reposTotal)}</strong></div>

    {receipt.items?.length > 0 && (
      <Box sx={{ mt: 1 }}>
        <strong>Herramientas:</strong>
        <ul style={{ margin: "8px 0 0 20px", padding: 0 }}>
          {receipt.items.map((it, i) => (
            <li key={i} style={{ marginBottom: '4px' }}>
              {it.name} ({it.category}) — {CLP.format(it.repositionValue)}
            </li>
          ))}
        </ul>
      </Box>
    )}

    {/* Botón OK */}
    <Box sx={{ mt: 2, display: 'flex', justifyContent: 'flex-end' }}>
      <Button onClick={handleOkay} variant="contained" size="small">
        OK
      </Button>
    </Box>
  </Box>
) : (
  <Box sx={{ fontSize: '16px', padding: '8px 0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
    <span>¡Préstamo creado exitosamente!</span>
    <Button onClick={handleOkay} variant="contained" size="small">OK</Button>
  </Box>
)}
        </Alert>
      </Snackbar>
    </Box>
  );
}