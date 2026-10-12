// Light or dark mode: the "dark" class on <html> (src/index.css swaps the colour tokens), remembered in localStorage.
// index.js applies the saved choice before the first render, so every page - public site or staff portal - gets it.
const KEY = "theme";

export const savedDarkMode = () => {
  try {
    return localStorage.getItem(KEY) === "dark";
  } catch (e) {
    return false; // storage unavailable (private mode)
  }
};

export const applyDarkMode = (dark, { remember = true } = {}) => {
  document.documentElement.classList.toggle("dark", dark);
  if (!remember) return;
  try {
    localStorage.setItem(KEY, dark ? "dark" : "light");
  } catch (e) {
    // storage unavailable - the choice lasts until the page is reloaded
  }
};
