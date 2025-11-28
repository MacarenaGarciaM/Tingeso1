import * as React from "react";
import Box from "@mui/material/Box";
import Drawer from "@mui/material/Drawer";
import List from "@mui/material/List";
import Divider from "@mui/material/Divider";
import ListItemButton from "@mui/material/ListItemButton";
import ListItemIcon from "@mui/material/ListItemIcon";
import ListItemText from "@mui/material/ListItemText";
import Construction from "@mui/icons-material/Construction";
import HardwareIcon from '@mui/icons-material/Hardware';
import AddShoppingCartIcon from '@mui/icons-material/AddShoppingCart';
import ThreeSixtyIcon from '@mui/icons-material/ThreeSixty';
import HistoryIcon from '@mui/icons-material/History';
import PaidIcon from '@mui/icons-material/Paid';
import HomeIcon from "@mui/icons-material/Home";
import AssignmentIcon from '@mui/icons-material/Assignment';
import AnalyticsIcon from '@mui/icons-material/Analytics';
import { useNavigate } from "react-router-dom";
import { useKeycloak } from "@react-keycloak/web";
import AssignmentLateIcon from '@mui/icons-material/AssignmentLate';  

export default function Sidemenu({ open, toggleDrawer }) {
  const navigate = useNavigate();
  const { keycloak } = useKeycloak();               

  const realmRoles = keycloak?.tokenParsed?.realm_access?.roles || [];
  const clientRoles = keycloak?.tokenParsed?.resource_access?.["sisgr-frontend"]?.roles || [];
  const isAdmin =
    realmRoles.includes("ADMIN") ||
    clientRoles.includes("admin") ||
    clientRoles.includes("ADMIN");
  const isUserOrAdmin = realmRoles.includes("ADMIN") || realmRoles.includes("USER")|| clientRoles.includes("admin") || clientRoles.includes("USER") || clientRoles.includes("user");

  const listOptions = () => (
    <Box role="presentation" onClick={toggleDrawer(false)}>
      <List>
        <ListItemButton onClick={() => navigate("/home")}>
          <ListItemIcon><HomeIcon /></ListItemIcon>
          <ListItemText primary="Home" />
        </ListItemButton>

        <Divider />

        {isUserOrAdmin && (
        <ListItemButton onClick={() => navigate("/tools/names")}>
          <ListItemIcon><Construction /></ListItemIcon>
          <ListItemText primary="Herramientas (Nombre/Categoría)" />
        </ListItemButton>
        )}

        {isAdmin && (
          <ListItemButton onClick={() => navigate("/tools/create")}>
            <ListItemIcon><HardwareIcon/></ListItemIcon>
            <ListItemText primary="Crear herramienta" />
          </ListItemButton>
        )}

        {isUserOrAdmin && (
          <ListItemButton onClick={() => navigate("/loans/create")}>
            <ListItemIcon><AddShoppingCartIcon /></ListItemIcon>
            <ListItemText primary="Crear préstamo" />
            </ListItemButton>
          )}

        {isUserOrAdmin && (  
        <ListItemButton onClick={() => navigate("/reports/top-tools")}>
          <ListItemIcon><AnalyticsIcon /></ListItemIcon>
          <ListItemText primary="Ranking de herramientas" />
        </ListItemButton>
        )}

      </List>



      <Divider />

      {isUserOrAdmin && (  
      <ListItemButton onClick={() => navigate("/loans/mine")}>
        <ListItemIcon><AssignmentIcon /></ListItemIcon>
        <ListItemText primary="Mis préstamos" />
      </ListItemButton>
      )}

      <List>
        {isAdmin && (
          <ListItemButton onClick={() => navigate("/loans/return")}>
            <ListItemIcon><ThreeSixtyIcon /></ListItemIcon>
            <ListItemText primary="Devolución de herramientas" />
          </ListItemButton>
        )}

        {isAdmin &&(
          <ListItemButton onClick={() => navigate("/kardex")}>
            <ListItemIcon><HistoryIcon /></ListItemIcon>
            <ListItemText primary="Kardex" />
          </ListItemButton>
        )}

        {isAdmin && (
        <ListItemButton onClick={() => navigate("/admin/fines")}>
          <ListItemIcon><PaidIcon /></ListItemIcon>
          <ListItemText primary="Multas y penalizaciones" />
        </ListItemButton>
      )}

      {isAdmin && (
      <ListItemButton onClick={() => navigate("/loans/overdue")}>
        <ListItemIcon><AssignmentLateIcon /></ListItemIcon>
        <ListItemText primary="Préstamos con atraso" />
      </ListItemButton>
    )}



      </List>
    </Box>
  );

  return (
    <div>
      <Drawer anchor="left" open={open} onClose={toggleDrawer(false)}>
        {listOptions()}
      </Drawer>
    </div>
  );
}
