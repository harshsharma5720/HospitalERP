import React from "react";
import { Moon, Sun } from "lucide-react";
import useAppStore from "../useAppStore";

// The light / dark switch (an icon button), for the staff layout and the public header
export default function ThemeButton({ className = "" }) {
  const darkMode = useAppStore((s) => s.darkMode);
  const toggleDarkMode = useAppStore((s) => s.toggleDarkMode);
  const label = darkMode ? "Switch to light mode" : "Switch to dark mode";
  const Icon = darkMode ? Sun : Moon;
  return (
    <button
      type="button"
      onClick={toggleDarkMode}
      aria-label={label}
      title={label}
      className={`rounded-lg p-2 text-ink-muted hover:bg-surface-2 hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40 ${className}`}
    >
      <Icon size={18} aria-hidden="true" />
    </button>
  );
}
