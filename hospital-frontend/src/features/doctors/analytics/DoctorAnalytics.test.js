import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import DoctorAnalytics from "./DoctorAnalytics";
import * as analyticsApi from "./api";

jest.mock("./api");
// The charts are tested on their own (AppointmentTrendChart.test.js, BarSeriesChart.test.js); here they show what they get
jest.mock("../../../shared/dashboard/AppointmentTrendChart", () => {
  const actual = jest.requireActual("../../../shared/dashboard/AppointmentTrendChart");
  const React = require("react");
  return { __esModule: true, SERIES: actual.SERIES, default: ({ trend }) => React.createElement("div", { "data-testid": "trend-chart", "data-days": trend.length }) };
});
jest.mock("../../../shared/dashboard/BarSeriesChart", () => {
  const React = require("react");
  return {
    __esModule: true,
    default: ({ data, label, xKey, xTick, tooltipTitle, valueText }) =>
      React.createElement(
        "ul",
        { "aria-label": label },
        data.map((row, i) => React.createElement("li", { key: i }, `${xTick(row[xKey])} | ${tooltipTitle(row[xKey])} | ${valueText(row)}`))
      ),
  };
});

const days = (n, make) => Array.from({ length: n }, (_, i) => make(`2026-10-${String(10 + i).padStart(2, "0")}`, i));

const VISITS = {
  period: { days: 7, from: "2026-10-10", to: "2026-10-16" },
  doctor: { id: 7, name: "Anita Rao", specialist: "CARDIOLOGY" },
  visits: { total: 4, completed: 1, missed: 1, upcoming: 1, cancelled: 1, cancellationRate: 25.0, noShowRate: 50.0 },
  perDay: days(7, (date, i) => ({ date, completed: i === 5 ? 1 : 0, missed: i === 4 ? 1 : 0, upcoming: i === 6 ? 1 : 0, cancelled: i === 3 ? 1 : 0 })),
  byHour: [
    { hour: 10, visits: 1 },
    { hour: 11, visits: 0 },
    { hour: 16, visits: 2 },
  ],
  byWeekday: ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"].map((day, i) => ({ day, visits: i === 3 ? 2 : 0 })),
  waiting: {
    measured: 2, averageMinutes: 22.5, longestMinutes: 30,
    perDay: days(7, (date, i) => ({ date, measured: i >= 5 ? 1 : 0, averageMinutes: i === 5 ? 15.0 : i === 6 ? 30.0 : null })),
  },
};
const EARNINGS = {
  period: { days: 7, from: "2026-10-10", to: "2026-10-16" },
  earned: 550.0, paidVisits: 1, averagePerVisit: 550.0, refunds: 500.0, refundedVisits: 1, unpaid: 500.0, unpaidVisits: 1,
  perDay: days(7, (date, i) => ({ date, earned: i === 5 ? 550.0 : 0 })),
};

const tile = (label) => screen.getByText(label, { selector: "p" }).parentElement;

beforeEach(() => {
  analyticsApi.getAnalytics.mockResolvedValue({ data: VISITS });
  analyticsApi.getEarnings.mockResolvedValue({ data: EARNINGS });
});
afterEach(() => jest.resetAllMocks());

test("the logged-in doctor's figures: tiles, what each chart shows, and the numbers", async () => {
  render(<DoctorAnalytics />);
  expect(await screen.findByRole("heading", { name: "Last 7 days · 10 Oct 2026 – 16 Oct 2026" })).toBeInTheDocument();
  expect(analyticsApi.getAnalytics).toHaveBeenCalledWith(30, undefined);
  expect(analyticsApi.getEarnings).toHaveBeenCalledWith(30, undefined);

  expect(tile("Visits")).toHaveTextContent("Visits41 completed · 1 upcoming");
  expect(tile("No-shows")).toHaveTextContent("No-shows50%1 visit missed");
  expect(tile("Cancelled")).toHaveTextContent("Cancelled25%1 visit");
  expect(tile("Average wait")).toHaveTextContent("Average wait22.5 min2 visits measured · longest 30 min");
  expect(tile("Earned")).toHaveTextContent("Earned₹5501 paid visit · ₹550.00 each");

  expect(screen.getByTestId("trend-chart")).toHaveAttribute("data-days", "7");
  const perDay = screen.getByRole("region", { name: "Visits per day" });
  expect(within(perDay).getByRole("list", { name: "Legend" })).toHaveTextContent("CompletedMissedUpcomingCancelled");
  const dayRows = within(perDay).getAllByRole("row");
  expect(dayRows[0]).toHaveTextContent("DayCompletedMissedUpcomingCancelled");
  expect(dayRows[1]).toHaveTextContent("16 Oct 2026001"); // newest first: today, 1 upcoming
  expect(dayRows[2]).toHaveTextContent("15 Oct 20261000");

  const hours = within(screen.getByRole("region", { name: "Busy hours" })).getByRole("list", { name: "Visits by the hour they start" });
  expect(within(hours).getAllByRole("listitem").map((li) => li.textContent)).toEqual([
    "10:00 | 10:00–11:00 | 1 visit",
    "11:00 | 11:00–12:00 | 0 visits",
    "16:00 | 16:00–17:00 | 2 visits",
  ]);
  const weekdays = within(screen.getByRole("region", { name: "Busy days" })).getAllByRole("listitem");
  expect(weekdays[3]).toHaveTextContent("Thu | Thursday | 2 visits");

  const waiting = screen.getByRole("region", { name: "Waiting time" });
  const waits = within(waiting).getAllByRole("listitem").map((li) => li.textContent);
  expect(waits[0]).toBe("Sat 10 Oct | 10 Oct 2026 | Not measured");
  expect(waits[6]).toBe("Fri 16 Oct | 16 Oct 2026 | 30 min average · 1 visit");

  const earnings = screen.getByRole("region", { name: "Earnings" });
  expect(earnings).toHaveTextContent("Earned₹550.00Refunds₹500.00 (1 visit)Still unpaid₹500.00 (1 visit)");
  expect(within(earnings).getAllByRole("listitem")[5]).toHaveTextContent("Thu 15 Oct | 15 Oct 2026 | ₹550.00");
});

test("another period loads again; an admin's view of a doctor names the doctor", async () => {
  const onDoctor = jest.fn();
  render(<DoctorAnalytics doctorId="7" onDoctor={onDoctor} />);
  await screen.findByRole("heading", { name: /Last 7 days/ });
  expect(analyticsApi.getAnalytics).toHaveBeenCalledWith(30, "7");
  expect(onDoctor).toHaveBeenCalledWith(VISITS.doctor);

  analyticsApi.getAnalytics.mockResolvedValue({ data: { ...VISITS, period: { days: 90, from: "2026-07-19", to: "2026-10-16" } } });
  fireEvent.click(screen.getByRole("button", { name: "90 days" }));
  expect(await screen.findByRole("heading", { name: /Last 90 days · 19 Jul 2026/ })).toBeInTheDocument();
  expect(analyticsApi.getAnalytics).toHaveBeenLastCalledWith(90, "7");
  expect(analyticsApi.getEarnings).toHaveBeenLastCalledWith(90, "7");
  expect(screen.getByRole("button", { name: "90 days" })).toHaveAttribute("aria-pressed", "true");
});

test("a quiet period: no visits, nothing measured, nothing paid", async () => {
  analyticsApi.getAnalytics.mockResolvedValue({
    data: {
      ...VISITS,
      visits: { total: 0, completed: 0, missed: 0, upcoming: 0, cancelled: 0, cancellationRate: 0, noShowRate: 0 },
      byHour: [],
      waiting: { measured: 0, averageMinutes: null, longestMinutes: null, perDay: [] },
    },
  });
  analyticsApi.getEarnings.mockResolvedValue({ data: { ...EARNINGS, earned: 0, paidVisits: 0, averagePerVisit: null } });
  render(<DoctorAnalytics />);
  await screen.findByRole("heading", { name: /Last 7 days/ });
  expect(tile("Average wait")).toHaveTextContent("Average wait—From check-in to being called in");
  expect(tile("Earned")).toHaveTextContent("₹0No paid visits in this period");
  expect(screen.getByRole("region", { name: "Busy hours" })).toHaveTextContent("No visits in this period.");
  expect(screen.getByRole("region", { name: "Waiting time" })).toHaveTextContent("No waits measured in this period.");
});

test("earnings that can't be loaded don't hide the visits; visits that can't be loaded say so", async () => {
  analyticsApi.getEarnings.mockRejectedValue({ response: { data: { message: "Billing is unavailable" } } });
  const { unmount } = render(<DoctorAnalytics />);
  await screen.findByRole("heading", { name: /Last 7 days/ });
  expect(tile("Visits")).toHaveTextContent("4");
  expect(tile("Earned")).toHaveTextContent("Earned—Billing is unavailable");
  expect(within(screen.getByRole("region", { name: "Earnings" })).getByRole("alert")).toHaveTextContent("Billing is unavailable");
  unmount();

  analyticsApi.getAnalytics.mockRejectedValue({ response: { data: { message: "Doctor not found" } } });
  render(<DoctorAnalytics />);
  await waitFor(() => expect(screen.getByRole("alert")).toHaveTextContent("Doctor not found"));
  expect(screen.queryByText("Visits", { selector: "p" })).not.toBeInTheDocument();
});
