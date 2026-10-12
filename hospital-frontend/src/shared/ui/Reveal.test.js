import { render, screen } from "@testing-library/react";
import Reveal from "./Reveal";

afterEach(() => {
  delete window.IntersectionObserver;
});

test("with a browser that watches scrolling, the content starts hidden and lower, to fade in", () => {
  window.IntersectionObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  };
  render(<Reveal as="li">Cardiology</Reveal>);
  const item = screen.getByText("Cardiology");
  expect(item.tagName).toBe("LI");
  expect(item).toHaveStyle("opacity: 0");
});

test("without one, the content is simply shown", () => {
  render(<Reveal className="card">Cardiology</Reveal>);
  const item = screen.getByText("Cardiology");
  expect(item).toHaveClass("card");
  expect(item).not.toHaveStyle("opacity: 0");
});
