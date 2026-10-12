import { render, screen } from "@testing-library/react";
import App from "./App";
import { getQueueScreen } from "../features/appointments/api";

jest.mock("../features/appointments/api");
// The animations need a canvas, which the test browser has not got
jest.mock("lottie-react", () => () => null);

afterEach(() => {
  window.history.pushState({}, "", "/");
  jest.resetAllMocks();
});

test("the waiting-room screen opens without a login, without the site's footer", async () => {
  localStorage.clear();
  getQueueScreen.mockResolvedValue({ data: { date: "2026-10-11", doctors: [] } });
  window.history.pushState({}, "", "/queue-screen");

  render(<App />);

  expect(await screen.findByText("No patients in the queue right now.")).toBeInTheDocument();
  expect(screen.getByRole("heading", { name: "Now serving" })).toBeInTheDocument();
  expect(screen.queryByRole("contentinfo")).not.toBeInTheDocument(); // no site footer on the TV
});
