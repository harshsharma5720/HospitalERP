import { render, screen, fireEvent, waitFor, within, act } from "@testing-library/react";
import AdminDashboard from "./AdminDashboard";
import * as adminApi from "./api";

jest.mock("./api");
// The chart itself is tested in AppointmentTrendChart.test.js
jest.mock("./AppointmentTrendChart", () => {
  const actual = jest.requireActual("./AppointmentTrendChart");
  return {
    __esModule: true,
    SERIES: actual.SERIES,
    default: () => require("react").createElement("div", { "data-testid": "trend-chart" }),
  };
});

const day = (date, completed, missed, upcoming, cancelled, newPatients = 0) =>
  ({ date, completed, missed, upcoming, cancelled, newPatients });

const DASHBOARD = {
  period: { days: 7, from: "2026-09-30", to: "2026-10-06" },
  today: {
    date: "2026-10-06", appointments: 5, completed: 1, upcoming: 3, cancelled: 1,
    doctorsOnLeave: 1, activePatients: 1234, activeDoctors: 5, activeReceptionists: 2,
  },
  trend: [
    day("2026-09-30", 0, 0, 0, 0), day("2026-10-01", 0, 0, 0, 0), day("2026-10-02", 0, 0, 0, 0),
    day("2026-10-03", 0, 0, 0, 0), day("2026-10-04", 2, 1, 0, 1), day("2026-10-05", 1, 0, 0, 1, 2),
    day("2026-10-06", 1, 0, 3, 1, 1),
  ],
  cancellations: {
    appointments: 11, cancelled: 3, cancelledByPatient: 2, cancelledByDoctor: 1, cancellationRate: 27.3,
    missed: 1, missedRate: 25.0,
  },
  newPatients: { count: 3, countedSince: "2026-10-06T09:00:00" },
  busiestSpecializations: [
    { specialist: "CARDIOLOGY", appointments: 4 },
    { specialist: "NOT_ASSIGNED", appointments: 1 },
  ],
  busiestDoctors: [{ doctorId: 7, doctorName: "Dr Rao", specialist: "CARDIOLOGY", appointments: 4, completed: 3 }],
};

const tile = (label) => screen.getByText(label, { selector: "p" }).closest("div");

beforeEach(() => adminApi.getDashboard.mockResolvedValue({ data: DASHBOARD }));
afterEach(() => jest.resetAllMocks());

test("shows today's and the period's real figures - no invented numbers", async () => {
  render(<AdminDashboard />);

  expect(await screen.findByText("Today · 6 Oct 2026")).toBeInTheDocument();
  expect(adminApi.getDashboard).toHaveBeenCalledWith(7);
  expect(within(tile("Appointments today")).getByText("5")).toBeInTheDocument();
  expect(within(tile("Appointments today")).getByText("1 completed · 3 upcoming · 1 cancelled")).toBeInTheDocument();
  expect(within(tile("Doctors on leave today")).getByText("1")).toBeInTheDocument();
  expect(within(tile("Active patients")).getByText("1,234")).toBeInTheDocument();

  expect(screen.getByText("Last 7 days · 30 Sep 2026 – 6 Oct 2026")).toBeInTheDocument();
  expect(within(tile("Appointments")).getByText("11")).toBeInTheDocument();
  expect(within(tile("Cancelled")).getByText("27.3%")).toBeInTheDocument();
  expect(within(tile("Cancelled")).getByText("3 appointments: 2 by patients, 1 by doctors")).toBeInTheDocument();
  expect(within(tile("Missed visits")).getByText("25%")).toBeInTheDocument();
  expect(within(tile("New patients")).getByText("Counted since 6 Oct 2026")).toBeInTheDocument();

  expect(screen.getByTestId("trend-chart")).toBeInTheDocument();
  const legend = screen.getByRole("list", { name: "Legend" });
  expect(within(legend).getAllByRole("listitem").map((li) => li.textContent))
    .toEqual(["Completed", "Missed", "Upcoming", "Cancelled"]);

  expect(screen.getByText("Cardiology")).toBeInTheDocument();
  expect(screen.getByText("Not assigned")).toBeInTheDocument();
  expect(screen.getByText("Dr Rao · Cardiology")).toBeInTheDocument();
  expect(screen.getByText("4 (3 completed)")).toBeInTheDocument();

  // The old placeholder cards are gone
  expect(screen.queryByText(/Sales Value|Revenue|Traffic Share|Page Visits/)).not.toBeInTheDocument();
});

test("the numbers behind the chart are also in a table, newest day first", async () => {
  render(<AdminDashboard />);
  await screen.findByText("Today · 6 Oct 2026");

  const rows = within(screen.getByRole("table")).getAllByRole("row");
  expect(rows).toHaveLength(8); // header + 7 days
  expect(within(rows[1]).getAllByRole("cell").map((cell) => cell.textContent))
    .toEqual(["6 Oct 2026", "1", "0", "3", "1", "1"]); // completed, missed, upcoming, cancelled, new patients
});

test("the period buttons switch between 7, 30 and 90 days", async () => {
  render(<AdminDashboard />);
  await screen.findByText("Today · 6 Oct 2026");
  expect(screen.getByRole("button", { name: "7 days" })).toHaveAttribute("aria-pressed", "true");

  adminApi.getDashboard.mockResolvedValue({
    data: { ...DASHBOARD, period: { days: 30, from: "2026-09-07", to: "2026-10-06" } },
  });
  fireEvent.click(screen.getByRole("button", { name: "30 days" }));

  expect(await screen.findByText("Last 30 days · 7 Sep 2026 – 6 Oct 2026")).toBeInTheDocument();
  expect(adminApi.getDashboard).toHaveBeenLastCalledWith(30);
  expect(screen.getByRole("button", { name: "30 days" })).toHaveAttribute("aria-pressed", "true");
  expect(screen.getByRole("button", { name: "7 days" })).toHaveAttribute("aria-pressed", "false");
});

test("a late answer to an earlier click doesn't overwrite the newer period", async () => {
  let answerFor7;
  adminApi.getDashboard.mockImplementation((days) =>
    days === 7
      ? new Promise((resolve) => { answerFor7 = resolve; })
      : Promise.resolve({ data: { ...DASHBOARD, period: { days: 90, from: "2026-07-09", to: "2026-10-06" } } })
  );
  render(<AdminDashboard />);
  fireEvent.click(screen.getByRole("button", { name: "90 days" }));
  expect(await screen.findByText("Last 90 days · 9 Jul 2026 – 6 Oct 2026")).toBeInTheDocument();

  await act(async () => answerFor7({ data: DASHBOARD })); // the 7-day answer arrives last
  expect(screen.getByText("Last 90 days · 9 Jul 2026 – 6 Oct 2026")).toBeInTheDocument();
});

test("an empty period says so instead of showing empty lists", async () => {
  adminApi.getDashboard.mockResolvedValue({
    data: {
      ...DASHBOARD,
      busiestSpecializations: [],
      busiestDoctors: [],
      newPatients: { count: 0, countedSince: null },
    },
  });
  render(<AdminDashboard />);

  expect(await screen.findAllByText("No appointments in this period.")).toHaveLength(2);
  expect(screen.getByText("Counting starts with the next new account")).toBeInTheDocument();
});

test("shows the server's message when the dashboard can't be loaded", async () => {
  adminApi.getDashboard.mockRejectedValue({
    response: { status: 400, data: { message: "The period must be 7, 30 or 90 days" } },
  });
  render(<AdminDashboard />);

  expect(await screen.findByRole("alert")).toHaveTextContent("The period must be 7, 30 or 90 days");
  await waitFor(() => expect(screen.queryByText("Loading dashboard...")).not.toBeInTheDocument());
});
