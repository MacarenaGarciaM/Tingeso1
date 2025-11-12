import "./App.css";
import { BrowserRouter as Router, Route, Routes } from "react-router-dom";
import Navbar from "./components/Navbar";
import Home from "./components/Home";
import NotFound from "./components/NotFound";
import { useKeycloak } from "@react-keycloak/web";
import ToolNamesCategories from "./components/ToolNamesCategories";
import AuthProvisioner from "./components/AuthProvisioner";
import ToolCreate from "./components/ToolCreate";
import LoanCreate from "./components/LoanCreate";
import LoanReturn from "./components/LoanReturn";
import KardexList from "./components/KardexList";
import Ranking from "./components/Ranking";
import PagarMultas from "./components/PagarMultas";
import AllLoans from "./components/AllLoans";
import OverdueLoans from "./components/OverDueLoans";


export default function App() {
  const { initialized } = useKeycloak();
  if (!initialized) return <div>Cargando…</div>;

  return (
    <Router>
      {/* Se monta una vez y hace GET /auth/me cuando hay sesión */}
      <AuthProvisioner />

      <div className="container">
        <Navbar />
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/home" element={<Home />} />
          <Route path="*" element={<NotFound />} />
          <Route path="/tools/names" element={<ToolNamesCategories />} />
          <Route path="*" element={<NotFound />} />
          <Route path="/tools/names" element={<ToolNamesCategories />} />
          <Route path="/tools/create" element={<ToolCreate />} />
          <Route path="/loans/create" element={<LoanCreate />} />
          <Route path="/loans/return" element={<LoanReturn />} />
          <Route path="/kardex" element={<KardexList />} />
          <Route path="/reports/top-tools" element={<Ranking />} />
          <Route path="/admin/fines" element={<PagarMultas />} />
          <Route path="/loans/mine" element={<AllLoans />} />
          <Route path="/loans/overdue" element={<OverdueLoans />} />
        </Routes>
      </div>
    </Router>
  );
}
