import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { CalendarCheck, HeartPulse } from "lucide-react";
import { Button, Modal } from "./ui";
import { HOSPITAL } from "./hospitalInfo";
import useAuthStore from "../features/auth/useAuthStore";

const SEEN_KEY = "welcomeSeen";

const seenThisVisit = () => {
  try {
    return sessionStorage.getItem(SEEN_KEY) === "1";
  } catch (e) {
    return false;
  }
};

const markSeen = () => {
  try {
    sessionStorage.setItem(SEEN_KEY, "1");
  } catch (e) {
    // storage unavailable: it may show again on the next page
  }
};

// The welcome to visitors who aren't logged in: log in, or create an account (docs/UI_REFRESH_PLAN.md, U.6).
// On its own (the home page) it comes up after a moment, once per visit. With onClose (the doctors page, when a
// visitor tries to book) it shows at once and the page decides when it goes.
export default function PopupForm({ onClose, delay = 2000 }) {
  const onDemand = Boolean(onClose);
  const [open, setOpen] = useState(onDemand);
  const loggedIn = useAuthStore((s) => Boolean(s.token));

  useEffect(() => {
    if (onDemand || loggedIn || seenThisVisit()) return undefined;
    const timer = setTimeout(() => setOpen(true), delay);
    return () => clearTimeout(timer);
  }, [onDemand, delay, loggedIn]);

  if (!open) return null;

  const close = () => {
    markSeen();
    setOpen(false);
    onClose?.();
  };

  return (
    <Modal
      size="sm"
      onClose={close}
      title={
        <span className="flex items-center gap-3">
          <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary text-white">
            <HeartPulse size={20} aria-hidden="true" />
          </span>
          Welcome to {HOSPITAL.name}
        </span>
      }
      description={onDemand ? "Log in to book this appointment." : "Log in to book and follow your appointments, or create an account; it takes a minute."}
    >
      <div className="space-y-3">
        <Button as={Link} to="/login" onClick={markSeen} size="lg" className="w-full">
          Log in
        </Button>
        <Button as={Link} to="/register" onClick={markSeen} variant="secondary" size="lg" className="w-full">
          Create an account
        </Button>
        <p className="flex items-center justify-center gap-2 pt-1 text-xs text-ink-muted">
          <CalendarCheck size={14} aria-hidden="true" />
          SMS confirmation for every booking
        </p>
      </div>
    </Modal>
  );
}
