import { useEffect, useState } from "react";
import {
  Box, Paper, Stack, TextField, Button, Typography, Table, TableHead, TableRow, TableCell,
  TableBody, TableFooter, IconButton, Checkbox, Snackbar, Alert
} from "@mui/material";
import ChevronLeftIcon from "@mui/icons-material/ChevronLeft";
import ChevronRightIcon from "@mui/icons-material/ChevronRight";
import { getLoansWithDebts, payFines } from "../services/loan.service";

const CLP = new Intl.NumberFormat("es-CL", { style: "currency", currency: "CLP", maximumFractionDigits: 0 });

export default function PagarMultas() {
  const [rows, setRows] = useState([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(12);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState("");

  // filtros
  const [rutUser, setRutUser] = useState("");
  const [start, setStart] = useState(""); // YYYY-MM-DD
  const [end, setEnd] = useState("");

  // selección de pagos por fila
  const [payloads, setPayloads] = useState({}); // { [loanId]: { payLateFine: bool, payDamagePenalty: bool } }

  //maneja el snackbar de toast
  const [toast, setToast] = useState({ open:false, msg:"", sev:"success" });

  // carga datos cuando cambia filtro, página o tamaño
  const load = async (p = page) => {
    try {
      setLoading(true);
      setErr("");
      const data = await getLoansWithDebts({ rutUser, start, end, page: p, size });
      setRows(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
      setPage(data.number || 0);
      // inicializa payloads
      const pl = {};
      (data.content || []).forEach(l => {
        pl[l.id] = {
          payLateFine: false,
          payDamagePenalty: false,
        };
      });
      setPayloads(pl);
    } catch (e) {
      setErr(e?.response?.data || e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(0); /* init */ }, [size]); // al cambiar tamaño, recarga

  //busca con los filtros actuales
  const onSearch = (e) => { e.preventDefault(); load(0); };

  const toggle = (loanId, key) => {
    setPayloads(prev => ({ ...prev, [loanId]: { ...prev[loanId], [key]: !prev[loanId]?.[key] } }));
  };

  const onPay = async (loan) => {
    const p = payloads[loan.id] || {};
    if (!p.payLateFine && !p.payDamagePenalty) {
      setToast({ open:true, msg:"Selecciona al menos una multa a pagar.", sev:"warning" });
      return;
    }
    try {
      await payFines(loan.id, {
        payLateFine: p.payLateFine && (loan.lateFine || 0) > 0 && !loan.lateFinePaid,
        payDamagePenalty: p.payDamagePenalty && (loan.damagePenalty || 0) > 0 && !loan.damagePenaltyPaid
      });
      setToast({ open:true, msg:"Pago registrado. Estado de usuario recalculado.", sev:"success" });
      // refresca la lista (este préstamo puede desaparecer si ya no queda deuda)
      await load(page);
    } catch (e) {
      setToast({ open:true, msg: e?.response?.data || e.message, sev:"error" });
    }
  };

  return (
    <Box sx={{ p:3, maxWidth: 1200, mx:"auto" }}>
      <Typography variant="h5" sx={{ mb:2 }}>Gestión de multas y penalizaciones</Typography>
      {err && <Alert severity="error" sx={{ mb:2 }}>{String(err)}</Alert>}

      <Paper sx={{ p:2, mb:2 }} component="form" onSubmit={onSearch}>
        <Stack direction={{ xs:"column", md:"row" }} spacing={2} useFlexGap flexWrap="wrap">
          <TextField label="RUT usuario" value={rutUser} onChange={e=>setRutUser(e.target.value)} />
          <TextField label="Desde" type="date" InputLabelProps={{shrink:true}} value={start} onChange={e=>setStart(e.target.value)} />
          <TextField label="Hasta" type="date" InputLabelProps={{shrink:true}} value={end} onChange={e=>setEnd(e.target.value)} />
          <TextField
            label="Tamaño página"
            type="number"
            inputProps={{ min:5, max:100 }}
            value={size}
            onChange={e=>setSize(Number(e.target.value)||12)}
            sx={{ width: 150 }}
          />
          <Button type="submit" variant="contained" disabled={loading}>{loading ? "Cargando..." : "Buscar"}</Button>
        </Stack>
      </Paper>

      <Paper sx={{ p:2 }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Loan #</TableCell>
              <TableCell>RUT</TableCell>
              <TableCell>Reserva</TableCell>
              <TableCell>Devolución pactada</TableCell>
              <TableCell align="right">Multa atraso</TableCell>
              <TableCell>Pagada</TableCell>
              <TableCell align="right">Penalización daño</TableCell>
              <TableCell>Pagada</TableCell>
              <TableCell>Marcar pago</TableCell>
              <TableCell>Acción</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map(l => {
              const p = payloads[l.id] || {};
              const lateFineDisabled = !l.lateFine || l.lateFine <= 0 || l.lateFinePaid;
              const damageDisabled = !l.damagePenalty || l.damagePenalty <= 0 || l.damagePenaltyPaid;
              return (
                <TableRow key={l.id}>
                  <TableCell>#{l.id}</TableCell>
                  <TableCell>{l.rutUser}</TableCell>
                  <TableCell>{l.reservationDate}</TableCell>
                  <TableCell>{l.returnDate}</TableCell>
                  <TableCell align="right">{CLP.format(l.lateFine || 0)}</TableCell>
                  <TableCell>{l.lateFinePaid ? "Sí" : "No"}</TableCell>
                  <TableCell align="right">{CLP.format(l.damagePenalty || 0)}</TableCell>
                  <TableCell>{l.damagePenaltyPaid ? "Sí" : "No"}</TableCell>
                  <TableCell>
                    <Stack direction="row" spacing={1}>
                      <Checkbox
                        checked={!!p.payLateFine}
                        onChange={() => toggle(l.id, "payLateFine")}
                        disabled={lateFineDisabled}
                      />
                      <Typography variant="body2">Atraso</Typography>
                      <Checkbox
                        checked={!!p.payDamagePenalty}
                        onChange={() => toggle(l.id, "payDamagePenalty")}
                        disabled={damageDisabled}
                      />
                      <Typography variant="body2">Daño</Typography>
                    </Stack>
                  </TableCell>
                  <TableCell>
                    <Button
                      variant="contained"
                      size="small"
                      onClick={() => onPay(l)}
                      disabled={(lateFineDisabled && damageDisabled)}
                    >
                      Pagar
                    </Button>
                  </TableCell>
                </TableRow>
              );
            })}
            {!rows.length && (
              <TableRow>
                <TableCell colSpan={10}>
                  <Typography color="text.secondary">No hay deudas pendientes con los filtros actuales.</Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
          <TableFooter>
            <TableRow>
              <TableCell colSpan={10}>
                <Box sx={{ display:"flex", alignItems:"center", justifyContent:"space-between" }}>
                  <Typography variant="body2">
                    Página {page+1} de {Math.max(totalPages,1)} — {totalElements} préstamos con deuda
                  </Typography>
                  <Box>
                    <IconButton disabled={page<=0} onClick={()=>load(page-1)}><ChevronLeftIcon/></IconButton>
                    <IconButton disabled={page>=totalPages-1} onClick={()=>load(page+1)}><ChevronRightIcon/></IconButton>
                  </Box>
                </Box>
              </TableCell>
            </TableRow>
          </TableFooter>
        </Table>
      </Paper>

      <Snackbar
        open={toast.open}
        autoHideDuration={3000}
        onClose={()=>setToast(s=>({ ...s, open:false }))}
        anchorOrigin={{ vertical:"bottom", horizontal:"right" }}
      >
        <Alert onClose={()=>setToast(s=>({ ...s, open:false }))} severity={toast.sev} variant="filled" sx={{ width:"100%" }}>
          {toast.msg}
        </Alert>
      </Snackbar>
    </Box>
  );
}
