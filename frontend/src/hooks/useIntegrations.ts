import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { disconnectWhoop, getIntegrations, getWhoopConnectUrl, syncWhoopNow } from "../api/integrations.js";

export function useIntegrations() {
  return useQuery({ queryKey: ["integrations"], queryFn: getIntegrations });
}

export function useConnectWhoop() {
  return useMutation({
    mutationFn: async () => {
      const { authorizeUrl } = await getWhoopConnectUrl();
      window.location.href = authorizeUrl;
    },
  });
}

export function useSyncWhoopNow() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: syncWhoopNow,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["integrations"] });
      queryClient.invalidateQueries({ queryKey: ["recovery"] });
      queryClient.invalidateQueries({ queryKey: ["training"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard", "today"] });
      queryClient.invalidateQueries({ queryKey: ["insights"] });
      queryClient.invalidateQueries({ queryKey: ["timeline"] });
      queryClient.invalidateQueries({ queryKey: ["goals"] });
    },
  });
}

export function useDisconnectWhoop() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: disconnectWhoop,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["integrations"] });
    },
  });
}
