import { useEffect, useMemo, useState } from "react";
import { useKeycloak } from "@react-keycloak/web";
import {
  Box, Paper, Typography, TextField, MenuItem, Table, TableBody, TableCell, TableHead, TableRow,
  TablePagination, CircularProgress, Alert, Stack
} from "@mui/material";
import { getOverdueLoans } from "../services/loan.service";
import { getMe } from "../services/auth.service";

const CLP = new Intl.NumberFormat("es-CL", { style: "currency", currency: "CLP", maximumFractionDigits: 0 });

export default function OverdueLoans() {
  const { keycloak } = useKeycloak();
  const roles = keycloak?.tokenParsed?.realm_access?.roles || [];
  const isAdmin = roles.map(r => String(r).toUpperCase()).includes("ADMIN");

  const [me, setMe] = useState(null);
  const [loading, setLoading] = useState(true); // indica si está cargando datos del backend
  const [err, setErr] = useState(""); //guarda errores para mostrar en un Alert
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(12);
  const [data, setData] = useState({ content: [], totalElements: 0 });

  // filtro por rut (admin puede ver todos; user solo el suyo)
  const [rutFilter, setRutFilter] = useState("");

  // carga datos cuando cambia filtro, página o tamaño
  useEffect(() => {
    (async () => {
      try {
        setLoading(true);
        const u = await getMe();
        setMe(u);
        const rut = isAdmin ? (rutFilter || undefined) : u.rut;
        const res = await getOverdueLoans({ rutUser: rut, page, size });
        setData(res || { content: [], totalElements: 0 });
        setErr("");
      } catch (e) {
        setErr(e?.response?.data || e.message);
      } finally {
        setLoading(false);
      }
    })();
  }, [isAdmin, rutFilter, page, size]);

  return (
    <Box sx={{ p:3 }}>
      <Typography variant="h5" sx={{ mb:2 }}>Préstamos con atraso</Typography>
      {err && <Alert severity="error" sx={{ mb:2 }}>{String(err)}</Alert>}

      <Paper sx={{ p:2 }}>
        <Stack direction={{ xs:"column", sm:"row" }} spacing={2} sx={{ mb:2 }} alignItems="center">
          {isAdmin ? (
            <TextField
              label="Filtrar por RUT (opcional)"
              value={rutFilter}
              onChange={(e) => { setPage(0); setRutFilter(e.target.value.trim()); }}
              size="small"
            />
          ) : (
            <Typography variant="body2">Mostrando atrasos de: <b>{me?.rut}</b></Typography>
          )}
        </Stack>

        {loading ? (
          <Box sx={{ p:2, display:"flex", alignItems:"center", gap:1 }}>
            <CircularProgress size={20} /> Cargando…
          </Box>
        ) : (
          <>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell><b>ID</b></TableCell>
                  <TableCell><b>RUT</b></TableCell>
                  <TableCell><b>Reserva</b></TableCell>
                  <TableCell><b>Devolución pactada</b></TableCell>
                  <TableCell><b>Total</b></TableCell>
                  <TableCell><b>Ítems</b></TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {data.content?.map(l => (
                  <TableRow key={l.id} hover>
                    <TableCell>#{l.id}</TableCell>
                    <TableCell>{l.rutUser}</TableCell>
                    <TableCell>{l.reservationDate}</TableCell>
                    <TableCell>{l.returnDate}</TableCell>
                    <TableCell>{CLP.format(l.total || 0)}</TableCell>
                    <TableCell>{l.items?.map(i => i.tool?.name).join(", ")}</TableCell>
                  </TableRow>
                ))}
                {!data.content?.length && (
                  <TableRow><TableCell colSpan={6}>No hay préstamos con atraso.</TableCell></TableRow>
                )}
              </TableBody>
            </Table>

            <TablePagination
              component="div"
              count={data.totalElements || 0}
              page={page}
              onPageChange={(_, p) => setPage(p)}
              rowsPerPage={size}
              onRowsPerPageChange={(e) => { setSize(parseInt(e.target.value,10)); setPage(0); }}
              rowsPerPageOptions={[5,10,12,20,50]}
              labelRowsPerPage="Filas por página"
            />
          </>
        )}
      </Paper>
    </Box>
  );
}
