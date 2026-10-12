import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import LeaveApproval from "./LeaveApproval";
import * as adminApi from "./api";
import { ConfirmProvider, ToastProvider } from "../../shared/ui";

jest.mock("./api", () => ({
  getPendingLeaves: jest.fn(),
  getApprovedLeaves: jest.fn(),
  getRejectedLeaves: jest.fn(),
  decideLeave: jest.fn(),
}));

const LEAVE = { id: 7, userId: 11, role: "ROLE_DOCTOR", startDate: "2026-10-20", endDate: "2026-10-22", reason: "Conference", status: "PENDING" };

const renderPage = () =>
  render(
    <ToastProvider>
      <ConfirmProvider>
        <LeaveApproval />
      </ConfirmProvider>
    </ToastProvider>
  );

beforeEach(() => {
  adminApi.getPendingLeaves.mockResolvedValue({ data: [LEAVE] });
  adminApi.getApprovedLeaves.mockResolvedValue({ data: [] });
  adminApi.decideLeave.mockResolvedValue({ data: {} });
});
afterEach(() => jest.clearAllMocks());

test("lists the pending requests with their days", async () => {
  renderPage();
  const row = (await screen.findByText("Conference")).closest("tr");
  expect(row).toHaveTextContent("User #11");
  expect(row).toHaveTextContent("Doctor");
  expect(row).toHaveTextContent("Tue 20 Oct 2026");
  expect(within(row).getByText("3")).toBeInTheDocument();
});

test("approving asks first, warns about the appointments, then approves and reloads", async () => {
  renderPage();
  fireEvent.click(await screen.findByRole("button", { name: "Approve leave 7" }));
  const dialog = screen.getByRole("dialog", { name: "Approve this leave?" });
  expect(dialog).toHaveTextContent("Appointments in this period will be cancelled");
  fireEvent.click(within(dialog).getByRole("button", { name: "Approve" }));
  await waitFor(() => expect(adminApi.decideLeave).toHaveBeenCalledWith(7, "approve"));
  expect(await screen.findByRole("status")).toHaveTextContent("Leave approved.");
  expect(adminApi.getPendingLeaves).toHaveBeenCalledTimes(2);
});

test("Cancel leaves the request pending", async () => {
  renderPage();
  fireEvent.click(await screen.findByRole("button", { name: "Reject leave 7" }));
  fireEvent.click(within(screen.getByRole("dialog", { name: "Reject this leave request?" })).getByRole("button", { name: "Cancel" }));
  await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
  expect(adminApi.decideLeave).not.toHaveBeenCalled();
});

test("the other tabs have no decision buttons", async () => {
  adminApi.getApprovedLeaves.mockResolvedValue({ data: [{ ...LEAVE, status: "APPROVED" }] });
  renderPage();
  await screen.findByText("Conference");
  fireEvent.click(screen.getByRole("button", { name: "Approved" }));
  await waitFor(() => expect(adminApi.getApprovedLeaves).toHaveBeenCalled());
  expect(await screen.findByText("Approved", { selector: "span" })).toBeInTheDocument();
  expect(screen.queryByRole("button", { name: /Approve leave/ })).not.toBeInTheDocument();
});
