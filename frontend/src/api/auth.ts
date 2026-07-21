import { apiClient } from "./client.js";

export function googleLoginUrl(): string {
  return `${import.meta.env.VITE_API_BASE_URL}/auth/google`;
}

export async function getAuthStatus(): Promise<{ googleOAuthConfigured: boolean }> {
  const { data } = await apiClient.get("/auth/status");
  return data;
}

export async function devLogin(): Promise<{ token: string }> {
  const { data } = await apiClient.post("/auth/dev-login");
  return data;
}
