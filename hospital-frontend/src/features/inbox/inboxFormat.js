import { formatDate } from "../appointments/appointmentFormat";
import { toLocalISODate } from "../../shared/utils/dateUtils";

// When a message came: "Just now", "5 min ago", "Today, 10:05", "Yesterday, 10:05", "Mon 12 Oct, 10:05"
// (createdAt is the server's local time without a zone, "2026-10-19T10:05:31.123456")
export const whenLabel = (createdAt, now = new Date()) => {
  const at = new Date(createdAt);
  const minutes = Math.floor((now - at) / 60000);
  if (minutes < 1) return "Just now";
  if (minutes < 60) return `${minutes} min ago`;
  const time = createdAt.slice(11, 16);
  const day = createdAt.slice(0, 10);
  if (day === toLocalISODate(now)) return `Today, ${time}`;
  const yesterday = new Date(now);
  yesterday.setDate(yesterday.getDate() - 1);
  if (day === toLocalISODate(yesterday)) return `Yesterday, ${time}`;
  return `${formatDate(day)}, ${time}`;
};

// The badge: nothing at 0, "9+" above 9
export const badgeLabel = (unread) => (unread > 9 ? "9+" : String(unread));
