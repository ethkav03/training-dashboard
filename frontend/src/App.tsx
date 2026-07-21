import { Navigate, Route, Routes } from "react-router-dom";
import { AppShell } from "./components/layout/AppShell.js";
import { ProtectedRoute } from "./components/layout/ProtectedRoute.js";
import { LoginPage } from "./pages/LoginPage.js";
import { AuthCallbackPage } from "./pages/AuthCallbackPage.js";
import { OnboardingPage } from "./pages/OnboardingPage.js";
import { TodayPage } from "./pages/TodayPage.js";
import { ProgressPage } from "./pages/ProgressPage.js";
import { TrainingPage } from "./pages/TrainingPage.js";
import { ExerciseProgressionPage } from "./pages/training/ExerciseProgressionPage.js";
import { GoalsPage } from "./pages/GoalsPage.js";
import { InsightsPage } from "./pages/InsightsPage.js";
import { TimelinePage } from "./pages/TimelinePage.js";
import { SettingsPage } from "./pages/SettingsPage.js";

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/auth/callback" element={<AuthCallbackPage />} />
      <Route path="/onboarding" element={<OnboardingPage />} />

      <Route
        path="/*"
        element={
          <ProtectedRoute>
            <AppShell>
              <Routes>
                <Route path="today" element={<TodayPage />} />
                <Route path="progress/*" element={<ProgressPage />} />
                <Route path="training" element={<TrainingPage />} />
                <Route path="training/exercises/:name" element={<ExerciseProgressionPage />} />
                <Route path="goals" element={<GoalsPage />} />
                <Route path="insights" element={<InsightsPage />} />
                <Route path="timeline" element={<TimelinePage />} />
                <Route path="settings" element={<SettingsPage />} />
                <Route path="*" element={<Navigate to="/today" replace />} />
              </Routes>
            </AppShell>
          </ProtectedRoute>
        }
      />
    </Routes>
  );
}
