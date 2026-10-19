import { act, render, screen, fireEvent, within } from "@testing-library/react";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import InboxBell, { POLL_MS } from "./InboxBell";
import { badgeLabel, whenLabel } from "./inboxFormat";
import * as inboxApi from "./api"; // mocked for every test in setupTests.js
import useAuthStore from "../auth/useAuthStore";

function Address() {
  return <p data-testid="address">{useLocation().pathname}</p>;
}

const renderBell = () =>
  render(
    <MemoryRouter initialEntries={["/"]}>
      <Routes>
        <Route
          path="*"
          element={
            <>
              <InboxBell />
              <Address />
            </>
          }
        />
      </Routes>
    </MemoryRouter>
  );

const iso = (date) => {
  const pad = (n) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:00`;
};
const minutesAgo = (n) => iso(new Date(Date.now() - n * 60000));
const MESSAGES = [
  { id: 3, kind: "BILL_READY", title: "Your bill is ready", body: "B-2026-000042: ₹500.00.", link: "/edit-profile", createdAt: minutesAgo(2), read: false },
  { id: 2, kind: "VISIT_BOOKED", title: "Appointment booked", body: "Dr. Anita Rao, Tue 21 Oct.", link: "/appointment-details", createdAt: minutesAgo(30), read: false },
  { id: 1, kind: "LEAVE_REJECTED", title: "Your leave was not approved", body: "15–17 Oct.", link: null, createdAt: minutesAgo(0), read: true },
];

const bell = () => screen.getByRole("button", { name: /^Notifications/ });
const flush = () => act(async () => {});

beforeEach(() => useAuthStore.setState({ token: "t", role: "ROLE_PATIENT", userId: 7 }));
afterEach(() => {
  jest.useRealTimers();
  useAuthStore.setState({ token: null, role: null, userId: null });
});

test("the unread count, asked every 30 seconds and when the tab is shown again; nothing when logged out", async () => {
  jest.useFakeTimers();
  inboxApi.getUnreadCount
    .mockResolvedValueOnce({ data: { unread: 0 } })
    .mockResolvedValueOnce({ data: { unread: 3 } })
    .mockResolvedValue({ data: { unread: 12 } });
  renderBell();
  await flush();
  expect(bell()).toHaveAccessibleName("Notifications");
  expect(within(bell()).queryByText("0")).not.toBeInTheDocument();

  await act(async () => {
    jest.advanceTimersByTime(POLL_MS);
  });
  expect(bell()).toHaveAccessibleName("Notifications, 3 unread");
  expect(within(bell()).getByText("3")).toBeInTheDocument();
  expect(inboxApi.getUnreadCount).toHaveBeenCalledTimes(2);

  await act(async () => {
    document.dispatchEvent(new Event("visibilitychange"));
  });
  expect(inboxApi.getUnreadCount).toHaveBeenCalledTimes(3);
  expect(within(bell()).getByText("9+")).toBeInTheDocument();

  await act(async () => {
    useAuthStore.setState({ token: null, role: null, userId: null });
  });
  expect(screen.queryByRole("button", { name: /^Notifications/ })).not.toBeInTheDocument();
  await act(async () => {
    jest.advanceTimersByTime(POLL_MS * 3);
  });
  expect(inboxApi.getUnreadCount).toHaveBeenCalledTimes(3);
});

test("the panel: newest first, unread ones marked; a message opens its page and is read; all can be marked read", async () => {
  inboxApi.getUnreadCount.mockResolvedValue({ data: { unread: 2 } });
  inboxApi.getInbox.mockResolvedValue({ data: { messages: MESSAGES, unread: 2 } });
  renderBell();
  await flush();
  fireEvent.click(bell());
  const panel = await screen.findByRole("dialog", { name: "Notifications" });
  expect(bell()).toHaveAttribute("aria-expanded", "true");
  const items = await within(panel).findAllByRole("button", { name: /bill|Appointment|leave/ });
  expect(items.map((item) => item.textContent)).toEqual([
    "New: Your bill is readyB-2026-000042: ₹500.00.2 min ago",
    "New: Appointment bookedDr. Anita Rao, Tue 21 Oct.30 min ago",
    "Your leave was not approved15–17 Oct.Just now",
  ]);

  fireEvent.click(items[1]);
  expect(inboxApi.markRead).toHaveBeenCalledWith(2);
  expect(screen.getByTestId("address")).toHaveTextContent("/appointment-details");
  expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  expect(bell()).toHaveAccessibleName("Notifications, 1 unread");

  inboxApi.getInbox.mockResolvedValue({ data: { messages: MESSAGES.map((m) => (m.id === 2 ? { ...m, read: true } : m)), unread: 1 } });
  fireEvent.click(bell());
  const again = await screen.findByRole("dialog", { name: "Notifications" });
  await within(again).findByText("Your bill is ready");
  fireEvent.click(within(again).getByRole("button", { name: "Mark all as read" }));
  expect(inboxApi.markAllRead).toHaveBeenCalledTimes(1);
  expect(bell()).toHaveAccessibleName("Notifications");
  expect(within(again).getByRole("button", { name: "Mark all as read" })).toBeDisabled();
  expect(within(again).queryByText(/^New:/)).not.toBeInTheDocument();

  // A message without a page only closes the panel
  fireEvent.click(within(again).getByRole("button", { name: /Your leave was not approved/ }));
  expect(screen.getByTestId("address")).toHaveTextContent("/appointment-details");
  expect(inboxApi.markRead).toHaveBeenCalledTimes(1); // it was read already
});

test("Escape and a click outside close the panel; an empty inbox and one that can't be loaded say so", async () => {
  renderBell();
  await flush();
  fireEvent.click(bell());
  expect(await screen.findByText("No notifications yet.")).toBeInTheDocument();
  fireEvent.keyDown(document, { key: "Escape" });
  expect(screen.queryByRole("dialog")).not.toBeInTheDocument();

  inboxApi.getInbox.mockRejectedValueOnce({ response: { status: 500 } });
  fireEvent.click(bell());
  expect(await screen.findByText("The notifications couldn't be loaded. Please try again.")).toBeInTheDocument();
  fireEvent.mouseDown(screen.getByTestId("address"));
  expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
});

test("when a message came, and the badge", () => {
  const now = new Date(2026, 9, 19, 18, 30);
  expect(whenLabel("2026-10-19T18:29:40", now)).toBe("Just now");
  expect(whenLabel("2026-10-19T18:25:00", now)).toBe("5 min ago");
  expect(whenLabel("2026-10-19T10:05:31.123456", now)).toBe("Today, 10:05");
  expect(whenLabel("2026-10-18T10:05:00", now)).toBe("Yesterday, 10:05");
  expect(whenLabel("2026-10-12T10:05:00", now)).toBe("Mon 12 Oct, 10:05");
  expect(badgeLabel(1)).toBe("1");
  expect(badgeLabel(9)).toBe("9");
  expect(badgeLabel(10)).toBe("9+");
});
