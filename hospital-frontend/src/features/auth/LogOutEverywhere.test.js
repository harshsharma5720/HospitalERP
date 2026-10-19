// "Log out of all devices" (docs/SAFER_LOGIN_TOKENS_PLAN.md)
import { render, screen, fireEvent, within } from "@testing-library/react";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import LogOutEverywhereCard from "./LogOutEverywhere";
import useAuthStore from "./useAuthStore";
import { ConfirmProvider, ToastProvider } from "../../shared/ui";

function Address() {
  const { pathname, search } = useLocation();
  return <p>{`${pathname}${search}`}</p>;
}

const renderCard = () =>
  render(
    <ToastProvider>
      <ConfirmProvider>
        <MemoryRouter initialEntries={["/edit-profile"]}>
          <Routes>
            <Route path="/edit-profile" element={<LogOutEverywhereCard />} />
            <Route path="/login" element={<Address />} />
          </Routes>
        </MemoryRouter>
      </ConfirmProvider>
    </ToastProvider>
  );

let logoutEverywhere;
beforeEach(() => {
  logoutEverywhere = jest.fn();
  useAuthStore.setState({ token: "t", role: "ROLE_PATIENT", userId: 7, logoutEverywhere });
});

const askToLogOut = () => {
  fireEvent.click(screen.getByRole("button", { name: "Log out of all devices" }));
  return screen.findByRole("dialog", { name: "Log out of all devices?" });
};

test("asks first, then logs out everywhere and the login page says so", async () => {
  logoutEverywhere.mockResolvedValue();
  renderCard();
  const dialog = await askToLogOut();
  expect(dialog).toHaveTextContent("You'll be logged out on every phone and computer where you're logged in, this one too.");
  fireEvent.click(within(dialog).getByRole("button", { name: "Log out everywhere" }));
  expect(await screen.findByText("/login?loggedOut=everywhere")).toBeInTheDocument();
  expect(logoutEverywhere).toHaveBeenCalledTimes(1);
});

test("Cancel changes nothing", async () => {
  renderCard();
  fireEvent.click(within(await askToLogOut()).getByRole("button", { name: "Cancel" }));
  expect(await screen.findByRole("button", { name: "Log out of all devices" })).toBeEnabled();
  expect(logoutEverywhere).not.toHaveBeenCalled();
});

test("when the server can't do it: says so, and the page stays", async () => {
  logoutEverywhere.mockRejectedValue({ response: { status: 503, data: {} } });
  renderCard();
  fireEvent.click(within(await askToLogOut()).getByRole("button", { name: "Log out everywhere" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("Couldn't log you out of all devices. Please try again.");
  expect(screen.getByRole("button", { name: "Log out of all devices" })).toBeEnabled();
});
