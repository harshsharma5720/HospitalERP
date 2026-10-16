import { tickInterval } from "./dashboardFormat";

test("as many axis labels as fit the chart's width, evenly", () => {
  expect(tickInterval(30, 900)).toBe(2); // 10 fit: every 3rd day
  expect(tickInterval(90, 900)).toBe(8); // every 9th
  expect(tickInterval(30, 300)).toBe(9); // a phone: 3 fit
  expect(tickInterval(7, 300, 36)).toBe(0); // the weekdays all fit
  expect(tickInterval(9, 300, 44)).toBe(1); // hours on a phone: every other one
});

test("before the chart is measured: every label up to 10 bars, then about 8 of them", () => {
  expect(tickInterval(10)).toBe(0);
  expect(tickInterval(30)).toBe(3);
});
