import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createTrainingSession,
  deleteTrainingSession,
  getExerciseNames,
  getExerciseProgression,
  getLoadSummary,
  getTrainingSession,
  getTrainingSessions,
  updateTrainingSession,
} from "../api/training.js";
import type { TrainingSessionWriteDto } from "@momentum/shared";

export function useTrainingSessions(params?: { from?: string; to?: string; type?: string }) {
  return useQuery({
    queryKey: ["training", "list", params ?? {}],
    queryFn: () => getTrainingSessions(params),
    refetchOnMount: "always",
  });
}

export function useTrainingSession(id: string | undefined) {
  return useQuery({
    queryKey: ["training", "detail", id],
    queryFn: () => getTrainingSession(id!),
    enabled: !!id,
  });
}

export function useLoadSummary() {
  return useQuery({ queryKey: ["training", "load-summary"], queryFn: getLoadSummary, refetchOnMount: "always" });
}

export function useExerciseNames() {
  return useQuery({ queryKey: ["training", "exercise-names"], queryFn: getExerciseNames });
}

export function useExerciseProgression(name: string | undefined) {
  return useQuery({
    queryKey: ["training", "progression", name],
    queryFn: () => getExerciseProgression(name!),
    enabled: !!name,
  });
}

export function useCreateTrainingSession() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createTrainingSession,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["training"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard", "today"] });
      queryClient.invalidateQueries({ queryKey: ["insights"] });
      queryClient.invalidateQueries({ queryKey: ["goals"] });
      queryClient.invalidateQueries({ queryKey: ["nutrition"] });
      queryClient.invalidateQueries({ queryKey: ["timeline"] });
    },
  });
}

export function useUpdateTrainingSession() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: TrainingSessionWriteDto }) =>
      updateTrainingSession(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["training"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard", "today"] });
      queryClient.invalidateQueries({ queryKey: ["insights"] });
      queryClient.invalidateQueries({ queryKey: ["goals"] });
      queryClient.invalidateQueries({ queryKey: ["nutrition"] });
      queryClient.invalidateQueries({ queryKey: ["timeline"] });
    },
  });
}

export function useDeleteTrainingSession() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: deleteTrainingSession,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["training"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard", "today"] });
      queryClient.invalidateQueries({ queryKey: ["timeline"] });
    },
  });
}
