import { render, screen, fireEvent } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import RegisterUser from "./RegisterUser";
import * as adminApi from "./api";
import { ToastProvider } from "../../shared/ui";

jest.mock("./api");

const renderPage = () =>
  render(
    <ToastProvider>
      <MemoryRouter initialEntries={["/admin/register-user"]}>
        <Routes>
          <Route path="/admin/register-user" element={<RegisterUser />} />
          <Route path="/admin/manage-users" element={<p>users page</p>} />
        </Routes>
      </MemoryRouter>
    </ToastProvider>
  );

const fill = (label, value) => fireEvent.change(screen.getByLabelText(label), { target: { value } });

afterEach(() => jest.resetAllMocks());

test("registers a staff account and goes back to the users", async () => {
  adminApi.createUser.mockResolvedValue({ data: {} });
  renderPage();
  fill("Role", "RECEPTIONIST");
  fill("Username", " desk2 ");
  fill("Email", "desk2@example.com");
  fill("Phone number", "9876500002");
  fill("Password", "s3cret-pass");
  fireEvent.click(screen.getByRole("button", { name: "Register" }));

  expect(await screen.findByText("users page")).toBeInTheDocument();
  expect(adminApi.createUser).toHaveBeenCalledWith({
    username: "desk2", email: "desk2@example.com", phoneNumber: "9876500002", password: "s3cret-pass", role: "RECEPTIONIST",
  });
  expect(screen.getByRole("status")).toHaveTextContent("desk2 registered.");
});

test("without a role nothing is sent", async () => {
  renderPage();
  fill("Username", "desk2");
  fill("Email", "desk2@example.com");
  fill("Password", "s3cret-pass");
  fireEvent.click(screen.getByRole("button", { name: "Register" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("Please select a role");
  expect(adminApi.createUser).not.toHaveBeenCalled();
});

test("a refused registration stays on the form and says why", async () => {
  adminApi.createUser.mockRejectedValue({ response: { data: { message: "Username is already taken" } } });
  renderPage();
  fill("Role", "DOCTOR");
  fill("Username", "rao");
  fill("Email", "rao@example.com");
  fill("Password", "s3cret-pass");
  fireEvent.click(screen.getByRole("button", { name: "Register" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("Username is already taken");
  expect(screen.getByLabelText("Username")).toHaveValue("rao");
});
