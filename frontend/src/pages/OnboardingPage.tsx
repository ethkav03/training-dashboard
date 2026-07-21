import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { GoalDirection, GoalType, UnitSystem } from "@momentum/shared";
import { skipOnboarding, submitOnboarding } from "../api/users.js";
import { useAuth } from "../context/AuthContext.js";
import { Button } from "../components/ui/Button.js";
import { Card } from "../components/ui/Card.js";

const PRIMARY_GOAL_OPTIONS: { value: string; label: string }[] = [
  { value: "LOSE_WEIGHT", label: "Lose weight" },
  { value: "MAINTAIN", label: "Maintain" },
  { value: "GAIN_MUSCLE", label: "Gain muscle" },
  { value: "IMPROVE_PERFORMANCE", label: "Improve performance" },
  { value: "CUSTOM", label: "Custom" },
];

const inputClass =
  "w-full rounded-lg border border-hairline bg-page px-3 py-2 text-sm text-ink-primary outline-none focus:border-series-1";

export function OnboardingPage() {
  const { refreshUser } = useAuth();
  const navigate = useNavigate();
  const [submitting, setSubmitting] = useState(false);

  const [heightCm, setHeightCm] = useState("");
  const [currentWeightKg, setCurrentWeightKg] = useState("");
  const [unitSystem, setUnitSystem] = useState<UnitSystem>(UnitSystem.METRIC);
  const [trainingFrequencyPerWeek, setTrainingFrequencyPerWeek] = useState("4");
  const [primaryGoalOption, setPrimaryGoalOption] = useState("LOSE_WEIGHT");
  const [targetWeightKg, setTargetWeightKg] = useState("");

  async function handleSkip() {
    setSubmitting(true);
    try {
      await skipOnboarding();
      await refreshUser();
      navigate("/today", { replace: true });
    } finally {
      setSubmitting(false);
    }
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    try {
      const wantsWeightGoal = primaryGoalOption === "LOSE_WEIGHT" || primaryGoalOption === "GAIN_MUSCLE";
      await submitOnboarding({
        heightCm: heightCm ? Number(heightCm) : undefined,
        currentWeightKg: currentWeightKg ? Number(currentWeightKg) : undefined,
        unitSystem,
        trainingFrequencyPerWeek: trainingFrequencyPerWeek ? Number(trainingFrequencyPerWeek) : undefined,
        primaryGoal:
          wantsWeightGoal && targetWeightKg
            ? {
                type: GoalType.BODY_WEIGHT,
                title: primaryGoalOption === "LOSE_WEIGHT" ? "Reach target weight" : "Gain muscle weight",
                targetValue: Number(targetWeightKg),
                targetUnit: "kg",
                direction:
                  primaryGoalOption === "LOSE_WEIGHT" ? GoalDirection.DECREASE : GoalDirection.INCREASE,
              }
            : undefined,
      });
      await refreshUser();
      navigate("/today", { replace: true });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-page px-4 py-8">
      <Card className="w-full max-w-md">
        <h1 className="text-xl font-semibold">Let's set up Momentum</h1>
        <p className="mt-1 text-sm text-ink-secondary">
          A few quick details help personalize your dashboard. You can skip this and fill it in later.
        </p>

        <form className="mt-6 flex flex-col gap-4" onSubmit={handleSubmit}>
          <div className="grid grid-cols-2 gap-3">
            <label className="text-sm">
              <span className="mb-1 block text-ink-secondary">Units</span>
              <select
                className={inputClass}
                value={unitSystem}
                onChange={(e) => setUnitSystem(e.target.value as UnitSystem)}
              >
                <option value={UnitSystem.METRIC}>Metric (kg/cm)</option>
                <option value={UnitSystem.IMPERIAL}>Imperial (lb/in)</option>
              </select>
            </label>
            <label className="text-sm">
              <span className="mb-1 block text-ink-secondary">Height (cm)</span>
              <input
                className={inputClass}
                type="number"
                value={heightCm}
                onChange={(e) => setHeightCm(e.target.value)}
                placeholder="178"
              />
            </label>
          </div>

          <label className="text-sm">
            <span className="mb-1 block text-ink-secondary">Current weight (kg)</span>
            <input
              className={inputClass}
              type="number"
              step="0.1"
              value={currentWeightKg}
              onChange={(e) => setCurrentWeightKg(e.target.value)}
              placeholder="82.5"
            />
          </label>

          <label className="text-sm">
            <span className="mb-1 block text-ink-secondary">Primary goal</span>
            <select
              className={inputClass}
              value={primaryGoalOption}
              onChange={(e) => setPrimaryGoalOption(e.target.value)}
            >
              {PRIMARY_GOAL_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </label>

          {(primaryGoalOption === "LOSE_WEIGHT" || primaryGoalOption === "GAIN_MUSCLE") && (
            <label className="text-sm">
              <span className="mb-1 block text-ink-secondary">Target weight (kg)</span>
              <input
                className={inputClass}
                type="number"
                step="0.1"
                value={targetWeightKg}
                onChange={(e) => setTargetWeightKg(e.target.value)}
                placeholder="76"
              />
            </label>
          )}

          <label className="text-sm">
            <span className="mb-1 block text-ink-secondary">Typical training frequency (sessions/week)</span>
            <input
              className={inputClass}
              type="number"
              min={0}
              max={14}
              value={trainingFrequencyPerWeek}
              onChange={(e) => setTrainingFrequencyPerWeek(e.target.value)}
            />
          </label>

          <div className="mt-2 flex gap-3">
            <Button type="submit" disabled={submitting} className="flex-1">
              {submitting ? "Saving..." : "Get started"}
            </Button>
            <Button type="button" variant="ghost" disabled={submitting} onClick={handleSkip}>
              Skip for now
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
}
