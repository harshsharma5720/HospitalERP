import React from "react";
import { motion, useReducedMotion } from "framer-motion";

// Fades its content in and up as it scrolls into view, once (docs/UI_REFRESH_PLAN.md, U.6). delay staggers items
// of a row (seconds). People who ask their system for reduced motion get the content without the movement, and so
// does a browser that can't tell what is in view (no IntersectionObserver - framer-motion would fail there).
const canWatchScrolling = () => typeof window !== "undefined" && "IntersectionObserver" in window;

export default function Reveal({ as = "div", delay = 0, className, children, ...props }) {
  const reduce = useReducedMotion();
  if (reduce || !canWatchScrolling()) {
    const Plain = as;
    return (
      <Plain className={className} {...props}>
        {children}
      </Plain>
    );
  }
  const Moving = motion[as] ?? motion.div;
  return (
    <Moving
      className={className}
      initial={{ opacity: 0, y: 24 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, amount: 0.2 }}
      transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1], delay }}
      {...props}
    >
      {children}
    </Moving>
  );
}
