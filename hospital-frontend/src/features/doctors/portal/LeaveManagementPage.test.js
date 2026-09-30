import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import LeaveManagementPage from "./LeaveManagementPage";
import { getMyLeaves } from "../api";

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
