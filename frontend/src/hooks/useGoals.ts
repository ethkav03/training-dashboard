import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createGoal, deleteGoal, getGoals, updateGoal } from "../api/goals.js";

export function useGoals(params?: { status?: string; type?: string }) {
  return useQuery({ queryKey: ["goals", params ?? {}], queryFn: () => getGoals(params) });
}

export function useCreateGoal() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createGoal,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["goals"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard", "today"] });
    },
  });
}

export function useUpdateGoal() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, patch }: { id: string; patch: Parameters<typeof updateGoal>[1] }) => updateGoal(id, patch),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["goals"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard", "today"] });
    },
  });
}

export function useDeleteGoal() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: deleteGoal,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["goals"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard", "today"] });
    },
  });
}
