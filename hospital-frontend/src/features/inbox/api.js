import axios from "axios";
import { API_BASE_URL } from "../../app/config";

// The logged-in user's notification centre (docs/NOTIFICATION_CENTRE_PLAN.md). A message: { id, kind, title, body,
// link (a page of the app, or null), createdAt, read }. The login token is added by app/setupAxios.
const INBOX = `${API_BASE_URL}/api/inbox`;

// The newest 50 of the last 90 days, newest first: { messages, unread }
export const getInbox = () => axios.get(INBOX);

// { unread } - asked every 30 seconds
export const getUnreadCount = () => axios.get(`${INBOX}/unread-count`);

export const markRead = (messageId) => axios.post(`${INBOX}/${messageId}/read`);

export const markAllRead = () => axios.post(`${INBOX}/read-all`);
