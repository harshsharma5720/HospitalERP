import { create } from "zustand";
import { applyDarkMode, savedDarkMode } from "./theme";

const useAppStore = create((set) => ({
  darkMode: savedDarkMode(),

  toggleDarkMode: () =>
    set((state) => {
      const darkMode = !state.darkMode;
      applyDarkMode(darkMode);
      return { darkMode };
    }),
}));

export default useAppStore;
