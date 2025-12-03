import { useEffect, useState } from "react";
import {
  Box, Paper, Stack, TextField, Button, Typography, Table, TableHead, TableRow,
  TableCell, TableBody, TableFooter, IconButton, Alert
} from "@mui/material";
import { getKardex } from "../services/kardex.service";
import { getMe } from "../services/auth.service";
import ChevronLeftIcon from "@mui/icons-material/ChevronLeft";
import ChevronRightIcon from "@mui/icons-material/ChevronRight";

const CLP = new Intl.NumberFormat("es-CL", { style:"currency", currency:"CLP", maximumFractionDigits:0 });

export default function KardexList() {
  const [me, setMe] = useState(null);
  const [rows, setRows] = useState([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(12);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [err, setErr] = useState("");

  // filtros
  const [toolId, setToolId] = useState("");
  const [rutUser, setRutUser] = useState("");
  const [type, setType] = useState("");
  const [name, setName] = useState("");
  const [category, setCategory] = useState("");
  const [start, setStart] = useState(""); // YYYY-MM-DD
  const [end, setEnd] = useState("");

  const load = async (p = page) => {
    try {
      const u = me || await getMe();
      if (!me) setMe(u);

      const resp = await getKardex({
        toolId: toolId || undefined,
        rutUser: rutUser || undefined,
        type: type || undefined,
        name: name || undefined,
        category: category || undefined,
        start: start || undefined,
        end: end || undefined,
        page: p,
        size,
        sort: "movementDate,desc"
      });
      setRows(resp.content || []);
      setTotalPages(resp.totalPages || 0);
      setTotalElements(resp.totalElements || 0);
      setPage(resp.number || 0);
      setErr("");
    } catch (e) {
      setErr(e?.response?.data || e.message);
    }
  };

  useEffect(() => {
    load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [size]); // si cambias tamaño, refresca

  const onSearch = (e) => {
    e.preventDefault();
    load(0);
  };

  return (
    <Box sx={{ p:3, maxWidth: 1100, mx:"auto" }}>
      <Typography variant="h5" sx={{ mb:2 }}>Kardex de herramientas</Typography>
      {err && <Alert severity="error" sx={{ mb:2 }}>{String(err)}</Alert>}

      <Paper sx={{ p:2, mb:2 }} component="form" onSubmit={onSearch}>
        <Stack direction={{ xs:"column", md:"row" }} spacing={2} useFlexGap flexWrap="wrap">
          <TextField label="Tool ID" value={toolId} onChange={e=>setToolId(e.target.value)} />
          <TextField label="RUT usuario" value={rutUser} onChange={e=>setRutUser(e.target.value)} />
          <TextField label="Tipo (ej. Ingreso)" value={type} onChange={e=>setType(e.target.value)} />
          <TextField label="Nombre" value={name} onChange={e=>setName(e.target.value)} />
          <TextField label="Categoría" value={category} onChange={e=>setCategory(e.target.value)} />
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
          <Button type="submit" variant="contained">Buscar</Button>
        </Stack>
      </Paper>

      <Paper sx={{ p:2 }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Fecha</TableCell>
              <TableCell>Tipo</TableCell>
              <TableCell>Tool ID</TableCell>
              <TableCell>Herramienta</TableCell>
              <TableCell>Categoría</TableCell>
              <TableCell align="right">Stock</TableCell>
              <TableCell>RUT</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map(k => (
              <TableRow key={k.id}>
                <TableCell>{k.movementDate}</TableCell>
                <TableCell>{k.type}</TableCell>
                <TableCell>{k.tool?.id}</TableCell>
                <TableCell>{k.tool?.name}</TableCell>
                <TableCell>{k.tool?.category}</TableCell>
                <TableCell align="right">{k.stock}</TableCell>
                <TableCell>{k.rutUser}</TableCell>
              </TableRow>
            ))}
            {!rows.length && (
              <TableRow>
                <TableCell colSpan={7}>
                  <Typography color="text.secondary">Sin movimientos que coincidan con los filtros.</Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
          <TableFooter>
            <TableRow>
              <TableCell colSpan={7}>
                <Box sx={{ display:"flex", alignItems:"center", justifyContent:"space-between" }}>
                  <Typography variant="body2">
                    Página {page+1} de {Math.max(totalPages,1)} — {totalElements} movimientos
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
    </Box>
  );
}
