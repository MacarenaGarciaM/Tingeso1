import { useEffect, useState } from "react";
import {
  Box, Paper, Stack, TextField, Button,
  Typography, Table, TableHead, TableRow, TableCell, TableBody, Alert
} from "@mui/material";
import { getTopTools } from "../services/loan.service";
// Opcional (si usas recharts):
import { ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid } from "recharts";

export default function TopTools() {
  const [limit, setLimit] = useState(10); //maximo de herramientas
  const [start, setStart] = useState(""); // YYYY-MM-DD
  const [end, setEnd]     = useState(""); //rango inicio de fecha
  const [rows, setRows]   = useState([]); //rango fin fecha
  const [err, setErr]     = useState("");
  const [loading, setLoading] = useState(false);

  // carga datos desde el backend
  const load = async () => {
    try {
      setLoading(true);
      setErr("");
      const data = await getTopTools({
        limit: Number(limit) || 10, 
        start: start || undefined,
        end:   end   || undefined
      });
      setRows(Array.isArray(data) ? data : []);
    } catch (e) {
      setErr(e?.response?.data || e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); /* carga inicial */ }, []);

  const onSubmit = (e) => { e.preventDefault(); load(); }; //evita que se recarge la pagina y vuelve a llamar a load con los filtros

  return (
    <Box sx={{ p:3, maxWidth: 1000, mx:"auto" }}>
      <Typography variant="h5" sx={{ mb:2 }}>Herramientas más prestadas</Typography>
      {err && <Alert severity="error" sx={{ mb:2 }}>{String(err)}</Alert>}

      <Paper sx={{ p:2, mb:2 }} component="form" onSubmit={onSubmit}>
        <Stack direction={{ xs:"column", md:"row" }} spacing={2} useFlexGap flexWrap="wrap">
          <TextField
            label="Límite"
            type="number"
            inputProps={{ min:1, max:100 }}
            value={limit}
            onChange={e => setLimit(e.target.value)}
            sx={{ width: 140 }}
          />
          <TextField
            label="Desde"
            type="date"
            InputLabelProps={{ shrink: true }}
            value={start}
            onChange={e => setStart(e.target.value)}
          />
          <TextField
            label="Hasta"
            type="date"
            InputLabelProps={{ shrink: true }}
            value={end}
            onChange={e => setEnd(e.target.value)}
          />
          <Button type="submit" variant="contained" disabled={loading}>
            {loading ? "Cargando..." : "Buscar"}
          </Button>
        </Stack>
      </Paper>

      <Paper sx={{ p:2, mb:2 }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Herramienta</TableCell>
              <TableCell align="right">Veces</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((r, i) => (
              <TableRow key={i}>
                <TableCell>{r.tool}</TableCell>
                <TableCell align="right">{r.times}</TableCell>
              </TableRow>
            ))}
            {!rows.length && (
              <TableRow><TableCell colSpan={2}>
                <Typography color="text.secondary">Sin datos para el filtro seleccionado.</Typography>
              </TableCell></TableRow>
            )}
          </TableBody>
        </Table>
      </Paper>

      {!!rows.length && (
        <Paper sx={{ p:2 }}>
          <Typography variant="subtitle1" sx={{ mb:1 }}>Gráfico</Typography>
          <Box sx={{ width: "100%", height: 320 }}>
            <ResponsiveContainer>
              <BarChart data={rows}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="tool" tick={{ fontSize: 12 }} interval={0} angle={-15} textAnchor="end" height={70}/>
                <YAxis allowDecimals={false}/>
                <Tooltip />
                <Bar dataKey="times" fill="#ec7cc1"/>
              </BarChart>
            </ResponsiveContainer>
          </Box>
        </Paper>
      )}
    </Box>
  );
}
