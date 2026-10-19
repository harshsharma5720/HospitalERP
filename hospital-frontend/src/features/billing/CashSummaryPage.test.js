import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import CashSummaryPage from "./CashSummaryPage";
import { differenceOf } from "./cashParts";
import * as billingApi from "./api";
import { ToastProvider } from "../../shared/ui";
import { addDays, toLocalISODate } from "../../shared/utils/dateUtils";

jest.mock("./api");

const TODAY = toLocalISODate();
const method = (name, payments, taken, refunds = 0, refunded = 0) => ({
  method: name, payments, taken, refunds, refunded, net: taken - refunded,
});
const line = (at, kind, billId, patientName, name, by, amount, reference = null) => ({
  at: `${TODAY}T${at}:00`, kind, billId, billNumber: `B-2026-0000${billId}`, patientName, doctorName: "Anita Rao", method: name,
  reference, by, amount,
});

const SUMMARY = {
  date: TODAY,
  methods: [method("CASH", 2, 1100, 1, 500), method("CARD", 1, 500), method("UPI", 0, 0), method("ONLINE", 1, 500)],
  totals: { payments: 4, taken: 2100, refunds: 1, refunded: 500, net: 1600 },
  people: [
    { by: "deskmeena", payments: 3, taken: 1600, refunds: 1, refunded: 500 },
    { by: null, payments: 1, taken: 500, refunds: 0, refunded: 0 },
  ],
  lines: [
    line("09:05", "PAYMENT", 11, "Asha Verma", "CASH", "deskmeena", 600),
    line("11:00", "PAYMENT", 12, "Ravi Kumar", "ONLINE", null, 500, "pay_X1"),
    line("15:30", "REFUND", 13, "Mira Shah", "CASH", "deskmeena", 500),
  ],
  unpaid: { bills: 2, amount: 1000 },
  closes: [
    { id: 7, periodStart: `${TODAY}T00:00:00`, closedAt: `${TODAY}T13:00:00`, closedBy: "deskmeena", cashIn: 600, cashOut: 0,
      expected: 600, counted: 550, difference: -50, note: "Change given twice" },
  ],
  sinceLastClose: { from: `${TODAY}T13:00:00`, payments: 1, cashIn: 1100, refunds: 1, cashOut: 500, expected: 600 },
};

const renderAt = (entry) =>
  render(
    <ToastProvider>
      <MemoryRouter initialEntries={[entry]}>
        <Routes>
          <Route path="/cash-summary" element={<CashSummaryPage />} />
          <Route path="/admin/cash-summary" element={<CashSummaryPage />} />
        </Routes>
      </MemoryRouter>
    </ToastProvider>
  );

const rowOf = (text) => screen.getByText(text).closest("tr");

beforeEach(() => {
  billingApi.getCashSummaryToday.mockResolvedValue({ data: SUMMARY });
});
afterEach(() => jest.resetAllMocks());

test("today's summary: the totals, each method, every payment and refund, who took it, the unpaid bills and the closes", async () => {
  renderAt("/cash-summary");
  expect(await screen.findAllByText("₹2,100.00")).toHaveLength(2); // the tile and the total
  expect(screen.getByText("4 payments")).toBeInTheDocument();
  expect(screen.getByText("1 refund")).toBeInTheDocument();
  expect(screen.getAllByText("₹1,600.00").length).toBeGreaterThan(0);
  expect(billingApi.getCashSummaryToday).toHaveBeenCalledTimes(1);
  expect(billingApi.getCashSummaryFor).not.toHaveBeenCalled();

  const methods = screen.getByRole("region", { name: "By method" });
  expect(within(methods).getByText("Cash").closest("tr")).toHaveTextContent("₹1,100.00 (2)₹500.00 (1)₹600.00");
  expect(within(methods).getByText("Online").closest("tr")).toHaveTextContent("₹500.00 (1)");

  const lines = screen.getByRole("region", { name: "Payments and refunds" });
  expect(within(lines).getByRole("link", { name: "B-2026-000011" })).toHaveAttribute("href", "/bills/11");
  expect(rowOf("Ravi Kumar")).toHaveTextContent("11:00B-2026-000012Ravi KumarOnline (pay_X1)Online₹500.00");
  expect(rowOf("Mira Shah")).toHaveTextContent("Refund");
  expect(rowOf("Mira Shah")).toHaveTextContent("-₹500.00");

  const people = screen.getByRole("region", { name: "Who took it" });
  expect(people).toHaveTextContent("deskmeena3 payments, ₹1,600.00 · 1 refund, ₹500.00");
  expect(people).toHaveTextContent("Online (the patient)1 payment, ₹500.00");

  expect(screen.getByText(/Still unpaid: 2 bills of today's visits, ₹1,000.00/)).toBeInTheDocument();
  expect(screen.getByRole("link", { name: "See them" })).toHaveAttribute("href", `/bills?from=${TODAY}&to=${TODAY}&status=unpaid`);

  const closes = screen.getByRole("region", { name: "Close the day" });
  expect(closes).toHaveTextContent("Cash in the drawer since 13:00₹600.00₹1,100.00 taken, ₹500.00 given back");
  const made = within(closes).getByRole("list", { name: "The day's closes" });
  expect(made).toHaveTextContent("Closed at 13:00 by deskmeena₹50.00 short");
  expect(made).toHaveTextContent("Expected ₹600.00, counted ₹550.00");
  expect(made).toHaveTextContent("Change given twice");
});

test("closing with the cash as expected: no note, a confirmation, and the summary again", async () => {
  billingApi.closeTheDay.mockResolvedValue({ data: { ...SUMMARY.closes[0], id: 8, expected: 600, counted: 600, difference: 0 } });
  renderAt("/cash-summary");
  fireEvent.click(await screen.findByRole("button", { name: "Close the day..." }));
  const dialog = screen.getByRole("dialog", { name: "Close the day" });
  expect(dialog).toHaveTextContent("The drawer should hold ₹600.00: ₹1,100.00 taken, ₹500.00 given back.");
  fireEvent.change(within(dialog).getByLabelText("Cash counted (₹)"), { target: { value: "600" } });
  expect(within(dialog).getByText("Matches")).toBeInTheDocument();
  expect(within(dialog).queryByLabelText("Why is it different?")).not.toBeInTheDocument();
  fireEvent.click(within(dialog).getByRole("button", { name: "Close the day" }));

  expect(await screen.findByText("The day is closed: the cash matches.")).toBeInTheDocument();
  expect(billingApi.closeTheDay).toHaveBeenCalledWith({ counted: 600, note: null });
  expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  expect(billingApi.getCashSummaryToday).toHaveBeenCalledTimes(2);
});

test("a different count needs a note, and a refusal is shown in the dialog", async () => {
  renderAt("/cash-summary");
  fireEvent.click(await screen.findByRole("button", { name: "Close the day..." }));
  const dialog = screen.getByRole("dialog", { name: "Close the day" });
  fireEvent.change(within(dialog).getByLabelText("Cash counted (₹)"), { target: { value: "550" } });
  expect(within(dialog).getByText("₹50.00 short")).toBeInTheDocument();
  fireEvent.click(within(dialog).getByRole("button", { name: "Close the day" }));
  expect(within(dialog).getByRole("alert")).toHaveTextContent("The cash counted isn't what was expected. Please say why.");
  expect(billingApi.closeTheDay).not.toHaveBeenCalled();

  fireEvent.change(within(dialog).getByLabelText("Why is it different?"), { target: { value: " Gave too much change " } });
  billingApi.closeTheDay.mockRejectedValueOnce({
    response: { status: 409, data: { message: "Someone has just closed the day. Please look at the summary again." } },
  });
  fireEvent.click(within(dialog).getByRole("button", { name: "Close the day" }));
  expect(await within(dialog).findByText("Someone has just closed the day. Please look at the summary again.")).toBeInTheDocument();
  expect(billingApi.closeTheDay).toHaveBeenCalledWith({ counted: 550, note: "Gave too much change" });

  billingApi.closeTheDay.mockResolvedValueOnce({ data: { ...SUMMARY.closes[0], id: 9, difference: -50 } });
  fireEvent.click(within(dialog).getByRole("button", { name: "Close the day" }));
  expect(await screen.findByText("The day is closed: ₹50.00 short.")).toBeInTheDocument();
});

test("a quiet day: nothing taken, nobody, no unpaid bills, and the drawer still to close", async () => {
  billingApi.getCashSummaryToday.mockResolvedValue({
    data: {
      ...SUMMARY,
      methods: SUMMARY.methods.map((m) => method(m.method, 0, 0)),
      totals: { payments: 0, taken: 0, refunds: 0, refunded: 0, net: 0 },
      people: [],
      lines: [],
      unpaid: { bills: 0, amount: 0 },
      closes: [],
      sinceLastClose: { from: `${TODAY}T00:00:00`, payments: 0, cashIn: 0, refunds: 0, cashOut: 0, expected: 0 },
    },
  });
  renderAt("/cash-summary");
  expect(await screen.findByText("No payments or refunds yet today.")).toBeInTheDocument();
  expect(screen.getByText("Nobody yet.")).toBeInTheDocument();
  expect(screen.getByText("0 payments")).toBeInTheDocument();
  expect(screen.queryByText(/Still unpaid/)).not.toBeInTheDocument();
  expect(screen.getByRole("region", { name: "Close the day" })).toHaveTextContent("Cash in the drawer since 00:00₹0.00");
  expect(screen.queryByRole("list", { name: "The day's closes" })).not.toBeInTheDocument();
});

test("the PDF downloads; a summary that can't be loaded says so and loads again", async () => {
  global.URL.createObjectURL = jest.fn(() => "blob:summary");
  global.URL.revokeObjectURL = jest.fn();
  billingApi.getCashSummaryPdfToday.mockResolvedValue({ data: "%PDF-" });
  billingApi.getCashSummaryToday.mockRejectedValueOnce({ response: { status: 500, data: { message: "Server down" } } });
  renderAt("/cash-summary");
  expect(await screen.findByText("Server down")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Download PDF" })).toBeDisabled();
  fireEvent.click(screen.getByRole("button", { name: "Try again" }));
  expect(await screen.findAllByText("₹2,100.00")).toHaveLength(2);

  const click = jest.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => {});
  fireEvent.click(screen.getByRole("button", { name: "Download PDF" }));
  await waitFor(() => expect(click).toHaveBeenCalledTimes(1));
  expect(billingApi.getCashSummaryPdfToday).toHaveBeenCalledTimes(1);
  expect(click.mock.instances[0].download).toBe(`day-summary-${TODAY}.pdf`);
  click.mockRestore();
});

test("admins pick any day: a past day shows its closes but can't be closed any more", async () => {
  billingApi.getCashSummaryFor.mockResolvedValue({ data: { ...SUMMARY, date: "2026-10-10", sinceLastClose: null, closes: [] } });
  renderAt("/admin/cash-summary?date=2026-10-10");
  expect(await screen.findByText("The day wasn't closed.")).toBeInTheDocument();
  expect(billingApi.getCashSummaryFor).toHaveBeenCalledWith("2026-10-10");
  expect(billingApi.getCashSummaryToday).not.toHaveBeenCalled();
  expect(screen.queryByRole("button", { name: "Close the day..." })).not.toBeInTheDocument();
  expect(screen.getByRole("link", { name: "B-2026-000011" })).toHaveAttribute("href", "/admin/bills/11");
  expect(screen.getByText(/of the day's visits/)).toBeInTheDocument();

  fireEvent.change(screen.getByLabelText("Day"), { target: { value: "2026-10-12" } });
  fireEvent.click(screen.getByRole("button", { name: "Show" }));
  expect(await screen.findByText("The day wasn't closed.")).toBeInTheDocument();
  expect(billingApi.getCashSummaryFor).toHaveBeenLastCalledWith("2026-10-12");

  fireEvent.click(screen.getByRole("button", { name: "Today" }));
  await screen.findByText("The day wasn't closed.");
  expect(billingApi.getCashSummaryFor).toHaveBeenLastCalledWith(TODAY);
});

const day = (date, extra = {}) => ({
  date, payments: 0, taken: 0, refunds: 0, refunded: 0, net: 0, cashNet: 0, closes: 0, difference: null, ...extra,
});
const RANGE = {
  from: "2026-10-10",
  to: "2026-10-12",
  days: [
    day("2026-10-10"),
    day("2026-10-11", { payments: 3, taken: 1500, net: 1500, cashNet: 1000 }),
    day("2026-10-12", { payments: 4, taken: 2100, refunds: 1, refunded: 500, net: 1600, cashNet: 600, closes: 2, difference: -50 }),
  ],
  methods: SUMMARY.methods,
  totals: { payments: 7, taken: 3600, refunds: 1, refunded: 500, net: 3100 },
  closes: [
    { ...SUMMARY.closes[0], id: 7, closedAt: "2026-10-12T13:00:00" },
    { ...SUMMARY.closes[0], id: 8, closedAt: "2026-10-12T19:00:00", closedBy: "deskravi", counted: 600, difference: 0, note: null },
  ],
};
const lastDays = (n) => toLocalISODate(addDays(new Date(), 1 - n));

test("admins see several days: a row per day newest first, the closes that didn't match, and a day opens its own", async () => {
  billingApi.getCashRange.mockResolvedValue({ data: RANGE });
  billingApi.getCashSummaryFor.mockResolvedValue({ data: { ...SUMMARY, date: "2026-10-12", sinceLastClose: null } });
  renderAt("/admin/cash-summary?from=2026-10-10&to=2026-10-12");
  const days = await screen.findByRole("region", { name: "Day by day" });
  expect(billingApi.getCashRange).toHaveBeenCalledWith("2026-10-10", "2026-10-12");
  expect(billingApi.getCashSummaryFor).not.toHaveBeenCalled();
  expect(screen.getByText(/Sat 10 Oct 2026 to Mon 12 Oct 2026/)).toBeInTheDocument();
  expect(screen.getAllByText("₹3,600.00")).toHaveLength(2); // the tile and the total
  expect(screen.queryByRole("button", { name: "Download PDF" })).not.toBeInTheDocument(); // one day at a time
  expect(screen.getByRole("button", { name: "Several days" })).toHaveAttribute("aria-pressed", "true");

  const rows = within(days).getAllByRole("row").slice(1).map((row) => row.textContent);
  expect(rows).toEqual([
    "Mon 12 Oct 2026₹2,100.00₹500.00₹1,600.00₹600.002 closes₹50.00 short",
    "Sun 11 Oct 2026₹1,500.00₹0.00₹1,500.00₹1,000.00Not closed",
    "Sat 10 Oct 2026₹0.00₹0.00₹0.00₹0.00—",
  ]);
  const mismatched = screen.getByRole("region", { name: "Cash that didn't match" });
  expect(mismatched).toHaveTextContent("Mon 12 Oct, 13:00 · deskmeena₹50.00 short");
  expect(mismatched).toHaveTextContent("Change given twice");
  expect(mismatched).not.toHaveTextContent("deskravi");

  fireEvent.click(within(days).getByRole("link", { name: "Mon 12 Oct 2026" }));
  expect(await screen.findByText("Closed at 13:00 by deskmeena")).toBeInTheDocument();
  expect(billingApi.getCashSummaryFor).toHaveBeenCalledWith("2026-10-12");
  expect(screen.getByRole("button", { name: "One day" })).toHaveAttribute("aria-pressed", "true");
});

test("admins choose several days: the last 7 or 30, or any from and to; a refused choice says why", async () => {
  billingApi.getCashSummaryFor.mockResolvedValue({ data: { ...SUMMARY, sinceLastClose: null } });
  billingApi.getCashRange.mockResolvedValue({ data: RANGE });
  renderAt("/admin/cash-summary");
  await screen.findAllByText("₹2,100.00");
  fireEvent.click(screen.getByRole("button", { name: "Several days" }));
  expect(screen.getByLabelText("From")).toHaveValue(lastDays(7));
  expect(screen.getByLabelText("To")).toHaveValue(TODAY);
  expect(billingApi.getCashRange).not.toHaveBeenCalled();

  fireEvent.click(screen.getByRole("button", { name: "Last 30 days" }));
  await screen.findByRole("region", { name: "Day by day" });
  expect(billingApi.getCashRange).toHaveBeenLastCalledWith(lastDays(30), TODAY);

  fireEvent.change(screen.getByLabelText("From"), { target: { value: "2026-10-01" } });
  fireEvent.change(screen.getByLabelText("To"), { target: { value: "2026-10-05" } });
  fireEvent.click(screen.getByRole("button", { name: "Show" }));
  await waitFor(() => expect(billingApi.getCashRange).toHaveBeenLastCalledWith("2026-10-01", "2026-10-05"));

  billingApi.getCashRange.mockRejectedValueOnce({ response: { status: 400, data: { message: "Choose at most 31 days" } } });
  fireEvent.click(screen.getByRole("button", { name: "Last 7 days" }));
  await waitFor(() => expect(billingApi.getCashRange).toHaveBeenLastCalledWith(lastDays(7), TODAY));
  expect(await screen.findByText("Choose at most 31 days")).toBeInTheDocument();

  fireEvent.click(screen.getByRole("button", { name: "One day" }));
  fireEvent.click(screen.getByRole("button", { name: "Today" }));
  expect(await screen.findAllByText("₹2,100.00")).toHaveLength(2);
  expect(billingApi.getCashSummaryFor).toHaveBeenLastCalledWith(TODAY);
});

test("the desk has no other days to choose", async () => {
  renderAt("/cash-summary");
  await screen.findAllByText("₹2,100.00");
  expect(screen.queryByRole("button", { name: "Several days" })).not.toBeInTheDocument();
  expect(screen.queryByLabelText("Day")).not.toBeInTheDocument();
});

test("a close's difference is named, not shown by colour alone", () => {
  expect(differenceOf(0)).toEqual({ label: "Matches", tone: "success" });
  expect(differenceOf(-50)).toEqual({ label: "₹50.00 short", tone: "danger" });
  expect(differenceOf("20.5")).toEqual({ label: "₹20.50 over", tone: "warning" });
});
