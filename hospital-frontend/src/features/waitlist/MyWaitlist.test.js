import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import MyWaitlist from "./MyWaitlist";
import * as waitlistApi from "./api";
import { ConfirmProvider, ToastProvider } from "../../shared/ui";

// The waitlist api is mocked for every test (setupTests.js)

const wait = (id, extra = {}) => ({
  id, patientId: 41, relativeId: null, name: "Asha Verma", patientName: "Asha Verma", doctorId: 3, doctorName: "Anita Rao",
  date: "2026-11-20", shift: "MORNING", status: "WAITING", toldAt: null, ...extra,
});

const renderCard = () =>
  render(
    <ToastProvider>
      <ConfirmProvider>
        <MemoryRouter>
          <MyWaitlist />
        </MemoryRouter>
      </ConfirmProvider>
    </ToastProvider>
  );

test("nothing at all without waits, or when they can't be loaded", async () => {
  const { unmount } = renderCard();
  await waitFor(() => expect(waitlistApi.getMyWaits).toHaveBeenCalled());
  expect(screen.queryByRole("region", { name: "On the waitlist" })).not.toBeInTheDocument();
  unmount();

  waitlistApi.getMyWaits.mockRejectedValueOnce({ response: { status: 500 } });
  renderCard();
  await waitFor(() => expect(waitlistApi.getMyWaits).toHaveBeenCalledTimes(2));
  expect(screen.queryByRole("region", { name: "On the waitlist" })).not.toBeInTheDocument();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});

test("each wait with its free times; a time that freed up says so; leaving asks first", async () => {
  waitlistApi.getMyWaits.mockResolvedValue({
    data: [wait(1), wait(2, { relativeId: 5, name: "Mira Verma", shift: null, toldAt: "2026-11-19T10:05:00" })],
  });
  waitlistApi.leaveWaitlist.mockResolvedValue({});
  renderCard();

  const card = await screen.findByRole("region", { name: "On the waitlist" });
  const [asha, mira] = within(card).getAllByRole("listitem");
  expect(asha).toHaveTextContent("Dr. Anita Rao · Fri 20 Nov 2026");
  expect(asha).toHaveTextContent("Morning · for Asha Verma");
  expect(asha).not.toHaveTextContent("A time freed up");
  expect(within(asha).getByRole("link", { name: "Free times" })).toHaveAttribute("href", "/appointments?doctorId=3&date=2026-11-20&shift=MORNING");
  expect(mira).toHaveTextContent("Any time that day · for Mira Verma");
  expect(mira).toHaveTextContent("A time freed up: see if it's still free");
  expect(within(mira).getByRole("link", { name: "Free times" })).toHaveAttribute("href", "/appointments?doctorId=3&date=2026-11-20");

  fireEvent.click(within(mira).getByRole("button", { name: "Leave the waitlist for Mira Verma with Dr. Anita Rao on Fri 20 Nov 2026" }));
  const dialog = screen.getByRole("dialog", { name: "Leave the waitlist?" });
  fireEvent.click(within(dialog).getByRole("button", { name: "Stay on it" }));
  await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
  expect(waitlistApi.leaveWaitlist).not.toHaveBeenCalled();

  fireEvent.click(within(mira).getByRole("button", { name: /Leave the waitlist for Mira Verma/ }));
  fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Leave" }));
  await waitFor(() => expect(within(card).getAllByRole("listitem")).toHaveLength(1));
  expect(waitlistApi.leaveWaitlist).toHaveBeenCalledWith(2);
  expect(screen.getByRole("status")).toHaveTextContent("You've left the waitlist.");
});

test("a refused leave says why and keeps the wait", async () => {
  waitlistApi.getMyWaits.mockResolvedValue({ data: [wait(1)] });
  waitlistApi.leaveWaitlist.mockRejectedValue({ response: { status: 409, data: { message: "This wait has already ended" } } });
  renderCard();
  fireEvent.click(await screen.findByRole("button", { name: /Leave the waitlist for Asha Verma/ }));
  fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Leave" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("This wait has already ended");
  expect(screen.getAllByRole("listitem")).toHaveLength(1);
});
