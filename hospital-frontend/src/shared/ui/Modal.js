import React, { useEffect, useId, useRef } from "react";
import { X } from "lucide-react";

const SIZES = { sm: "max-w-md", md: "max-w-lg", lg: "max-w-3xl" };

// A dialog over the page for a form or details (docs/UI_REFRESH_PLAN.md). × and Escape close it, and so does a
// click beside it unless a half-filled form shouldn't be lost that way (closeOnBackdrop={false}). It opens with the
// first field focused; the page behind doesn't scroll.
export default function Modal({ title, description, onClose, size = "md", closeOnBackdrop = true, children }) {
  const id = useId();
  const panel = useRef(null);
  const close = useRef(onClose);

  useEffect(() => {
    close.current = onClose;
  });

  useEffect(() => {
    const first = panel.current.querySelector("input, select, textarea, button:not([data-modal-close])");
    (first ?? panel.current).focus();
    const onKey = (e) => {
      if (e.key === "Escape") close.current();
    };
    document.addEventListener("keydown", onKey);
    const overflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = overflow;
    };
  }, []);

  return (
    <div
      className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/40 p-4 dark:bg-black/60 sm:items-center"
      onMouseDown={(e) => {
        if (closeOnBackdrop && e.target === e.currentTarget) close.current();
      }}
    >
      <div
        ref={panel}
        tabIndex={-1}
        role="dialog"
        aria-modal="true"
        aria-labelledby={`${id}-title`}
        aria-describedby={description ? `${id}-description` : undefined}
        className={`my-8 w-full ${SIZES[size] ?? SIZES.md} rounded-xl border border-line bg-surface text-ink shadow-overlay focus:outline-none`}
      >
        <div className="flex items-start justify-between gap-4 border-b border-line px-6 py-4">
          <div className="min-w-0">
            <h2 id={`${id}-title`} className="text-lg font-semibold">
              {title}
            </h2>
            {description && (
              <div id={`${id}-description`} className="mt-0.5 text-sm text-ink-muted">
                {description}
              </div>
            )}
          </div>
          <button
            type="button"
            data-modal-close
            onClick={() => close.current()}
            aria-label="Close"
            className="rounded-lg p-1.5 text-ink-muted hover:bg-surface-2 hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40"
          >
            <X size={18} aria-hidden="true" />
          </button>
        </div>
        <div className="px-6 py-5">{children}</div>
      </div>
    </div>
  );
}
