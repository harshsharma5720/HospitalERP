import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import LeaveManagementPage from "./LeaveManagementPage";
import { getMyLeaves, applyForLeave } from "../api";
import { ToastProvider } from "../../../shared/ui";

jest.mock("../api");

// Unsigned test token; the page only reads the payload
const token = `x.${btoa(JSON.stringify({ sub: "rao", role: "ROLE_DOCTOR", userId: 5 }))}.y`;
// Long enough for a render loop (effect → setState → effect ...) to show up in the call count
const settle = () => new Promise((resolve) => setTimeout(resolve, 100));

beforeEach(() => {
  localStorage.setItem("jwtToken", token);
  getMyLeaves.mockResolvedValue({ data: [] });
});

afterEach(() => {
  localStorage.clear();
  jest.resetAllMocks();
});

test("loads the leaves once, and again only when the tab changes", async () => {
  render(
    <MemoryRouter>
      <LeaveManagementPage />
    </MemoryRouter>
  );

  await waitFor(() => expect(getMyLeaves).toHaveBeenCalledWith(5, "pending"));
  await settle();
  expect(getMyLeaves).toHaveBeenCalledTimes(1);

  fireEvent.click(screen.getByText("Approved Leaves"));

  await waitFor(() => expect(getMyLeaves).toHaveBeenCalledWith(5, "approved"));
  await settle();
  expect(getMyLeaves).toHaveBeenCalledTimes(2);
});

const renderWithToasts = () =>
  render(
    <ToastProvider>
      <MemoryRouter>
        <LeaveManagementPage />
      </MemoryRouter>
    </ToastProvider>
  );

test("lists the leave with its days and status", async () => {
  getMyLeaves.mockResolvedValue({
    data: [{ id: 1, startDate: "2026-10-20", endDate: "2026-10-22", reason: "Conference", status: "PENDING" }],
  });
  renderWithToasts();
  const row = (await screen.findByText("Conference")).closest("tr");
  expect(row).toHaveTextContent("Tue 20 Oct 2026");
  expect(row).toHaveTextContent("Thu 22 Oct 2026");
  expect(row).toHaveTextContent("3");
  expect(row).toHaveTextContent("Pending");
});

test("applying sends the dates and reason, closes the form, says so and reloads", async () => {
  applyForLeave.mockResolvedValue({ data: {} });
  renderWithToasts();
  await waitFor(() => expect(getMyLeaves).toHaveBeenCalledTimes(1));

  fireEvent.click(screen.getByRole("button", { name: "Apply for leave" }));
  const form = screen.getByRole("dialog", { name: "Apply for leave" });
  fireEvent.change(screen.getByLabelText("From"), { target: { value: "2026-11-02" } });
  fireEvent.change(screen.getByLabelText("To"), { target: { value: "2026-11-03" } });
  fireEvent.change(screen.getByLabelText("Reason"), { target: { value: " Family function " } });
  fireEvent.click(within(form).getByRole("button", { name: "Apply" }));

  expect(await screen.findByRole("status")).toHaveTextContent("Leave applied.");
  expect(applyForLeave).toHaveBeenCalledWith({
    userId: 5, role: "ROLE_DOCTOR", startDate: "2026-11-02", endDate: "2026-11-03", reason: "Family function",
  });
  expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  expect(getMyLeaves).toHaveBeenCalledTimes(2);
});

test("a refused request keeps the form open and shows why", async () => {
  applyForLeave.mockRejectedValue({ response: { data: { message: "You already have leave on these dates" } } });
  renderWithToasts();
  fireEvent.click(await screen.findByRole("button", { name: "Apply for leave" }));
  fireEvent.change(screen.getByLabelText("From"), { target: { value: "2026-11-02" } });
  fireEvent.change(screen.getByLabelText("To"), { target: { value: "2026-11-02" } });
  fireEvent.change(screen.getByLabelText("Reason"), { target: { value: "Rest" } });
  fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Apply" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("You already have leave on these dates");
  expect(screen.getByRole("dialog", { name: "Apply for leave" })).toBeInTheDocument();
});
