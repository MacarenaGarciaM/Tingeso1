// src/components/Navbar.jsx
import * as React from "react";
import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import Button from "@mui/material/Button";
import IconButton from "@mui/material/IconButton";
import MenuIcon from "@mui/icons-material/Menu";
import Sidemenu from "./Sidemenu";
import { useState } from "react";
import { useKeycloak } from "@react-keycloak/web";

export default function Navbar() {
  const [open, setOpen] = useState(false);
  const { keycloak, initialized } = useKeycloak();

  const toggleDrawer = (open) => () => setOpen(open);

  const handleLogin = () =>
    keycloak.login({ redirectUri: window.location.origin + "/" });

  const handleRegister = () =>
    keycloak.register({ redirectUri: window.location.origin + "/" });

  const handleLogout = () =>
    keycloak.logout({ redirectUri: window.location.origin + "/" });

  return (
    <Box sx={{ flexGrow: 1 }}>
      <AppBar position="static" sx={{ bgcolor: "#ec7cc1", color: "#ffffffff" }}>
        <Toolbar>
          <IconButton
            size="large"
            edge="start"
            aria-label="menu"
            sx={{ mr: 2, color: "#ffffffff" }} // White color for the menu icon
            onClick={toggleDrawer(true)}
          >
            <MenuIcon />
          </IconButton>


          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}> 
            ToolRent: Sistema de arriendo de herramientas  
          </Typography> 


          {!initialized ? null : keycloak.authenticated ? (
            <>
              <Typography sx={{ mr: 2 }}>
                {keycloak.tokenParsed?.preferred_username}
              </Typography>
              <Button color="inherit" onClick={handleLogout}>
                Logout
              </Button>
            </>
          ) : (
            <>
              <Button color="inherit" onClick={handleLogin}>
                Login
              </Button>
              <Button color="inherit" onClick={handleRegister}>
                Crear usuario
              </Button>
            </>
          )}
        </Toolbar>
      </AppBar>

      <Sidemenu open={open} toggleDrawer={toggleDrawer} />
    </Box>
  );
}
