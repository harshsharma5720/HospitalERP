import React, { useCallback, useEffect, useId, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Bell } from "lucide-react";
import useAuthStore from "../auth/useAuthStore";
import { badgeLabel, whenLabel } from "./inboxFormat";
import * as inboxApi from "./api";

// The notification centre's bell (docs/NOTIFICATION_CENTRE_PLAN.md), in the staff top bar and the patients' header:
// the number unread, asked for every 30 seconds while logged in and at once when the tab is shown again; a panel with
// the newest messages, unread ones marked, Mark all as read; a message opens its page and is marked read.

export const POLL_MS = 30 * 1000;

export default function InboxBell({ className = "" }) {
  const token = useAuthStore((s) => s.token);
  const navigate = useNavigate();
  const panelId = useId();
  const box = useRef(null);
  const [unread, setUnread] = useState(0);
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState(null); // null while loading
  const [error, setError] = useState(false);

  const refreshCount = useCallback(() => {
    inboxApi
      .getUnreadCount()
      .then((res) => setUnread(res.data.unread))
      .catch(() => {}); // the next poll tries again
  }, []);

  // The count: now, every 30 seconds, and when the tab is shown again
  useEffect(() => {
    if (!token) return undefined;
    refreshCount();
    const timer = setInterval(refreshCount, POLL_MS);
    const onVisible = () => document.visibilityState === "visible" && refreshCount();
    document.addEventListener("visibilitychange", onVisible);
    return () => {
      clearInterval(timer);
      document.removeEventListener("visibilitychange", onVisible);
    };
  }, [token, refreshCount]);

  // The panel loads when it opens
  useEffect(() => {
    if (!open) return undefined;
    let current = true;
    setMessages(null);
    setError(false);
    inboxApi
      .getInbox()
      .then((res) => {
        if (!current) return;
        setMessages(res.data.messages);
        setUnread(res.data.unread);
      })
      .catch(() => current && setError(true));
    return () => {
      current = false;
    };
  }, [open]);

  // Escape and a click outside close it
  useEffect(() => {
    if (!open) return undefined;
    const onKey = (e) => e.key === "Escape" && setOpen(false);
    const onClick = (e) => box.current && !box.current.contains(e.target) && setOpen(false);
    document.addEventListener("keydown", onKey);
    document.addEventListener("mousedown", onClick);
    return () => {
      document.removeEventListener("keydown", onKey);
      document.removeEventListener("mousedown", onClick);
    };
  }, [open]);

  if (!token) return null;

  const openMessage = (message) => {
    if (!message.read) {
      setMessages((list) => list.map((m) => (m.id === message.id ? { ...m, read: true } : m)));
      setUnread((n) => Math.max(0, n - 1));
      inboxApi.markRead(message.id).catch(() => {});
    }
    setOpen(false);
    if (message.link) navigate(message.link);
  };

  const readAll = () => {
    setMessages((list) => (list || []).map((m) => ({ ...m, read: true })));
    setUnread(0);
    inboxApi.markAllRead().catch(refreshCount);
  };

  return (
    <div ref={box} className={`relative ${className}`}>
      <button
        type="button"
        onClick={() => setOpen(!open)}
        aria-expanded={open}
        aria-controls={panelId}
        aria-label={unread > 0 ? `Notifications, ${unread} unread` : "Notifications"}
        className="relative rounded-lg p-2 text-ink-muted hover:bg-surface-2 hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40"
      >
        <Bell size={18} aria-hidden="true" />
        {unread > 0 && (
          <span
            aria-hidden="true"
            className="absolute -right-0.5 -top-0.5 flex h-[18px] min-w-[18px] items-center justify-center rounded-full bg-danger px-1 text-[11px] font-semibold leading-none text-white ring-2 ring-surface"
          >
            {badgeLabel(unread)}
          </span>
        )}
      </button>

      {open && (
        <div
          id={panelId}
          role="dialog"
          aria-label="Notifications"
          className="fixed inset-x-4 top-[4.5rem] z-50 overflow-hidden rounded-xl border border-line bg-surface shadow-overlay sm:absolute sm:inset-x-auto sm:right-0 sm:top-full sm:mt-2 sm:w-[22rem]"
        >
          <div className="flex items-center justify-between gap-2 border-b border-line px-4 py-3">
            <h2 className="text-sm font-semibold text-ink">Notifications</h2>
            <button
              type="button"
              onClick={readAll}
              disabled={unread === 0}
              className="rounded text-sm font-medium text-primary-ink hover:underline disabled:cursor-default disabled:text-ink-muted disabled:no-underline"
            >
              Mark all as read
            </button>
          </div>
          {/* On a phone the panel spans the screen under the header; from sm up it hangs from the bell */}
          <div className="max-h-[70vh] overflow-y-auto">
            {error && <p className="px-4 py-6 text-sm text-ink-muted">The notifications couldn't be loaded. Please try again.</p>}
            {!error && messages === null && <p className="px-4 py-6 text-sm text-ink-muted">Loading...</p>}
            {messages?.length === 0 && <p className="px-4 py-6 text-sm text-ink-muted">No notifications yet.</p>}
            {messages?.length > 0 && (
              <ul className="divide-y divide-line">
                {messages.map((message) => (
                  <li key={message.id}>
                    <button
                      type="button"
                      onClick={() => openMessage(message)}
                      className={`flex w-full gap-3 px-4 py-3 text-left hover:bg-surface-2 focus-visible:bg-surface-2 focus-visible:outline-none ${
                        message.read ? "" : "bg-primary-ink/5"
                      }`}
                    >
                      <span
                        aria-hidden="true"
                        className={`mt-1.5 h-2 w-2 shrink-0 rounded-full ${message.read ? "bg-transparent" : "bg-primary"}`}
                      />
                      <span className="min-w-0">
                        <span className={`block text-sm ${message.read ? "text-ink" : "font-semibold text-ink"}`}>
                          {!message.read && <span className="sr-only">New: </span>}
                          {message.title}
                        </span>
                        <span className="mt-0.5 block text-sm text-ink-muted">{message.body}</span>
                        <span className="mt-1 block text-xs text-ink-muted">{whenLabel(message.createdAt)}</span>
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
