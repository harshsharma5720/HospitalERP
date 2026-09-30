import { create } from "zustand";

const useAppStore = create((set) => ({
  darkMode: false,

  toggleDarkMode: () =>
    set((state) => ({
      darkMode: !state.darkMode,
    })),
}));

export default useAppStore;
