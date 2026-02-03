import React from "react";
import { Loader2 } from "lucide-react";

export default function Button({
  children,
  onClick,
  type = "button",
  variant = "primary",
  loading = false,
  disabled = false,
  className = "",
}) {
  const baseStyles =
    "px-6 py-2 rounded-lg font-semibold transition-all flex items-center justify-center gap-2";

  const variants = {
    primary:
      "bg-gradient-to-r from-[#007B9E] to-[#00A2B8] text-white hover:opacity-90",
    secondary:
      "bg-gray-200 text-gray-800 hover:bg-gray-300",
    danger:
      "bg-red-600 text-white hover:bg-red-700",
  };

  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled || loading}
      className={`
        ${baseStyles}
        ${variants[variant]}
        ${(disabled || loading) && "opacity-60 cursor-not-allowed"}
        ${className}
      `}
    >
      {loading && <Loader2 className="animate-spin w-4 h-4" />}
      {children}
    </button>
  );
}
