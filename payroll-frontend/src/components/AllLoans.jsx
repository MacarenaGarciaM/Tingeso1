import { useEffect, useMemo, useState } from "react";
import {
  Box, Paper, Typography, Chip, Stack, Table, TableHead, TableRow, TableCell,
  TableBody, TableFooter, IconButton, CircularProgress, Alert, Button
} from "@mui/material";
import ChevronLeft from "@mui/icons-material/ChevronLeft";
import ChevronRight from "@mui/icons-material/ChevronRight";
import { getMe } from "../services/auth.service";
import { getLoansByRut } from "../services/loan.service";

const CLP = new Intl.NumberFormat("es-CL", { style: "currency", currency: "CLP", maximumFractionDigits: 0 });
const today = () => new Date().toISOString().slice(0,10);

// Deriva estado de un préstamo
function computeStatus(l) {
  const active = l.lateReturnDate == null;
  const overdue = active && (l.returnDate < today());
  const withDebt = ((l.lateFine || 0) > 0 && !l.lateFinePaid) ||
                   ((l.damagePenalty || 0) > 0 && !l.damagePenaltyPaid);
  const finished = !active;
  return { active, overdue, withDebt, finished };
}

function StatusChips({ loan }) {
  const s = computeStatus(loan);
  return (
    <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap">
      {s.active   && <Chip label="Activo"    color="primary" size="small" />}
      {s.overdue  && <Chip label="Atrasado"  color="warning" size="small" />}
      {s.withDebt && <Chip label="Con deuda" color="error"   size="small" />}
      {s.finished && <Chip label="Finalizado" variant="outlined" size="small" />}
    </Stack>
  );
}

//<Chip label="Activo"  color="success" size="small"  sx={{ bgcolor: "green.100", color: "green.800" }} />

export default function AllLoans() {
  const [me, setMe] = useState(null);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [rows, setRows] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  // filtro de estado: all | active | finished | overdue | debt
  const [filter, setFilter] = useState("all");

  const load = async (p = page) => {
    try {
      setLoading(true);
      setErr("");
      const u = me || await getMe();
      if (!me) setMe(u);
      const data = await getLoansByRut({ rutUser: (me || u).rut, page: p, size, sort: "reservationDate,desc" });
      setRows(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
      setPage(data.number || 0);
    } catch (e) {
      setErr(e?.response?.data || e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(0); /* init */ }, [size]);

  const filtered = useMemo(() => {
    if (!rows?.length) return [];
    if (filter === "all") return rows;
    return rows.filter(l => {
      const s = computeStatus(l);
      if (filter === "active")   return s.active;
      if (filter === "finished") return s.finished;
      if (filter === "overdue")  return s.overdue;
      if (filter === "debt")     return s.withDebt;
      return true;
    });
  }, [rows, filter]);

  return (
    <Box sx={{ p:3, maxWidth: 1200, mx:"auto" }}>
      <Typography variant="h5" sx={{ mb:2 }}>Mis préstamos</Typography>
      {err && <Alert severity="error" sx={{ mb:2 }}>{String(err)}</Alert>}

      <Paper sx={{ p:2, mb:2 /*bgcolor: "#f5f5f5"*/}}>
        <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap" alignItems="center">
          <Typography variant="body2" sx={{ mr:1 }}>Filtrar:</Typography>
          <Chip label="Todos"      onClick={()=>setFilter("all")}      color={filter==="all"?"primary":"default"} />
          <Chip label="Activos"    onClick={()=>setFilter("active")}   color={filter==="active"?"primary":"default"} />
          <Chip label="Finalizados"onClick={()=>setFilter("finished")} color={filter==="finished"?"primary":"default"} />
          <Chip label="Atrasados"  onClick={()=>setFilter("overdue")}  color={filter==="overdue"?"primary":"default"} />
          <Chip label="Con deuda"  onClick={()=>setFilter("debt")}     color={filter==="debt"?"primary":"default"} />
          <Box sx={{ flex:1 }} />
          <Button size="small"  onClick={()=>load(page)} disabled={loading}>
            {loading ? <CircularProgress size={16} /> : "Actualizar"}
          </Button>
        </Stack>
      </Paper>

      <Paper sx={{ p:2 }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>#</TableCell>
              <TableCell>Reserva</TableCell>
              <TableCell>Devolución pactada</TableCell>
              <TableCell>Devolución real</TableCell>
              <TableCell align="right">Total arriendo</TableCell>
              <TableCell align="right">Multa atraso</TableCell>
              <TableCell align="right">Penalización daño</TableCell>
              <TableCell>Estado</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {filtered.map(l => (
              <TableRow key={l.id} hover>
                <TableCell>#{l.id}</TableCell>
                <TableCell>{l.reservationDate}</TableCell>
                <TableCell>{l.returnDate}</TableCell>
                <TableCell>{l.lateReturnDate || "-"}</TableCell>
                <TableCell align="right">{CLP.format(l.total || 0)}</TableCell>
                <TableCell align="right">
                  {CLP.format(l.lateFine || 0)} {l.lateFine > 0 ? (l.lateFinePaid ? " (pagada)" : " (pendiente)") : ""}
                </TableCell>
                <TableCell align="right">
                  {CLP.format(l.damagePenalty || 0)} {l.damagePenalty > 0 ? (l.damagePenaltyPaid ? " (pagada)" : " (pendiente)") : ""}
                </TableCell>
                <TableCell><StatusChips loan={l} /></TableCell>
              </TableRow>
            ))}
            {!filtered.length && (
              <TableRow><TableCell colSpan={8}>
                <Typography color="text.secondary">No hay préstamos con ese filtro.</Typography>
              </TableCell></TableRow>
            )}
          </TableBody>
          <TableFooter>
            <TableRow>
              <TableCell colSpan={8}>
                <Stack direction="row" alignItems="center" justifyContent="space-between">
                  <Typography variant="body2">
                    Página {page+1} de {Math.max(totalPages,1)} — {totalElements} préstamos
                  </Typography>
                  <Box>
                    <IconButton disabled={page<=0} onClick={()=>load(page-1)}><ChevronLeft/></IconButton>
                    <IconButton disabled={page>=totalPages-1} onClick={()=>load(page+1)}><ChevronRight/></IconButton>
                  </Box>
                </Stack>
              </TableCell>
            </TableRow>
          </TableFooter>
        </Table>
      </Paper>
    </Box>
  );
}
