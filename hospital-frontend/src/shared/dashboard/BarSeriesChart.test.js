import { render } from "@testing-library/react";
import BarSeriesChart from "./BarSeriesChart";

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

const DATA = [
  { date: "2026-10-14", averageMinutes: 12.5 },
  { date: "2026-10-15", averageMinutes: null }, // nothing measured: no bar
  { date: "2026-10-16", averageMinutes: 30 },
];

test("one bar per value in the single-series color, none for a missing value, named for screen readers", () => {
  const { container, getByRole } = render(
    <BarSeriesChart
      data={DATA}
      xKey="date"
      yKey="averageMinutes"
      xTick={(d) => d.slice(5)}
      tooltipTitle={(d) => d}
      valueText={(row) => `${row.averageMinutes}`}
      label="Average waiting minutes per day"
      allowDecimals
    />
  );
  expect(getByRole("img", { name: "Average waiting minutes per day" })).toBeInTheDocument();
  expect(container.querySelector(".recharts-bar.series-single")).not.toBeNull();
  expect(container.querySelectorAll(".recharts-bar-rectangle path")).toHaveLength(2);
  // The axis labels go through xTick (jsdom can't measure text, so recharts thins them out: the screenshots show them)
  const ticks = [...container.querySelectorAll(".recharts-cartesian-axis-tick-value")].map((t) => t.textContent);
  expect(ticks.some((t) => /^10-1[4-6]$/.test(t))).toBe(true);
  expect(ticks).not.toContain("2026-10-16");
});

const MONTH = Array.from({ length: 30 }, (_, i) => `2026-09-${String(i + 1).padStart(2, "0")}`);

test("30 bars on a 600 px chart: as many labels as fit, evenly; a narrower label fits more", () => {
  const data = MONTH.map((date) => ({ date, value: 1 }));
  const labels = (labelWidth) => {
    const { container, unmount } = render(
      <BarSeriesChart data={data} xKey="date" yKey="value" xTick={(d) => d.slice(5)} tooltipTitle={(d) => d} valueText={() => ""} label="x" labelWidth={labelWidth} />
    );
    const shown = [...container.querySelectorAll(".recharts-cartesian-axis-tick-value")].map((t) => t.textContent).filter((t) => t.startsWith("09-"));
    unmount();
    return shown;
  };
  expect(labels(80)).toEqual(["09-01", "09-06", "09-11", "09-16", "09-21", "09-26"]);
  expect(labels(40)).toEqual(["09-01", "09-04", "09-07", "09-10", "09-13", "09-16", "09-19", "09-22", "09-25", "09-28"]); // 14 fit: every 3rd
});

