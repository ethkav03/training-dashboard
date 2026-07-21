import { useState } from "react";
import type { NutritionEntryDto } from "@momentum/shared";
import { Modal } from "../ui/Modal.js";
import { Button } from "../ui/Button.js";
import { useCreateNutritionEntry, useUpdateNutritionEntry } from "../../hooks/useNutrition.js";

const inputClass =
  "w-full rounded-lg border border-hairline bg-page px-3 py-2 text-sm text-ink-primary outline-none focus:border-series-2";

export function NutritionEntryForm({ onClose, entry }: { onClose: () => void; entry?: NutritionEntryDto }) {
  const [mealName, setMealName] = useState(entry?.mealName ?? "");
  const [calories, setCalories] = useState(entry ? String(entry.calories) : "");
  const [proteinG, setProteinG] = useState(entry?.proteinG != null ? String(entry.proteinG) : "");
  const [carbsG, setCarbsG] = useState(entry?.carbsG != null ? String(entry.carbsG) : "");
  const [fatG, setFatG] = useState(entry?.fatG != null ? String(entry.fatG) : "");
  const createMutation = useCreateNutritionEntry();
  const updateMutation = useUpdateNutritionEntry();
  const isPending = createMutation.isPending || updateMutation.isPending;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const payload = {
      mealName: mealName || undefined,
      calories: Number(calories),
      proteinG: proteinG ? Number(proteinG) : undefined,
      carbsG: carbsG ? Number(carbsG) : undefined,
      fatG: fatG ? Number(fatG) : undefined,
    };
    if (entry) {
      await updateMutation.mutateAsync({ id: entry.id, patch: payload });
    } else {
      await createMutation.mutateAsync({ ...payload, date: new Date().toISOString() });
    }
    onClose();
  }

  return (
    <Modal title={entry ? "Edit meal" : "Log meal"} onClose={onClose}>
      <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
        <label className="text-sm">
          <span className="mb-1 block text-ink-secondary">Meal name (optional)</span>
          <input
            autoFocus
            className={inputClass}
            value={mealName}
            onChange={(e) => setMealName(e.target.value)}
            placeholder="Lunch"
          />
        </label>
        <label className="text-sm">
          <span className="mb-1 block text-ink-secondary">Calories</span>
          <input
            required
            className={inputClass}
            type="number"
            value={calories}
            onChange={(e) => setCalories(e.target.value)}
            placeholder="650"
          />
        </label>
        <div className="grid grid-cols-3 gap-3">
          <label className="text-sm">
            <span className="mb-1 block text-ink-secondary">Protein (g)</span>
            <input className={inputClass} type="number" value={proteinG} onChange={(e) => setProteinG(e.target.value)} />
          </label>
          <label className="text-sm">
            <span className="mb-1 block text-ink-secondary">Carbs (g)</span>
            <input className={inputClass} type="number" value={carbsG} onChange={(e) => setCarbsG(e.target.value)} />
          </label>
          <label className="text-sm">
            <span className="mb-1 block text-ink-secondary">Fat (g)</span>
            <input className={inputClass} type="number" value={fatG} onChange={(e) => setFatG(e.target.value)} />
          </label>
        </div>
        <Button type="submit" disabled={isPending}>
          {isPending ? "Saving..." : entry ? "Save changes" : "Save"}
        </Button>
      </form>
    </Modal>
  );
}
