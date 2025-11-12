import api from "./api";

export async function getDailyRate() {
  const { data } = await api.get("/settings/daily-rate");
  return Number(data?.value ?? 0);
}

export async function setDailyRate(value) {
  const { data } = await api.put("/settings/daily-rate", { value: Number(value) || 0 });
  return Number(data?.value ?? 0);
}
