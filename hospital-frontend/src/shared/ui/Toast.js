import React, { createContext, useCallback, useContext, useMemo, useRef, useState } from "react";
import { CheckCircle2, AlertTriangle, Info, X } from "lucide-react";

// Toasts (docs/UI_REFRESH_PLAN.md): short messages in the corner instead of the browser's alert(). They go away by
// themselves (errors stay longer) or with ×. useToast() gives { success, error, info }; outside a ToastProvider
// (e.g. a unit test without one) they do nothing.

const ToastContext = createContext(null);
const NO_TOASTS = { success: () => {}, error: () => {}, info: () => {} };

const STYLES = {
  success: { icon: CheckCircle2, accent: "text-success-ink", role: "status" },
  error: { icon: AlertTriangle, accent: "text-danger-ink", role: "alert" },
  info: { icon: Info, accent: "text-info-ink", role: "status" },
};

export function ToastProvider({ children, duration = 5000 }) {
  const [toasts, setToasts] = useState([]);
  const nextId = useRef(1);

  const dismiss = useCallback((id) => setToasts((current) => current.filter((t) => t.id !== id)), []);

  const show = useCallback(
    (kind, message) => {
      const id = nextId.current++;
      setToasts((current) => [...current, { id, kind, message }]);
      setTimeout(() => dismiss(id), kind === "error" ? duration * 2 : duration);
    },
    [dismiss, duration]
  );

  const api = useMemo(
    () => ({
      success: (message) => show("success", message),
      error: (message) => show("error", message),
      info: (message) => show("info", message),
    }),
    [show]
  );

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="fixed bottom-4 right-4 z-[60] flex w-[min(24rem,calc(100vw-2rem))] flex-col gap-2" aria-live="polite">
        {toasts.map((toast) => {
          const { icon: Icon, accent, role } = STYLES[toast.kind];
          return (
            <div
              key={toast.id}
              role={role}
              className="flex items-start gap-3 rounded-xl border border-line bg-surface p-4 text-sm text-ink shadow-overlay"
            >
              <Icon size={18} className={`mt-0.5 shrink-0 ${accent}`} aria-hidden="true" />
              <p className="flex-1">{toast.message}</p>
              <button
                type="button"
                onClick={() => dismiss(toast.id)}
                aria-label="Close"
                className="rounded text-ink-muted hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40"
              >
                <X size={16} aria-hidden="true" />
              </button>
            </div>
          );
        })}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  return useContext(ToastContext) ?? NO_TOASTS;
}
