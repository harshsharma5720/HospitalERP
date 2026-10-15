import { dayLabel, deadlineText, doctorLabel, formatDate, formatTime, timeRange, visitStatus } from "./appointmentFormat";

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

test("each state has its badge tone", () => {
  const today = "2026-10-11";
  expect(visitStatus({ status: "SCHEDULED", date: "2026-10-12" }, today).tone).toBe("info");
  expect(visitStatus({ status: "COMPLETED", date: "2026-10-10" }, today).tone).toBe("success");
  expect(visitStatus({ status: "CONFIRMED", date: "2026-10-10" }, today).tone).toBe("warning");
  expect(visitStatus({ status: "CANCELLED_BY_PATIENT", date: "2026-10-12" }, today).tone).toBe("neutral");
});

test("a past appointment that was never completed is missed", () => {
  const today = "2026-10-11";
  expect(visitStatus({ status: "SCHEDULED", date: "2026-10-12" }, today).label).toBe("Scheduled");
  expect(visitStatus({ status: "SCHEDULED", date: "2026-10-11" }, today).label).toBe("Scheduled");
  expect(visitStatus({ status: "CONFIRMED", date: "2026-10-10" }, today).label).toBe("Missed");
  expect(visitStatus({ status: "COMPLETED", date: "2026-10-10" }, today).label).toBe("Completed");
  expect(visitStatus({ status: "CANCELLED_BY_DOCTOR", date: "2026-10-10" }, today).label).toBe("Cancelled by doctor");
});

test("doctors by name and speciality", () => {
  expect(doctorLabel({ name: "Anita Rao", specialist: "CARDIOLOGY" })).toBe("Dr. Anita Rao — Cardiology");
  expect(doctorLabel({ name: null, userName: "drnew", specialist: "NOT_ASSIGNED" })).toBe("Dr. drnew");
});

test("the change deadline reads as a time today or tomorrow, or a day and time", () => {
  const monday = new Date(2026, 9, 5, 12, 0);
  expect(deadlineText("2026-10-05T21:30:00", monday)).toBe("21:30 today");
  expect(deadlineText("2026-10-06T08:00:00", monday)).toBe("08:00 tomorrow");
  expect(deadlineText("2026-10-09T15:00:00", monday)).toBe("Fri 9 Oct, 15:00");
});
