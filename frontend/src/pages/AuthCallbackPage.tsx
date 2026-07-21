import { useEffect, useRef } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../context/AuthContext.js";

export function AuthCallbackPage() {
  const [params] = useSearchParams();
  const { setToken } = useAuth();
  const navigate = useNavigate();
  const handled = useRef(false);

  useEffect(() => {
    if (handled.current) return;
    handled.current = true;
    const token = params.get("token");
    if (!token) {
      navigate("/login", { replace: true });
      return;
    }
    setToken(token).then(() => navigate("/today", { replace: true }));
  }, [params, setToken, navigate]);

  return (
    <div className="flex h-screen items-center justify-center text-ink-muted">
      Signing you in...
    </div>
  );
}
