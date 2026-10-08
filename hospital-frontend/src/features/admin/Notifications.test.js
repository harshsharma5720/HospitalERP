import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import Notifications from "./Notifications";
import * as adminApi from "./api";

jest.mock("./api");

const FAILED_SMS = {
  id: 7, channel: "SMS", recipient: "+919800000077", subject: null, description: "reminder SMS to patient",
  status: "FAILED", attempts: 7, lastError: "RuntimeException: Twilio is down",
  createdAt: "2026-10-06T09:00:00", nextAttemptAt: "2026-10-06T12:51:00", giveUpAt: "2026-10-07T09:00:00", sentAt: null,
};
const SENT_MAIL = {
  id: 8, channel: "EMAIL", recipient: "asha@example.com", subject: "Appointment Confirmation - Hospital ERP",
  description: "booking email to patient", status: "SENT", attempts: 1, lastError: null,
  createdAt: "2026-10-07T10:00:00", nextAttemptAt: "2026-10-07T10:00:00", giveUpAt: "2026-10-08T09:00:00",
  sentAt: "2026-10-07T10:00:02",
};

const pageOf = (entries, { page = 0, totalPages = 1 } = {}) => ({
  data: { entries, page, size: 50, totalEntries: entries.length, totalPages },
});

// The table row of a cell, given the cell or its text
const rowOf = (target) =>
  (typeof target === "string" ? within(screen.getByRole("table")).getByText(target) : target).closest("tr");

beforeEach(() => {
  adminApi.getNotifications.mockResolvedValue(pageOf([FAILED_SMS]));
  adminApi.resendNotification.mockResolvedValue({ data: { ...FAILED_SMS, status: "PENDING" } });
});
afterEach(() => jest.resetAllMocks());

test("shows the failed messages first, with their error and a Resend button", async () => {
  render(<Notifications />);

  const row = rowOf(await screen.findByText("reminder SMS to patient"));
  expect(adminApi.getNotifications).toHaveBeenCalledWith({ status: "FAILED", page: 0, size: 50 });
  expect(within(row).getByText("+919800000077")).toBeInTheDocument();
  expect(within(row).getByText("Failed")).toBeInTheDocument();
  expect(within(row).getByText("7")).toBeInTheDocument();
  expect(within(row).getByText("RuntimeException: Twilio is down")).toBeInTheDocument();
  expect(within(row).getByRole("button", { name: "Resend" })).toBeInTheDocument();
  expect(screen.getByLabelText("Show")).toHaveValue("FAILED");
});

test("Resend queues the message again and reloads the list", async () => {
  render(<Notifications />);
  const row = rowOf(await screen.findByText("reminder SMS to patient"));

  adminApi.getNotifications.mockResolvedValue(pageOf([]));
  fireEvent.click(within(row).getByRole("button", { name: "Resend" }));

  await waitFor(() => expect(adminApi.resendNotification).toHaveBeenCalledWith(7));
  expect(await screen.findByRole("status")).toHaveTextContent(
    "The SMS to +919800000077 is queued again and will be sent in a moment."
  );
  await waitFor(() => expect(adminApi.getNotifications).toHaveBeenCalledTimes(2));
  expect(await screen.findByText(/Nothing failed/)).toBeInTheDocument();
});

test("a refused resend shows the server's reason", async () => {
  adminApi.resendNotification.mockRejectedValue({
    response: { status: 409, data: { message: "Only failed or skipped messages can be resent; this one is sent" } },
  });
  render(<Notifications />);
  fireEvent.click(within(rowOf(await screen.findByText("reminder SMS to patient"))).getByRole("button", { name: "Resend" }));

  expect(await screen.findByRole("alert")).toHaveTextContent("Only failed or skipped messages can be resent");
});

test("the filter shows other messages; only failed or skipped ones can be resent", async () => {
  render(<Notifications />);
  await screen.findByText("reminder SMS to patient");

  adminApi.getNotifications.mockResolvedValue(pageOf([SENT_MAIL, FAILED_SMS]));
  fireEvent.change(screen.getByLabelText("Show"), { target: { value: "" } });

  await waitFor(() => expect(adminApi.getNotifications).toHaveBeenLastCalledWith({ page: 0, size: 50 }));
  const sent = rowOf(await screen.findByText("booking email to patient"));
  expect(within(sent).getByText("Sent")).toBeInTheDocument();
  expect(within(sent).getByText("Email")).toBeInTheDocument();
  expect(within(sent).queryByRole("button", { name: "Resend" })).not.toBeInTheDocument();
  expect(within(rowOf("reminder SMS to patient")).getByRole("button", { name: "Resend" })).toBeInTheDocument();
});

test("pages through long lists", async () => {
  adminApi.getNotifications
    .mockResolvedValueOnce(pageOf([FAILED_SMS], { totalPages: 2 }))
    .mockResolvedValueOnce(pageOf([{ ...FAILED_SMS, id: 9, description: "booking SMS to doctor" }], { page: 1, totalPages: 2 }));
  render(<Notifications />);
  await screen.findByText("reminder SMS to patient");
  expect(screen.getByRole("button", { name: "Previous" })).toBeDisabled();

  fireEvent.click(screen.getByRole("button", { name: "Next" }));

  expect(await screen.findByText("booking SMS to doctor")).toBeInTheDocument();
  expect(adminApi.getNotifications).toHaveBeenLastCalledWith({ status: "FAILED", page: 1, size: 50 });
  expect(screen.getByRole("button", { name: "Next" })).toBeDisabled();
});
