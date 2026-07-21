import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { UnitSystem } from "@momentum/shared";
import { useAuth } from "../context/AuthContext.js";
import { deleteMe, exportDataUrl, updateMe } from "../api/users.js";
import { Card, CardTitle } from "../components/ui/Card.js";
import { Button } from "../components/ui/Button.js";
import { applyThemePreference, getStoredThemePreference, type ThemePreference } from "../lib/theme.js";
import { useConnectWhoop, useDisconnectWhoop, useIntegrations, useSyncWhoopNow } from "../hooks/useIntegrations.js";

export function SettingsPage() {
  const { user, refreshUser, logout } = useAuth();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [savingUnits, setSavingUnits] = useState(false);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [confirmingWhoopDisconnect, setConfirmingWhoopDisconnect] = useState(false);
  const [burnBaseline, setBurnBaseline] = useState(user?.estimatedDailyBurnKcal?.toString() ?? "");
  const [savingBurn, setSavingBurn] = useState(false);
  const [theme, setTheme] = useState<ThemePreference>(getStoredThemePreference());
  const [whoopCallbackNotice, setWhoopCallbackNotice] = useState<{ kind: "connected" | "error"; message?: string } | null>(
    null
  );

  const { data: integrations, refetch: refetchIntegrations } = useIntegrations();
  const connectWhoop = useConnectWhoop();
  const syncWhoopNow = useSyncWhoopNow();
  const disconnectWhoop = useDisconnectWhoop();
  const whoop = integrations?.find((i) => i.provider === "WHOOP");
  const healthConnect = integrations?.find((i) => i.provider === "HEALTH_CONNECT");

  useEffect(() => {
    const whoopParam = searchParams.get("whoop");
    if (!whoopParam) return;
    setWhoopCallbackNotice({
      kind: whoopParam === "connected" ? "connected" : "error",
      message: searchParams.get("message") ?? undefined,
    });
    setSearchParams((params) => {
      params.delete("whoop");
      params.delete("message");
      return params;
    }, { replace: true });
    refetchIntegrations();
  }, []);

  function handleThemeChange(preference: ThemePreference) {
    setTheme(preference);
    applyThemePreference(preference);
  }

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

  async function handleSaveBurnBaseline(e: React.FormEvent) {
    e.preventDefault();
    setSavingBurn(true);
    try {
      await updateMe({ estimatedDailyBurnKcal: burnBaseline ? Number(burnBaseline) : undefined });
      await refreshUser();
    } finally {
      setSavingBurn(false);
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
        <CardTitle>Appearance</CardTitle>
        <div className="mt-3 flex items-center gap-3">
          <Button size="sm" variant={theme === "system" ? "primary" : "secondary"} onClick={() => handleThemeChange("system")}>
            System
          </Button>
          <Button size="sm" variant={theme === "light" ? "primary" : "secondary"} onClick={() => handleThemeChange("light")}>
            Light
          </Button>
          <Button size="sm" variant={theme === "dark" ? "primary" : "secondary"} onClick={() => handleThemeChange("dark")}>
            Dark
          </Button>
        </div>
      </Card>

      <Card>
        <CardTitle>Energy baseline</CardTitle>
        <p className="mt-1 text-sm text-ink-secondary">
          Your estimated daily calorie burn at rest, before training. Fuel uses this to estimate energy
          balance — it's always shown as an estimate, never false precision.
        </p>
        <form className="mt-3 flex items-center gap-3" onSubmit={handleSaveBurnBaseline}>
          <input
            className="w-32 rounded-lg border border-hairline bg-page px-3 py-2 text-sm outline-none focus:border-series-2"
            type="number"
            value={burnBaseline}
            onChange={(e) => setBurnBaseline(e.target.value)}
            placeholder="2200"
          />
          <span className="text-sm text-ink-secondary">kcal/day</span>
          <Button type="submit" size="sm" disabled={savingBurn}>
            {savingBurn ? "Saving..." : "Save"}
          </Button>
        </form>
      </Card>

      <Card>
        <CardTitle>Integrations</CardTitle>

        {whoopCallbackNotice?.kind === "connected" && (
          <div className="mt-2 rounded-md border border-hairline bg-page px-3 py-2 text-sm text-status-good">
            WHOOP connected successfully.
          </div>
        )}
        {whoopCallbackNotice?.kind === "error" && (
          <div className="mt-2 rounded-md border border-hairline bg-page px-3 py-2 text-sm text-status-critical">
            Couldn't connect WHOOP{whoopCallbackNotice.message ? `: ${whoopCallbackNotice.message}` : "."}
          </div>
        )}

        <div className="mt-3 flex flex-col gap-4">
          <div>
            <div className="flex items-center justify-between">
              <div>
                <div className="font-medium text-ink-primary">WHOOP</div>
                <div className="text-xs text-ink-secondary">Recovery, sleep and workout data</div>
              </div>
              {whoop?.connected ? (
                <div className="flex items-center gap-2">
                  <Button
                    size="sm"
                    variant="secondary"
                    disabled={syncWhoopNow.isPending || whoop.lastSyncStatus === "SYNCING"}
                    onClick={() => syncWhoopNow.mutate()}
                  >
                    {syncWhoopNow.isPending || whoop.lastSyncStatus === "SYNCING" ? "Syncing..." : "Sync now"}
                  </Button>
                  {!confirmingWhoopDisconnect ? (
                    <Button size="sm" variant="danger" onClick={() => setConfirmingWhoopDisconnect(true)}>
                      Disconnect
                    </Button>
                  ) : (
                    <div className="flex items-center gap-2">
                      <Button
                        size="sm"
                        variant="danger"
                        onClick={() => {
                          disconnectWhoop.mutate();
                          setConfirmingWhoopDisconnect(false);
                        }}
                      >
                        Confirm
                      </Button>
                      <Button size="sm" variant="ghost" onClick={() => setConfirmingWhoopDisconnect(false)}>
                        Cancel
                      </Button>
                    </div>
                  )}
                </div>
              ) : (
                <Button
                  size="sm"
                  disabled={!whoop?.configured || connectWhoop.isPending}
                  onClick={() => connectWhoop.mutate()}
                >
                  Connect WHOOP
                </Button>
              )}
            </div>
            {whoop?.connected && (
              <div className="mt-1.5 text-xs text-ink-secondary">
                {whoop.lastSyncAt
                  ? `Last synced ${new Date(whoop.lastSyncAt).toLocaleString()}`
                  : "Connected — not synced yet"}
              </div>
            )}
            {whoop?.connected && whoop.lastSyncStatus === "ERROR" && whoop.lastSyncError && (
              <div className="mt-1.5 rounded-md border border-hairline bg-page px-3 py-2 text-xs text-status-critical">
                Last sync failed: {whoop.lastSyncError}
              </div>
            )}
            {syncWhoopNow.data?.status === "SUCCESS" && (
              <div className="mt-1.5 text-xs text-status-good">
                Synced {syncWhoopNow.data.recoveryRecordsSynced} recovery day
                {syncWhoopNow.data.recoveryRecordsSynced === 1 ? "" : "s"} and{" "}
                {syncWhoopNow.data.trainingSessionsSynced} workout
                {syncWhoopNow.data.trainingSessionsSynced === 1 ? "" : "s"}
                {syncWhoopNow.data.recoveryRecordsSkippedManualEdit > 0 &&
                  ` (${syncWhoopNow.data.recoveryRecordsSkippedManualEdit} day${syncWhoopNow.data.recoveryRecordsSkippedManualEdit === 1 ? "" : "s"} skipped — manually edited)`}
                .
              </div>
            )}
            {syncWhoopNow.data?.status === "ERROR" && (
              <div className="mt-1.5 rounded-md border border-hairline bg-page px-3 py-2 text-xs text-status-critical">
                Sync failed: {syncWhoopNow.data.errorMessage}
              </div>
            )}
            {!whoop?.configured && !whoop?.connected && (
              <p className="mt-1.5 text-xs text-ink-muted">
                Not configured yet — set WHOOP_CLIENT_ID/SECRET in backend/.env.
              </p>
            )}
          </div>

          <div>
            <div className="font-medium text-ink-primary">Health Connect (Android)</div>
            <div className="text-xs text-ink-secondary">Steps, weight, workouts and sleep from your phone</div>
            <div className="mt-1.5 text-xs text-ink-secondary">
              {healthConnect?.connected
                ? `Connected — last synced ${healthConnect.lastSyncAt ? new Date(healthConnect.lastSyncAt).toLocaleString() : "just now"}`
                : "Not connected yet — sign into the Momentum Android app with the same Google account"}
            </div>
          </div>

          <p className="text-xs text-ink-muted">
            MyFitnessPal sync isn't available in this build (their API is closed to new developers) — log meals
            manually via Progress.
          </p>
        </div>
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
