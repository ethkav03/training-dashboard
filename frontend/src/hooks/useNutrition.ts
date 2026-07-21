import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createNutritionEntry,
  deleteNutritionEntry,
  getNutritionEntries,
  getNutritionSummary,
  updateNutritionEntry,
} from "../api/nutrition.js";

export function useNutritionSummary(date?: string) {
  return useQuery({
    queryKey: ["nutrition", "summary", date ?? "today"],
    queryFn: () => getNutritionSummary(date),
    refetchOnMount: "always",
  });
}

export function useNutritionEntries(params?: { from?: string; to?: string }) {
  return useQuery({
    queryKey: ["nutrition", "entries", params ?? {}],
    queryFn: () => getNutritionEntries(params),
    refetchOnMount: "always",
  });
}

export function useCreateNutritionEntry() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createNutritionEntry,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["nutrition"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard", "today"] });
      queryClient.invalidateQueries({ queryKey: ["insights"] });
      queryClient.invalidateQueries({ queryKey: ["timeline"] });
    },
  });
}

export function useUpdateNutritionEntry() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, patch }: { id: string; patch: Parameters<typeof updateNutritionEntry>[1] }) =>
      updateNutritionEntry(id, patch),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["nutrition"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard", "today"] });
      queryClient.invalidateQueries({ queryKey: ["insights"] });
      queryClient.invalidateQueries({ queryKey: ["timeline"] });
    },
  });
}

export function useDeleteNutritionEntry() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: deleteNutritionEntry,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["nutrition"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard", "today"] });
      queryClient.invalidateQueries({ queryKey: ["timeline"] });
    },
  });
}
