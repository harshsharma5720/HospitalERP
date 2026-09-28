import React from "react";
import { CheckCircle } from "lucide-react";

export default function Loader({
  type = "page",
  text = "Loading...",
}) {

  /* ✅ SUCCESS LOADER */
  if (type === "success") {
    return (
      <div className="fixed inset-0 z-50 flex flex-col items-center justify-center bg-black/40 backdrop-blur-sm">
        <div className="bg-white dark:bg-[#0a1124] p-6 rounded-xl shadow-lg flex flex-col items-center gap-3">
          <CheckCircle className="w-12 h-12 text-green-500" />
          <p className="text-lg font-semibold text-green-600 text-center">
            {text}
          </p>
        </div>
      </div>
    );
  }

  /* ✅ HEARTBEAT / BOOKING LOADER */
  if (type === "heartbeat") {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm">
        <div className="bg-white dark:bg-[#0a1124] p-6 rounded-xl shadow-lg flex flex-col items-center gap-4">
          <div className="flex gap-2">
            <span className="w-3 h-3 bg-[#007B9E] rounded-full animate-pulse"></span>
            <span className="w-3 h-3 bg-[#007B9E] rounded-full animate-pulse delay-150"></span>
            <span className="w-3 h-3 bg-[#007B9E] rounded-full animate-pulse delay-300"></span>
          </div>
          <p className="text-sm text-gray-700 dark:text-gray-300 text-center">
            {text}
          </p>
        </div>
      </div>
    );
  }

  /* ✅ INLINE LOADER */
  if (type === "inline") {
    return (
      <div className="flex items-center gap-2">
        <span className="w-4 h-4 border-2 border-blue-500 border-t-transparent rounded-full animate-spin"></span>
        <span className="text-sm text-gray-600 dark:text-gray-300">
          {text}
        </span>
      </div>
    );
  }

  /* ✅ PAGE LOADER (DEFAULT) */
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-[#f6f9ff] dark:bg-[#0a1124]">
      <span className="w-12 h-12 border-4 border-[#007B9E] border-t-transparent rounded-full animate-spin"></span>
      <p className="mt-4 text-gray-600 dark:text-gray-300">
        {text}
      </p>
    </div>
  );
}
