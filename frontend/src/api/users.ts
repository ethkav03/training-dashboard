import type { OnboardingRequest, UserDto } from "@momentum/shared";
import { apiClient } from "./client.js";

export async function getMe(): Promise<UserDto> {
  const { data } = await apiClient.get("/users/me");
  return data;
}

export async function updateMe(patch: Partial<UserDto>): Promise<UserDto> {
  const { data } = await apiClient.patch("/users/me", patch);
  return data;
}

export async function submitOnboarding(payload: OnboardingRequest): Promise<UserDto> {
  const { data } = await apiClient.post("/users/me/onboarding", payload);
  return data;
}

export async function skipOnboarding(): Promise<UserDto> {
  const { data } = await apiClient.post("/users/me/onboarding/skip");
  return data;
}

export async function deleteMe(): Promise<void> {
  await apiClient.delete("/users/me");
}

export function exportDataUrl(): string {
  return `${import.meta.env.VITE_API_BASE_URL}/users/me/export`;
}
