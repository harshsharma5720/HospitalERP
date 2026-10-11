import { dayLabel, formatDate, formatTime, timeRange, visitStatus } from "./appointmentFormat";

test("dates read as today, tomorrow, or weekday day month", () => {
  const monday = new Date(2026, 9, 5); // Mon 5 Oct 2026
  expect(dayLabel("2026-10-05", monday)).toBe("Today");
  expect(dayLabel("2026-10-06", monday)).toBe("Tomorrow");
  expect(dayLabel("2026-10-09", monday)).toBe("Fri 9 Oct");
  expect(formatDate("2026-10-09", true)).toBe("Fri 9 Oct 2026");
});

test("times without seconds; a visit without its slot shows a dash", () => {
  expect(formatTime("10:00:00")).toBe("10:00");
  expect(formatTime(null)).toBe("");
  expect(timeRange({ startTime: "16:00:00", endTime: "16:30:00" })).toBe("16:00–16:30");
  expect(timeRange({ startTime: null, endTime: null })).toBe("—");
});

test("a past appointment that was never completed is missed", () => {
  const today = "2026-10-11";
  expect(visitStatus({ status: "SCHEDULED", date: "2026-10-12" }, today).label).toBe("Scheduled");
  expect(visitStatus({ status: "SCHEDULED", date: "2026-10-11" }, today).label).toBe("Scheduled");
  expect(visitStatus({ status: "CONFIRMED", date: "2026-10-10" }, today).label).toBe("Missed");
  expect(visitStatus({ status: "COMPLETED", date: "2026-10-10" }, today).label).toBe("Completed");
  expect(visitStatus({ status: "CANCELLED_BY_DOCTOR", date: "2026-10-10" }, today).label).toBe("Cancelled by doctor");
});
