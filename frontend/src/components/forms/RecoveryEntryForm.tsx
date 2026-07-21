import { useState } from "react";
import { Modal } from "../ui/Modal.js";
import { Button } from "../ui/Button.js";
import { useLogRecovery } from "../../hooks/useRecovery.js";

const inputClass =
  "w-full rounded-lg border border-hairline bg-page px-3 py-2 text-sm text-ink-primary outline-none focus:border-series-3";

export function RecoveryEntryForm({ onClose }: { onClose: () => void }) {
  const [sleepHours, setSleepHours] = useState("");
  const [sleepQuality, setSleepQuality] = useState("3");
  const [restingHr, setRestingHr] = useState("");
  const [hrv, setHrv] = useState("");
  const [soreness, setSoreness] = useState("3");
  const [energy, setEnergy] = useState("3");
  const mutation = useLogRecovery();

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    await mutation.mutateAsync({
      sleepHours: sleepHours ? Number(sleepHours) : undefined,
      sleepQuality: Number(sleepQuality),
      restingHr: restingHr ? Number(restingHr) : undefined,
      hrv: hrv ? Number(hrv) : undefined,
      soreness: Number(soreness),
      energy: Number(energy),
    });
    onClose();
  }

  return (
    <Modal title="Log today's recovery" onClose={onClose}>
      <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
        <label className="text-sm">
          <span className="mb-1 block text-ink-secondary">Sleep duration (hours)</span>
          <input
            autoFocus
            className={inputClass}
            type="number"
            step="0.25"
            value={sleepHours}
            onChange={(e) => setSleepHours(e.target.value)}
            placeholder="7.5"
          />
        </label>
        <div className="grid grid-cols-3 gap-3">
          <label className="text-sm">
            <span className="mb-1 block text-ink-secondary">Sleep quality (1-5)</span>
            <input className={inputClass} type="number" min={1} max={5} value={sleepQuality} onChange={(e) => setSleepQuality(e.target.value)} />
          </label>
          <label className="text-sm">
            <span className="mb-1 block text-ink-secondary">Soreness (1-5)</span>
            <input className={inputClass} type="number" min={1} max={5} value={soreness} onChange={(e) => setSoreness(e.target.value)} />
          </label>
          <label className="text-sm">
            <span className="mb-1 block text-ink-secondary">Energy (1-5)</span>
            <input className={inputClass} type="number" min={1} max={5} value={energy} onChange={(e) => setEnergy(e.target.value)} />
          </label>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <label className="text-sm">
            <span className="mb-1 block text-ink-secondary">Resting HR (optional)</span>
            <input className={inputClass} type="number" value={restingHr} onChange={(e) => setRestingHr(e.target.value)} />
          </label>
          <label className="text-sm">
            <span className="mb-1 block text-ink-secondary">HRV (optional)</span>
            <input className={inputClass} type="number" value={hrv} onChange={(e) => setHrv(e.target.value)} />
          </label>
        </div>
        <Button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? "Saving..." : "Save"}
        </Button>
      </form>
    </Modal>
  );
}
