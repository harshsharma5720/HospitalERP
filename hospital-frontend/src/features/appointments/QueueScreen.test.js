import { act, render, screen, waitFor, within } from "@testing-library/react";
import QueueScreen from "./QueueScreen";
import { getQueueScreen } from "./api";

jest.mock("./api");

const screenOf = (doctors) => ({ data: { date: "2026-10-11", doctors } });
const RAO = { doctorName: "Anita Rao", specialist: "CARDIOLOGY", nowServing: 4, next: [6, 7, 9] };
const SHAH = { doctorName: "Vikram Shah", specialist: null, nowServing: null, next: [2] };

const card = (name) => screen.getByRole("listitem", { name });

afterEach(() => {
  jest.useRealTimers();
  jest.resetAllMocks();
});

test("per doctor: the token being served and the next ones - no names, no login", async () => {
  getQueueScreen.mockResolvedValue(screenOf([RAO, SHAH]));
  render(<QueueScreen />);

  const rao = await screen.findByRole("listitem", { name: "Dr. Anita Rao" });
  expect(within(rao).getByText("Cardiology")).toBeInTheDocument();
  expect(within(rao).getByLabelText("Now serving")).toHaveTextContent("4");
  expect(within(within(rao).getByLabelText("Next")).getAllByText(/^\d+$/).map((t) => t.textContent)).toEqual(["6", "7", "9"]);
  // Nobody called in yet
  expect(within(card("Dr. Vikram Shah")).getByLabelText("Now serving")).toHaveTextContent("—");
  expect(screen.getByLabelText("Time")).toHaveTextContent(/^\d\d:\d\d$/);
});

test("an empty waiting room says so", async () => {
  getQueueScreen.mockResolvedValue(screenOf([]));
  render(<QueueScreen />);

  expect(await screen.findByText("No patients in the queue right now.")).toBeInTheDocument();
});

test("refreshes every 5 seconds; a doctor whose token changed is highlighted and announced", async () => {
  jest.useFakeTimers();
  getQueueScreen.mockResolvedValue(screenOf([RAO, SHAH]));
  render(<QueueScreen />);
  await screen.findByRole("listitem", { name: "Dr. Anita Rao" });
  expect(card("Dr. Anita Rao")).toHaveAttribute("data-just-called", "false"); // nothing to compare with yet

  getQueueScreen.mockResolvedValue(screenOf([{ ...RAO, nowServing: 6, next: [7, 9] }, SHAH]));
  act(() => {
    jest.advanceTimersByTime(5000);
  });

  await waitFor(() => expect(within(card("Dr. Anita Rao")).getByLabelText("Now serving")).toHaveTextContent("6"));
  expect(card("Dr. Anita Rao")).toHaveAttribute("data-just-called", "true");
  expect(card("Dr. Vikram Shah")).toHaveAttribute("data-just-called", "false");
  expect(screen.getByText("Dr. Anita Rao: now serving 6")).toBeInTheDocument();
  expect(getQueueScreen).toHaveBeenCalledTimes(2);
});

test("when the server can't be reached the last tokens stay up", async () => {
  jest.useFakeTimers();
  getQueueScreen.mockResolvedValueOnce(screenOf([RAO]));
  render(<QueueScreen />);
  await screen.findByRole("listitem", { name: "Dr. Anita Rao" });

  getQueueScreen.mockRejectedValue(new Error("Network Error"));
  act(() => {
    jest.advanceTimersByTime(5000);
  });

  expect(await screen.findByText("Reconnecting…")).toBeInTheDocument();
  expect(within(card("Dr. Anita Rao")).getByLabelText("Now serving")).toHaveTextContent("4");
});
