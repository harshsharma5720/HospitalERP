import React from "react";
import useAppStore from "./useAppStore";

// The light / dark switch of the public site (the staff portals have their own in StaffLayout)
export default function DarkThemeToggle() {
  const darkMode = useAppStore((s) => s.darkMode);
  const toggleDarkMode = useAppStore((s) => s.toggleDarkMode);

  return (
    <button
      onClick={toggleDarkMode}
      className="
        gap-2 px-3 py-1 rounded-md transition-all duration-200 shadow-md
        bg-white dark:bg-gray-700
        text-black dark:text-white
        transition
      "
    >
      {darkMode ? "🌙 Dark" : "☀ Light"}
    </button>
  );
}
