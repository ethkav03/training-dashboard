import type { InsightDto } from "@momentum/shared";

// Full rule-based insight generation (weight trend, training volume, sleep
// baseline, calorie/protein vs. goal) lands in the Insights sprint. Stubbed
// here so the dashboard composition endpoint has a stable shape to depend on.
export async function getTopInsights(_userId: string, _limit: number): Promise<InsightDto[]> {
  return [];
}

export async function getAllInsights(_userId: string): Promise<InsightDto[]> {
  return [];
}
