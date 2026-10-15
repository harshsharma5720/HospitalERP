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

// The patient's change deadline (docs/CANCELLATION_CUTOFF_PLAN.md), "2026-10-15T08:00:00" -> "08:00 today",
// "08:00 tomorrow" or "Fri 9 Oct, 08:00"
export const deadlineText = (dateTime, today = new Date()) => {
  const [date, time] = dateTime.split("T");
  const day = dayLabel(date, today);
  return day === "Today" || day === "Tomorrow" ? `${formatTime(time)} ${day.toLowerCase()}` : `${day}, ${formatTime(time)}`;
};

// "10:00–10:30", or "—" for a visit without its slot (old data)
export const timeRange = (appointment) =>
  appointment.startTime ? `${formatTime(appointment.startTime)}–${formatTime(appointment.endTime)}` : "—";

// "CARDIOLOGY" -> "Cardiology"; "" when none is set
export const specialityLabel = (specialist) => {
  if (!specialist || specialist === "NOT_ASSIGNED") return "";
  const words = specialist.toLowerCase().replace(/_/g, " ");
  return `${words.charAt(0).toUpperCase()}${words.slice(1)}`;
};

// "Dr. Anita Rao — Cardiology" (the user name when the profile has no name yet; no speciality when none is set)
export const doctorLabel = (doctor) => {
  const name = `Dr. ${doctor.name || doctor.userName}`;
  const speciality = specialityLabel(doctor.specialist);
  return speciality ? `${name} — ${speciality}` : name;
};

const STATUS_LABELS = {
  SCHEDULED: "Scheduled",
  CONFIRMED: "Confirmed",
  COMPLETED: "Completed",
  CANCELLED_BY_PATIENT: "Cancelled by patient",
  CANCELLED_BY_DOCTOR: "Cancelled by doctor",
};

// Badge tones (shared/ui/Badge): the badge always carries its label; the colour only supports it
const TONES = {
  upcoming: "info",
  completed: "success",
  missed: "warning",
  cancelled: "neutral",
};

// What the front desk calls an appointment's state: a past appointment that was never completed is "Missed"
export const visitStatus = (appointment, today = toLocalISODate()) => {
  const { status, date } = appointment;
  if (status === "COMPLETED") return { label: STATUS_LABELS.COMPLETED, tone: TONES.completed };
  if (status?.startsWith("CANCELLED")) return { label: STATUS_LABELS[status], tone: TONES.cancelled };
  if (date < today) return { label: "Missed", tone: TONES.missed };
  return { label: STATUS_LABELS[status] ?? status, tone: TONES.upcoming };
};
