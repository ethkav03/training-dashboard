import { useQuery } from "@tanstack/react-query";
import { getTimeline } from "../api/timeline.js";

export function useTimeline(from: string, to: string) {
  return useQuery({ queryKey: ["timeline", from, to], queryFn: () => getTimeline(from, to) });
}
