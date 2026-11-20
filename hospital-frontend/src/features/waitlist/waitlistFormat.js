// How a wait reads in the app (docs/WAITLIST_PLAN.md)

export const WAIT_SHIFTS = [
  ["MORNING", "Morning"],
  ["EVENING", "Evening"],
  ["", "Any time that day"],
];

// "Morning", "Evening" or "Any time that day" (shift null: either)
export const shiftLabel = (shift) => (WAIT_SHIFTS.find(([value]) => value === (shift ?? "")) ?? WAIT_SHIFTS[2])[1];

// The booking page on the wait's doctor, day and shift (as the bell's message opens it)
export const bookingLink = (wait, shift = wait.shift) =>
  `/appointments?doctorId=${wait.doctorId}&date=${wait.date}${shift ? `&shift=${shift}` : ""}`;
