import React from "react";

// The app's buttons (docs/UI_REFRESH_PLAN.md). variant: primary (the main action), secondary, danger, success, ghost,
// danger-ghost (a quiet destructive action in a row);
// size: sm, md, lg. as={Link} (with to=...) renders a link that looks like a button.
const VARIANTS = {
  primary: "bg-primary text-white hover:bg-primary-hover shadow-sm",
  secondary: "bg-surface text-ink border border-line hover:bg-surface-2",
  danger: "bg-danger text-white hover:bg-danger-hover shadow-sm",
  success: "bg-success text-white hover:bg-success-hover shadow-sm",
  ghost: "text-ink-muted hover:text-ink hover:bg-surface-2",
  "danger-ghost": "text-danger-ink hover:bg-danger-ink/10",
};

const SIZES = {
  sm: "h-8 px-3 text-sm",
  md: "h-10 px-4 text-sm",
  lg: "h-11 px-5 text-base",
};

export const buttonClasses = (variant = "primary", size = "md", className = "") =>
  "inline-flex items-center justify-center gap-2 rounded-lg font-medium whitespace-nowrap transition-colors " +
  "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40 focus-visible:ring-offset-1 " +
  "focus-visible:ring-offset-surface disabled:opacity-50 disabled:pointer-events-none " +
  `${VARIANTS[variant] ?? VARIANTS.primary} ${SIZES[size] ?? SIZES.md} ${className}`;

export default function Button({ as: Component = "button", variant, size, className, type, ...props }) {
  const isButton = Component === "button";
  return (
    <Component
      {...(isButton ? { type: type ?? "button" } : {})}
      className={buttonClasses(variant, size, className)}
      {...props}
    />
  );
}
