import { addDays, toLocalISODate } from "../../shared/utils/dateUtils";

// Dates, times and statuses of appointments as the front-desk pages show them.

const DAYS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
const MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

// "2026-10-09" -> "Fri 9 Oct" (+ " 2026" with the year)
export const formatDate = (isoDate, withYear = false) => {
  const [year, month, day] = isoDate.split("-").map(Number);
  const weekday = DAYS[new Date(year, month - 1, day).getDay()];
  return `${weekday} ${day} ${MONTHS[month - 1]}${withYear ? ` ${year}` : ""}`;
};

// "Today", "Tomorrow" or "Fri 9 Oct"
export const dayLabel = (isoDate, today = new Date()) => {
  if (isoDate === toLocalISODate(today)) return "Today";
  if (isoDate === toLocalISODate(addDays(today, 1))) return "Tomorrow";
  return formatDate(isoDate);
};

// "10:00:00" -> "10:00"; "" when unknown
export const formatTime = (time) => (time ? String(time).slice(0, 5) : "");

// "10:00–10:30", or "—" for a visit without its slot (old data)
export const timeRange = (appointment) =>
  appointment.startTime ? `${formatTime(appointment.startTime)}–${formatTime(appointment.endTime)}` : "—";

const STATUS_LABELS = {
  SCHEDULED: "Scheduled",
  CONFIRMED: "Confirmed",
  COMPLETED: "Completed",
  CANCELLED_BY_PATIENT: "Cancelled by patient",
  CANCELLED_BY_DOCTOR: "Cancelled by doctor",
};

// The badge always carries its label; the color only supports it
const STYLES = {
  upcoming: "bg-blue-100 text-blue-800",
  completed: "bg-green-100 text-green-800",
  missed: "bg-amber-100 text-amber-800",
  cancelled: "bg-gray-200 text-gray-700",
};

// What the front desk calls an appointment's state: a past appointment that was never completed is "Missed"
export const visitStatus = (appointment, today = toLocalISODate()) => {
  const { status, date } = appointment;
  if (status === "COMPLETED") return { label: STATUS_LABELS.COMPLETED, style: STYLES.completed };
  if (status?.startsWith("CANCELLED")) return { label: STATUS_LABELS[status], style: STYLES.cancelled };
  if (date < today) return { label: "Missed", style: STYLES.missed };
  return { label: STATUS_LABELS[status] ?? status, style: STYLES.upcoming };
};
