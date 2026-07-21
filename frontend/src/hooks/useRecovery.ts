import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { getRecoveryHistory, getRecoveryToday, logRecovery } from "../api/recovery.js";

export function useRecoveryToday() {
  return useQuery({ queryKey: ["recovery", "today"], queryFn: getRecoveryToday, refetchOnMount: "always" });
}

export function useRecoveryHistory(params?: { from?: string; to?: string }) {
  return useQuery({ queryKey: ["recovery", "history", params ?? {}], queryFn: () => getRecoveryHistory(params) });
}

export function useLogRecovery() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: logRecovery,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["recovery"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard", "today"] });
      queryClient.invalidateQueries({ queryKey: ["insights"] });
      queryClient.invalidateQueries({ queryKey: ["goals"] });
    },
  });
}
