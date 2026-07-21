import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { UnitSystem } from "@momentum/shared";
import { useAuth } from "../context/AuthContext.js";
import { deleteMe, exportDataUrl, updateMe } from "../api/users.js";
import { Card, CardTitle } from "../components/ui/Card.js";
import { Button } from "../components/ui/Button.js";

export function SettingsPage() {
  const { user, refreshUser, logout } = useAuth();
  const navigate = useNavigate();
  const [savingUnits, setSavingUnits] = useState(false);
  const [confirmingDelete, setConfirmingDelete] = useState(false);

  if (!user) return null;

  async function handleUnitChange(unitSystem: UnitSystem) {
    setSavingUnits(true);
    try {
      await updateMe({ unitSystem });
      await refreshUser();
    } finally {
      setSavingUnits(false);
    }
  }

  async function handleDelete() {
    await deleteMe();
    logout();
    navigate("/login", { replace: true });
  }

  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-4">
      <h1 className="text-xl font-semibold">Settings</h1>

      <Card>
        <CardTitle>Profile</CardTitle>
        <div className="mt-3 flex items-center gap-3">
          {user.avatarUrl ? (
            <img src={user.avatarUrl} alt={user.name} className="h-12 w-12 rounded-full" />
          ) : (
            <div className="h-12 w-12 rounded-full bg-series-1 text-white flex items-center justify-center text-lg font-semibold">
              {user.name[0]?.toUpperCase()}
            </div>
          )}
          <div>
            <div className="font-medium">{user.name}</div>
            <div className="text-sm text-ink-secondary">{user.email}</div>
          </div>
        </div>
      </Card>

      <Card>
        <CardTitle>Units & preferences</CardTitle>
        <div className="mt-3 flex items-center gap-3">
          <Button
            size="sm"
            variant={user.unitSystem === "METRIC" ? "primary" : "secondary"}
            disabled={savingUnits}
            onClick={() => handleUnitChange(UnitSystem.METRIC)}
          >
            Metric (kg/cm)
          </Button>
          <Button
            size="sm"
            variant={user.unitSystem === "IMPERIAL" ? "primary" : "secondary"}
            disabled={savingUnits}
            onClick={() => handleUnitChange(UnitSystem.IMPERIAL)}
          >
            Imperial (lb/in)
          </Button>
        </div>
      </Card>

      <Card>
        <CardTitle>Integrations</CardTitle>
        <p className="mt-2 text-sm text-ink-secondary">
          Google Fit, WHOOP and MyFitnessPal sync aren't connected yet in this build — the data model
          already tracks each record's source so these can be wired in later without a migration. For now,
          log everything manually via Progress and Training.
        </p>
      </Card>

      <Card>
        <CardTitle>Data controls</CardTitle>
        <div className="mt-3 flex flex-wrap items-center gap-3">
          <a href={exportDataUrl()} target="_blank" rel="noreferrer">
            <Button variant="secondary" size="sm">
              Export my data (JSON)
            </Button>
          </a>
          {!confirmingDelete ? (
            <Button variant="danger" size="sm" onClick={() => setConfirmingDelete(true)}>
              Delete account
            </Button>
          ) : (
            <div className="flex items-center gap-2">
              <span className="text-sm text-ink-secondary">Are you sure? This can't be undone.</span>
              <Button variant="danger" size="sm" onClick={handleDelete}>
                Yes, delete everything
              </Button>
              <Button variant="ghost" size="sm" onClick={() => setConfirmingDelete(false)}>
                Cancel
              </Button>
            </div>
          )}
        </div>
      </Card>
    </div>
  );
}
