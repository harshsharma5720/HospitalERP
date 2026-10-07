import { render } from "@testing-library/react";
import AppointmentTrendChart from "./AppointmentTrendChart";

// jsdom has no layout: give every element a 600 x 300 box so the chart has room to draw
const SIZE = { width: 600, height: 300 };
let restore;
beforeAll(() => {
  global.ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  };
  const proto = HTMLElement.prototype;
  const saved = ["clientWidth", "clientHeight", "offsetWidth", "offsetHeight"].map((name) => [
    name,
    Object.getOwnPropertyDescriptor(proto, name),
  ]);
  const rect = proto.getBoundingClientRect;
  Object.defineProperty(proto, "clientWidth", { configurable: true, get: () => SIZE.width });
  Object.defineProperty(proto, "offsetWidth", { configurable: true, get: () => SIZE.width });
  Object.defineProperty(proto, "clientHeight", { configurable: true, get: () => SIZE.height });
  Object.defineProperty(proto, "offsetHeight", { configurable: true, get: () => SIZE.height });
  proto.getBoundingClientRect = () => ({ ...SIZE, top: 0, left: 0, right: SIZE.width, bottom: SIZE.height, x: 0, y: 0 });
  restore = () => {
    saved.forEach(([name, descriptor]) => descriptor && Object.defineProperty(proto, name, descriptor));
    proto.getBoundingClientRect = rect;
  };
});
afterAll(() => restore());

const TREND = [
  { date: "2026-10-05", completed: 2, missed: 1, upcoming: 0, cancelled: 1, newPatients: 0 },
  { date: "2026-10-06", completed: 1, missed: 0, upcoming: 3, cancelled: 0, newPatients: 1 },
];

test("draws one stacked column per day; only each day's top segment has the rounded end", () => {
  const { container } = render(<AppointmentTrendChart trend={TREND} />);

  // Every series layer carries its color class (the colors come from AdminDashboard.css)
  ["completed", "missed", "upcoming", "cancelled"].forEach((key) =>
    expect(container.querySelector(`.recharts-bar.series-${key}`)).not.toBeNull()
  );
  // Day 1: completed, missed, cancelled (top) - day 2: completed, upcoming (top); empty segments draw nothing
  expect(container.querySelectorAll(".recharts-bar rect")).toHaveLength(3);
  const tops = container.querySelectorAll(".recharts-bar path");
  expect(tops).toHaveLength(2);
  expect(container.querySelector(".series-cancelled path")).not.toBeNull();
  expect(container.querySelector(".series-upcoming path")).not.toBeNull();
  tops.forEach((top) => expect(top.getAttribute("fill")).toBe("currentColor"));
});
