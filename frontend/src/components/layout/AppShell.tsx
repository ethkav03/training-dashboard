import type { ReactNode } from "react";
import { TopNav } from "./TopNav.js";
import { BottomNav } from "./BottomNav.js";

export function AppShell({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-screen bg-page">
      <TopNav />
      <main className="mx-auto max-w-6xl px-4 py-6 pb-20 md:pb-6">{children}</main>
      <BottomNav />
    </div>
  );
}
