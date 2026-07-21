import { useState } from "react";
import { Modal } from "../ui/Modal.js";
import { Button } from "../ui/Button.js";
import { useCreateNutritionEntry } from "../../hooks/useNutrition.js";

const inputClass =
  "w-full rounded-lg border border-hairline bg-page px-3 py-2 text-sm text-ink-primary outline-none focus:border-series-2";

export function NutritionEntryForm({ onClose }: { onClose: () => void }) {
  const [mealName, setMealName] = useState("");
  const [calories, setCalories] = useState("");
  const [proteinG, setProteinG] = useState("");
  const [carbsG, setCarbsG] = useState("");
  const [fatG, setFatG] = useState("");
  const mutation = useCreateNutritionEntry();

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    await mutation.mutateAsync({
      date: new Date().toISOString(),
      mealName: mealName || undefined,
      calories: Number(calories),
      proteinG: proteinG ? Number(proteinG) : undefined,
      carbsG: carbsG ? Number(carbsG) : undefined,
      fatG: fatG ? Number(fatG) : undefined,
    });
    onClose();
  }

  return (
    <Modal title="Log meal" onClose={onClose}>
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
        <Button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? "Saving..." : "Save"}
        </Button>
      </form>
    </Modal>
  );
}
