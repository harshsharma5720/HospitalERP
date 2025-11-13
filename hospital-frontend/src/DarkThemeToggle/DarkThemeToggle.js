import React, { useEffect } from "react";
import useAppStore from "../Store/useAppStore";


export default function DarkThemeToggle() {
  const { darkMode } = useAppStore();

  // Load theme on first render
  useEffect(() => {
    const savedTheme = localStorage.getItem("theme");

    const isDark = savedTheme === "dark";
    useAppStore.setState({ darkMode: isDark });

    if (isDark) {
      document.documentElement.classList.add("dark");
    } else {
      document.documentElement.classList.remove("dark");
    }
  }, []);

  // Toggle theme & update Zustand + localStorage + HTML class
  const handleClick = () => {
    useAppStore.setState((state) => {
      const newMode = !state.darkMode;

      if (newMode) {
        document.documentElement.classList.add("dark");
        localStorage.setItem("theme", "dark");
      } else {
        document.documentElement.classList.remove("dark");
        localStorage.setItem("theme", "light");
      }

      return { darkMode: newMode };
    });
  };

  return (
    <button
      onClick={handleClick}
      className="
        px-4 py-2 rounded-lg shadow-md
        bg-gray-200 dark:bg-gray-700
        text-black dark:text-white
        transition
      "
    >
      {darkMode ? "🌙 Dark" : "☀️ Light"}
    </button>
  );
}
