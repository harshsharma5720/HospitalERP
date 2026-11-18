import { bookingLink, shiftLabel } from "./waitlistFormat";

// How a wait reads (docs/WAITLIST_PLAN.md)

test("the shift waited for", () => {
  expect(shiftLabel("MORNING")).toBe("Morning");
  expect(shiftLabel("EVENING")).toBe("Evening");
  expect(shiftLabel(null)).toBe("Any time that day");
  expect(shiftLabel(undefined)).toBe("Any time that day");
});

test("the booking page on the wait's doctor, day and shift", () => {
  expect(bookingLink({ doctorId: 3, date: "2026-11-20", shift: "EVENING" })).toBe("/appointments?doctorId=3&date=2026-11-20&shift=EVENING");
  expect(bookingLink({ doctorId: 3, date: "2026-11-20", shift: null })).toBe("/appointments?doctorId=3&date=2026-11-20");
});
