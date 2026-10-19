// jest-dom adds custom jest matchers for asserting on DOM nodes.
// allows you to do things like:
// expect(element).toHaveTextContent(/react/i)
// learn more: https://github.com/testing-library/jest-dom
import '@testing-library/jest-dom';

// react-router v7 needs TextEncoder, which jsdom (Jest 27) does not provide
import { TextEncoder, TextDecoder } from 'util';
if (!global.TextEncoder) global.TextEncoder = TextEncoder;
if (!global.TextDecoder) global.TextDecoder = TextDecoder;

// The notification centre's bell is in every header (docs/NOTIFICATION_CENTRE_PLAN.md): no test calls the server for
// it, and a test that resets its mocks still gets an empty inbox. InboxBell.test.js sets its own answers.
jest.mock('./features/inbox/api');
const inboxApi = require('./features/inbox/api');
beforeEach(() => {
  inboxApi.getUnreadCount.mockResolvedValue({ data: { unread: 0 } });
  inboxApi.getInbox.mockResolvedValue({ data: { messages: [], unread: 0 } });
  inboxApi.markRead.mockResolvedValue({ data: {} });
  inboxApi.markAllRead.mockResolvedValue({ data: { read: 0 } });
});
