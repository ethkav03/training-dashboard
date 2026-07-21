import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createWeightEntry, deleteWeightEntry, getWeightTrend, updateWeightEntry } from "../api/weight.js";

export function useWeightTrend(params?: { from?: string; to?: string }) {
  return useQuery({
    queryKey: ["weight", "trend", params ?? {}],
    queryFn: () => getWeightTrend(params),
    refetchOnMount: "always",
  });
}

export function useCreateWeightEntry() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createWeightEntry,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["weight"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard", "today"] });
      queryClient.invalidateQueries({ queryKey: ["insights"] });
      queryClient.invalidateQueries({ queryKey: ["goals"] });
      queryClient.invalidateQueries({ queryKey: ["timeline"] });
    },
  });
}

export function useUpdateWeightEntry() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, patch }: { id: string; patch: Parameters<typeof updateWeightEntry>[1] }) =>
      updateWeightEntry(id, patch),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["weight"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard", "today"] });
      queryClient.invalidateQueries({ queryKey: ["insights"] });
      queryClient.invalidateQueries({ queryKey: ["goals"] });
      queryClient.invalidateQueries({ queryKey: ["timeline"] });
    },
  });
}

export function useDeleteWeightEntry() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: deleteWeightEntry,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["weight"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard", "today"] });
      queryClient.invalidateQueries({ queryKey: ["timeline"] });
    },
  });
}
