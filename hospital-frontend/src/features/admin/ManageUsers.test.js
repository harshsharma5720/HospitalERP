import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import ManageUsers from "./ManageUsers";
import * as adminApi from "./api";

jest.mock("./api");

const USERS = [
  { id: 2, username: "asha", role: "PATIENT", active: true },
  { id: 3, username: "drold", role: "DOCTOR", active: false, deactivatedAt: "2026-10-01T10:00:00" },
];

const rowOf = (username) => screen.getByText(username).closest("tr");

beforeEach(() => {
  adminApi.getAllUsers.mockResolvedValue({ data: USERS });
  adminApi.deactivateUser.mockResolvedValue({ data: {} });
  adminApi.reactivateUser.mockResolvedValue({ data: {} });
  jest.spyOn(window, "confirm").mockReturnValue(true);
  jest.spyOn(window, "alert").mockImplementation(() => {});
});

afterEach(() => jest.restoreAllMocks());

const renderPage = async () => {
  render(
    <MemoryRouter>
      <ManageUsers />
    </MemoryRouter>
  );
  await screen.findByText("asha");
};

test("shows each account's status with the matching action", async () => {
  await renderPage();
  expect(within(rowOf("asha")).getByText("Active")).toBeInTheDocument();
  expect(within(rowOf("asha")).getByRole("button", { name: "Deactivate" })).toBeInTheDocument();
  expect(within(rowOf("drold")).getByText("Deactivated")).toBeInTheDocument();
  expect(within(rowOf("drold")).getByRole("button", { name: "Reactivate" })).toBeInTheDocument();
});

test("deactivate and reactivate call the server and reload the list", async () => {
  await renderPage();

  fireEvent.click(within(rowOf("asha")).getByRole("button", { name: "Deactivate" }));
  await waitFor(() => expect(adminApi.deactivateUser).toHaveBeenCalledWith(2));

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

  await waitFor(() =>
    expect(window.alert).toHaveBeenCalledWith(expect.stringContaining("Deactivate it instead"))
  );
});
