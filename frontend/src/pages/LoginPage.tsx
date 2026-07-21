import { useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import { googleLoginUrl, devLogin, getAuthStatus } from "../api/auth.js";
import { useAuth } from "../context/AuthContext.js";
import { Button } from "../components/ui/Button.js";
import { Card } from "../components/ui/Card.js";

export function LoginPage() {
  const { isAuthenticated, isLoading, setToken } = useAuth();
  const [googleConfigured, setGoogleConfigured] = useState<boolean | null>(null);
  const [devLoggingIn, setDevLoggingIn] = useState(false);

  useEffect(() => {
    getAuthStatus()
      .then((s) => setGoogleConfigured(s.googleOAuthConfigured))
      .catch(() => setGoogleConfigured(false));
  }, []);

  if (!isLoading && isAuthenticated) {
    return <Navigate to="/today" replace />;
  }

  async function handleDevLogin() {
    setDevLoggingIn(true);
    try {
      const { token } = await devLogin();
      await setToken(token);
    } finally {
      setDevLoggingIn(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-page px-4">
      <Card className="w-full max-w-sm text-center">
        <h1 className="text-2xl font-semibold tracking-tight">Momentum</h1>
        <p className="mt-1 text-sm text-ink-secondary">See your progress. Build your momentum.</p>

        <div className="mt-8 flex flex-col gap-3">
          <Button
            variant="primary"
            disabled={!googleConfigured}
            onClick={() => {
              window.location.href = googleLoginUrl();
            }}
          >
            Continue with Google
          </Button>
          {googleConfigured === false && (
            <p className="text-xs text-ink-muted">
              Google OAuth isn't configured yet on the backend (missing GOOGLE_CLIENT_ID/SECRET).
              Use the dev login below, or see backend/.env.example.
            </p>
          )}
          <Button variant="secondary" disabled={devLoggingIn} onClick={handleDevLogin}>
            {devLoggingIn ? "Signing in..." : "Continue as Dev User (local only)"}
          </Button>
        </div>
      </Card>
    </div>
  );
}
