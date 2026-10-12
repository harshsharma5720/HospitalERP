import { render, screen } from "@testing-library/react";
import Reveal from "./Reveal";

// Its own file: framer-motion reads the reduced-motion preference once per page
test("people who ask for reduced motion get the content without the movement", () => {
  window.IntersectionObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  };
  window.matchMedia = (query) => ({
    matches: query.includes("prefers-reduced-motion"),
    media: query,
    addListener() {},
    removeListener() {},
    addEventListener() {},
    removeEventListener() {},
  });
  render(<Reveal>Cardiology</Reveal>);
  expect(screen.getByText("Cardiology")).not.toHaveStyle("opacity: 0");
  delete window.IntersectionObserver;
  delete window.matchMedia;
});
