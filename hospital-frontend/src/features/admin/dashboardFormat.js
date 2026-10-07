// Formatting for the admin dashboard. Dates arrive as "yyyy-MM-dd" (server's day) and are formatted without
// time zones, so a day never shifts.

const MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
const WEEKDAYS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

const parts = (isoDay) => {
  const [year, month, day] = String(isoDay).slice(0, 10).split("-").map(Number);
  return { year, month, day };
};

// "2026-10-06" -> "6 Oct 2026"
export const formatDay = (isoDay) => {
  const { year, month, day } = parts(isoDay);
  return `${day} ${MONTHS[month - 1]} ${year}`;
};

// "2026-10-06" -> "Tue 6 Oct" (chart axis)
export const formatShortDay = (isoDay) => {
  const { year, month, day } = parts(isoDay);
  const weekday = WEEKDAYS[new Date(Date.UTC(year, month - 1, day)).getUTCDay()];
  return `${weekday} ${day} ${MONTHS[month - 1]}`;
};

// 1234567 -> "12,34,567" (Indian grouping)
export const formatCount = (n) => Number(n ?? 0).toLocaleString("en-IN");

// 27.3 -> "27.3%"
export const formatPercent = (p) => `${Number(p ?? 0).toLocaleString("en-IN", { maximumFractionDigits: 1 })}%`;

// "NOT_ASSIGNED" -> "Not assigned", "CARDIOLOGY" -> "Cardiology"
export const formatSpecialization = (value) => {
  if (!value) return "Not assigned";
  const words = String(value).toLowerCase().replace(/_/g, " ");
  return words.charAt(0).toUpperCase() + words.slice(1);
};
