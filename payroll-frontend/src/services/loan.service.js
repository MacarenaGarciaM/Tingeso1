import api from "./api";

export function createLoan(rutUser, reservationDate, returnDate, items) {
  return api.post("/loan", items, {
    params: { rutUser, reservationDate, returnDate }
  }).then(r => r.data);
}

export async function getActiveLoans(rutUser) {
  try {
    const params = rutUser ? { rutUser } : undefined;
    const res = await api.get("/loan/active", { params });

    let data = res.data;
    if (typeof data === "string") {
      try { data = JSON.parse(data); } catch { /* deja data tal cual */ }
    }

    console.log("[getActiveLoans] params:", params, "len:", Array.isArray(data) ? data.length : typeof data);
    return data;
  } catch (e) {
    console.error("[getActiveLoans] ERROR", e?.response?.status, e?.response?.data);
    throw e;
  }
}

export async function returnLoan(loanId, payload) {
  const res = await api.post(`/loan/${loanId}/return`, payload);
  return res.data;
}

export function getTopTools({ limit = 10, start, end } = {}) {
  const params = {};
  if (limit) params.limit = limit;
  if (start) params.start = start; // "YYYY-MM-DD"
  if (end)   params.end   = end;
  return api.get("/loan/top", { params }).then(r => r.data); // [{tool, times}]
}

export function getLoansWithDebts({ rutUser, start, end, page = 0, size = 12, sort = "reservationDate,desc" } = {}) {
  const params = { page, size, sort };
  if (rutUser) params.rutUser = rutUser;
  if (start) params.start = start;
  if (end) params.end = end;
  return api.get("/loan/debts", { params }).then(r => r.data);  // Page<LoanEntity>
}

export function payFines(loanId, { payLateFine, payDamagePenalty }) {
  return api.post(`/loan/${loanId}/pay-fines`, { payLateFine, payDamagePenalty }).then(r => r.data);
}

export function getLoansByRut({ rutUser, page = 0, size = 12, sort = "reservationDate,desc" }) {
  return api.get("/loan/by-rut", { params: { rutUser, page, size, sort } }).then(r => r.data);
}

export async function getOverdueLoans({ rutUser, page = 0, size = 12, sort = "returnDate,asc" } = {}) {
  const params = new URLSearchParams({ page, size, sort });
  if (rutUser) params.append("rutUser", rutUser);
  const { data } = await api.get(`/loan/overdue?${params.toString()}`);
  return data; 
}

