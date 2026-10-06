import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import AuditLog from "./AuditLog";
import * as adminApi from "./api";

jest.mock("./api");

const ENTRIES = [
  {
    id: 3, occurredAt: "2026-10-05T16:40:12.123456", actorUsername: "drrao", actorRole: "DOCTOR",
    action: "CONSULTATION_VIEWED", targetType: "APPOINTMENT", targetId: 42, patientId: 7,
    patientName: "Asha Verma", details: null, ipAddress: "10.0.0.5",
  },
  {
    id: 2, occurredAt: "2026-10-05T09:00:00", actorUsername: null, actorRole: null,
    action: "PATIENT_LIST_VIEWED", targetType: "NONE", targetId: null, patientId: null, details: "12 patients",
  },
  {
    id: 1, occurredAt: "2026-10-04T18:30:00", actorUsername: "flowadmin", actorRole: "ADMIN",
    action: "ACCOUNT_DELETED", targetType: "USER", targetId: 9, patientId: 8, patientName: null,
    details: "olduser (PATIENT)",
  },
];

const pageOf = (entries, { page = 0, totalPages = 1, totalEntries = entries.length } = {}) => ({
  data: { entries, page, size: 50, totalEntries, totalPages },
});

const table = () => screen.getByRole("table");
const rowOf = (text) => within(table()).getByText(text).closest("tr");

beforeEach(() => {
  adminApi.getAuditLog.mockResolvedValue(pageOf(ENTRIES));
});

afterEach(() => jest.resetAllMocks());

test("shows the newest entries in a readable form", async () => {
  render(<AuditLog />);
  await within(table()).findByText("Viewed consultation");

  const viewed = rowOf("Viewed consultation");
  expect(within(viewed).getByText("2026-10-05 16:40:12")).toBeInTheDocument();
  expect(within(viewed).getByText("drrao")).toBeInTheDocument();
  expect(within(viewed).getByText("DOCTOR")).toBeInTheDocument();
  expect(within(viewed).getByText("Asha Verma #7")).toBeInTheDocument();
  expect(within(viewed).getByText("Appointment #42")).toBeInTheDocument();
  expect(within(viewed).getByText("10.0.0.5")).toBeInTheDocument();

  const listed = rowOf("Viewed patient list");
  expect(within(listed).getByText("System")).toBeInTheDocument(); // no logged-in user
  expect(within(listed).getByText("12 patients")).toBeInTheDocument();

  const deleted = rowOf("Deleted account");
  expect(within(deleted).getByText("(deleted) #8")).toBeInTheDocument();
  expect(within(deleted).getByText("User #9")).toBeInTheDocument();

  expect(adminApi.getAuditLog).toHaveBeenCalledWith({ page: 0, size: 50 });
  expect(screen.getByText("Page 1 of 1 · 3 entries")).toBeInTheDocument();
});

test("sends only the filled-in filters, and Clear shows everything again", async () => {
  render(<AuditLog />);
  await within(table()).findByText("Viewed consultation");

  fireEvent.change(screen.getByLabelText("Username"), { target: { value: " drrao " } });
  fireEvent.change(screen.getByLabelText("Action"), { target: { value: "CONSULTATION_VIEWED" } });
  fireEvent.change(screen.getByLabelText("From"), { target: { value: "2026-10-01" } });
  fireEvent.click(screen.getByRole("button", { name: "Search" }));

  await waitFor(() =>
    expect(adminApi.getAuditLog).toHaveBeenLastCalledWith({
      page: 0, size: 50, username: "drrao", action: "CONSULTATION_VIEWED", from: "2026-10-01",
    })
  );

  fireEvent.click(screen.getByRole("button", { name: "Clear" }));
  await waitFor(() => expect(adminApi.getAuditLog).toHaveBeenLastCalledWith({ page: 0, size: 50 }));
  expect(screen.getByLabelText("Username")).toHaveValue("");
});

test("pages through the results", async () => {
  adminApi.getAuditLog
    .mockResolvedValueOnce(pageOf(ENTRIES, { totalPages: 2, totalEntries: 53 }))
    .mockResolvedValueOnce(pageOf([ENTRIES[2]], { page: 1, totalPages: 2, totalEntries: 53 }));
  render(<AuditLog />);

  expect(await screen.findByText("Page 1 of 2 · 53 entries")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Previous" })).toBeDisabled();

  fireEvent.click(screen.getByRole("button", { name: "Next" }));

  expect(await screen.findByText("Page 2 of 2 · 53 entries")).toBeInTheDocument();
  expect(adminApi.getAuditLog).toHaveBeenLastCalledWith({ page: 1, size: 50 });
  expect(screen.getByRole("button", { name: "Next" })).toBeDisabled();
  expect(screen.getByRole("button", { name: "Previous" })).toBeEnabled();
});

test("shows the server's message when the log can't be loaded", async () => {
  adminApi.getAuditLog.mockRejectedValue({
    response: { status: 400, data: { message: "'from' must not be after 'to'" } },
  });
  render(<AuditLog />);

  expect(await screen.findByText("'from' must not be after 'to'")).toBeInTheDocument();
});
