import { useState } from "react";
import { Modal } from "../ui/Modal.js";
import { Button } from "../ui/Button.js";
import { useCreateWeightEntry } from "../../hooks/useWeight.js";

const inputClass =
  "w-full rounded-lg border border-hairline bg-page px-3 py-2 text-sm text-ink-primary outline-none focus:border-series-4";

export function WeightEntryForm({ onClose }: { onClose: () => void }) {
  const [weightKg, setWeightKg] = useState("");
  const [date, setDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [note, setNote] = useState("");
  const mutation = useCreateWeightEntry();

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    await mutation.mutateAsync({
      date: new Date(date).toISOString(),
      weightKg: Number(weightKg),
      note: note || undefined,
    });
    onClose();
  }

  return (
    <Modal title="Log weight" onClose={onClose}>
      <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
        <label className="text-sm">
          <span className="mb-1 block text-ink-secondary">Weight (kg)</span>
          <input
            autoFocus
            required
            className={inputClass}
            type="number"
            step="0.1"
            value={weightKg}
            onChange={(e) => setWeightKg(e.target.value)}
            placeholder="82.4"
          />
        </label>
        <label className="text-sm">
          <span className="mb-1 block text-ink-secondary">Date</span>
          <input className={inputClass} type="date" value={date} onChange={(e) => setDate(e.target.value)} />
        </label>
        <label className="text-sm">
          <span className="mb-1 block text-ink-secondary">Note (optional)</span>
          <input className={inputClass} value={note} onChange={(e) => setNote(e.target.value)} />
        </label>
        <Button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? "Saving..." : "Save"}
        </Button>
      </form>
    </Modal>
  );
}
