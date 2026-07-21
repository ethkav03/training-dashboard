import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createNutritionEntry,
  deleteNutritionEntry,
  getNutritionEntries,
  getNutritionSummary,
} from "../api/nutrition.js";

export function useNutritionSummary(date?: string) {
  return useQuery({
    queryKey: ["nutrition", "summary", date ?? "today"],
    queryFn: () => getNutritionSummary(date),
  });
}

export function useNutritionEntries(params?: { from?: string; to?: string }) {
  return useQuery({
    queryKey: ["nutrition", "entries", params ?? {}],
    queryFn: () => getNutritionEntries(params),
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
    },
  });
}
