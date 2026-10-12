import React from "react";
import { Reveal } from "../ui";

// Fades its content in as it scrolls into view (the shared Reveal: no movement for people who ask for reduced motion)
export default function ScrollAnimate({ children }) {
  return <Reveal>{children}</Reveal>;
}
