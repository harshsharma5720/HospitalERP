import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import ManageUsers from "./ManageUsers";
import * as adminApi from "./api";
import { ConfirmProvider, ToastProvider } from "../../shared/ui";

jest.mock("./api");

const USERS = [
  { id: 2, username: "asha", role: "PATIENT", active: true },
  { id: 3, username: "drold", role: "DOCTOR", active: false, deactivatedAt: "2026-10-01T10:00:00" },
  { id: 4, username: "desk1", role: "RECEPTIONIST", active: true },
];

const rowOf = (username) => screen.getByText(username).closest("tr");
const confirmWith = (label) => fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: label }));

beforeEach(() => {
  adminApi.getAllUsers.mockResolvedValue({ data: USERS });
  adminApi.deactivateUser.mockResolvedValue({ data: {} });
  adminApi.reactivateUser.mockResolvedValue({ data: {} });
});

afterEach(() => jest.resetAllMocks());

const renderPage = async () => {
  render(
    <ToastProvider>
      <ConfirmProvider>
        <MemoryRouter>
          <ManageUsers />
        </MemoryRouter>
      </ConfirmProvider>
    </ToastProvider>
  );
  await screen.findByText("asha");
};

test("shows each account's status with the matching action", async () => {
  await renderPage();
  expect(within(rowOf("asha")).getByText("Active")).toBeInTheDocument();
  expect(within(rowOf("asha")).getByText("Patient")).toBeInTheDocument();
  expect(within(rowOf("asha")).getByRole("button", { name: "Deactivate" })).toBeInTheDocument();
  expect(within(rowOf("drold")).getByText("Deactivated")).toBeInTheDocument();
  expect(within(rowOf("drold")).getByRole("button", { name: "Reactivate" })).toBeInTheDocument();
  expect(screen.getByRole("link", { name: "Add user" })).toHaveAttribute("href", "/admin/register-user");
  expect(screen.queryByRole("button", { name: "Edit" })).not.toBeInTheDocument(); // it never did anything
});

test("deactivating asks first; Cancel leaves the account alone", async () => {
  await renderPage();
  fireEvent.click(within(rowOf("asha")).getByRole("button", { name: "Deactivate" }));
  expect(screen.getByRole("dialog", { name: "Deactivate asha?" })).toBeInTheDocument();
  confirmWith("Cancel");
  await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
  expect(adminApi.deactivateUser).not.toHaveBeenCalled();
});

test("deactivate and reactivate call the server, say so and reload the list", async () => {
  await renderPage();

  fireEvent.click(within(rowOf("asha")).getByRole("button", { name: "Deactivate" }));
  confirmWith("Deactivate");
  await waitFor(() => expect(adminApi.deactivateUser).toHaveBeenCalledWith(2));
  expect(await screen.findByText("asha deactivated.")).toBeInTheDocument();

  fireEvent.click(within(rowOf("drold")).getByRole("button", { name: "Reactivate" }));
  await waitFor(() => expect(adminApi.reactivateUser).toHaveBeenCalledWith(3));
  await waitFor(() => expect(adminApi.getAllUsers).toHaveBeenCalledTimes(3));
});

test("a refused permanent delete shows the server's reason", async () => {
  adminApi.deleteUserPermanently.mockRejectedValue({
    response: { status: 409, data: { message: "This account has appointments or medical records, which must be kept. Deactivate it instead." } },
  });
  await renderPage();

  fireEvent.click(within(rowOf("asha")).getByRole("button", { name: "Delete permanently" }));
  confirmWith("Delete permanently");

  expect(await screen.findByRole("alert")).toHaveTextContent("Deactivate it instead");
});

test("the list narrows by username and by role", async () => {
  await renderPage();
  fireEvent.change(screen.getByLabelText("Search by username"), { target: { value: "DESK" } });
  expect(screen.getByText("desk1")).toBeInTheDocument();
  expect(screen.queryByText("asha")).not.toBeInTheDocument();

  fireEvent.change(screen.getByLabelText("Search by username"), { target: { value: "" } });
  fireEvent.change(screen.getByLabelText("Role"), { target: { value: "DOCTOR" } });
  expect(screen.getByText("drold")).toBeInTheDocument();
  expect(screen.queryByText("desk1")).not.toBeInTheDocument();

  fireEvent.change(screen.getByLabelText("Role"), { target: { value: "ADMIN" } });
  expect(screen.getByText("No users match.")).toBeInTheDocument();
});
