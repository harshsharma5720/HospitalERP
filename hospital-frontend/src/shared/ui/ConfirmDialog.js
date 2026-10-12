import React, { createContext, useCallback, useContext, useEffect, useRef, useState } from "react";
import Button from "./Button";

// A confirmation dialog instead of the browser's confirm() (docs/UI_REFRESH_PLAN.md):
//   const confirm = useConfirm();
//   if (await confirm({ title: "Deactivate Asha?", message: "...", confirmLabel: "Deactivate", tone: "danger" })) ...
// Escape or Cancel answer false. Outside a ConfirmProvider it falls back to the browser's confirm().

const ConfirmContext = createContext(null);

export function ConfirmProvider({ children }) {
  const [request, setRequest] = useState(null); // { title, message, confirmLabel, cancelLabel, tone, resolve }

  const confirm = useCallback(
    (options) => new Promise((resolve) => setRequest({ confirmLabel: "Confirm", cancelLabel: "Cancel", ...options, resolve })),
    []
  );

  const answer = (value) => {
    request?.resolve(value);
    setRequest(null);
  };

  return (
    <ConfirmContext.Provider value={confirm}>
      {children}
      {request && <Dialog request={request} onAnswer={answer} />}
    </ConfirmContext.Provider>
  );
}

function Dialog({ request, onAnswer }) {
  const confirmButton = useRef(null);

  useEffect(() => {
    confirmButton.current?.focus();
    const onKey = (e) => {
      if (e.key === "Escape") onAnswer(false);
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [onAnswer]);

  return (
    <div className="fixed inset-0 z-[70] flex items-center justify-center bg-black/40 p-4 dark:bg-black/60" onClick={() => onAnswer(false)}>
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="confirm-title"
        aria-describedby={request.message ? "confirm-message" : undefined}
        className="w-full max-w-md rounded-xl border border-line bg-surface p-6 shadow-overlay"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 id="confirm-title" className="text-lg font-semibold text-ink">
          {request.title}
        </h2>
        {request.message && (
          <p id="confirm-message" className="mt-2 text-sm text-ink-muted">
            {request.message}
          </p>
        )}
        <div className="mt-6 flex justify-end gap-2">
          <Button variant="secondary" onClick={() => onAnswer(false)}>
            {request.cancelLabel}
          </Button>
          <Button ref={confirmButton} variant={request.tone === "danger" ? "danger" : "primary"} onClick={() => onAnswer(true)}>
            {request.confirmLabel}
          </Button>
        </div>
      </div>
    </div>
  );
}

export function useConfirm() {
  const confirm = useContext(ConfirmContext);
  return confirm ?? ((options) => Promise.resolve(window.confirm(options.message ? `${options.title}\n\n${options.message}` : options.title)));
}
