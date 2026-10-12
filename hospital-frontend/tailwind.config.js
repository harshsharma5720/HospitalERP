/** @type {import('tailwindcss').Config} */
const defaultTheme = require("tailwindcss/defaultTheme");

// A colour from the design tokens in src/index.css (docs/UI_REFRESH_PLAN.md), with Tailwind's opacity modifiers
const token = (name) => `rgb(var(--${name}) / <alpha-value>)`;

module.exports = {
  darkMode: "class", // the "dark" class on <html> (DarkThemeToggle)

  content: ["./src/**/*.{js,jsx,ts,tsx}"],

  theme: {
    extend: {
      colors: {
        canvas: token("canvas"),
        surface: { DEFAULT: token("surface"), 2: token("surface-2") },
        line: token("line"),
        ink: { DEFAULT: token("ink"), muted: token("ink-muted") },
        primary: { DEFAULT: token("primary"), hover: token("primary-hover"), ink: token("primary-ink") },
        success: { DEFAULT: token("success"), hover: token("success-hover"), ink: token("success-ink") },
        warning: { DEFAULT: token("warning"), ink: token("warning-ink") },
        danger: { DEFAULT: token("danger"), hover: token("danger-hover"), ink: token("danger-ink") },
        info: { DEFAULT: token("info"), ink: token("info-ink") },
      },
      fontFamily: {
        sans: ["Inter", ...defaultTheme.fontFamily.sans],
      },
      boxShadow: {
        card: "0 1px 2px rgb(15 23 42 / 0.04), 0 1px 3px rgb(15 23 42 / 0.06)",
        overlay: "0 10px 30px rgb(15 23 42 / 0.18)",
      },
    },
  },

  plugins: [],
};
