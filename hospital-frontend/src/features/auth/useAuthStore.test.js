// The logged-in user in memory, kept alive by the refresh cookie (docs/SAFER_LOGIN_TOKENS_PLAN.md)
import axios from "axios";

const tokenFor = (role, userId, expSeconds) => `x.${btoa(JSON.stringify({ sub: "asha", role, userId, exp: expSeconds }))}.y`;
const inMinutes = (minutes) => Math.floor(Date.now() / 1000) + minutes * 60;

let posted; // messages to other tabs
let store;
let restoreSession;
beforeEach(() => {
  posted = [];
  global.BroadcastChannel = class {
    postMessage(message) {
      posted.push(message);
    }
  };
  jest.resetModules();
  ({ default: store, restoreSession } = require("./useAuthStore"));
  jest.spyOn(require("axios"), "post");
});
afterEach(() => {
  jest.useRealTimers();
  jest.restoreAllMocks();
  delete global.BroadcastChannel;
});

const post = () => require("axios").post;

test("on start the login is restored from the refresh cookie, and the old stored token is dropped", async () => {
  localStorage.setItem("jwtToken", "old");
  const token = tokenFor("ROLE_DOCTOR", 5, inMinutes(15));
  post().mockResolvedValue({ data: { token } });
  expect(store.getState().ready).toBe(false);

  await restoreSession();
  expect(post()).toHaveBeenCalledWith(
    expect.stringMatching(/\/api\/auth\/refresh$/),
    null,
    expect.objectContaining({ withCredentials: true, headers: { "X-Requested-With": "XMLHttpRequest" } })
  );
  expect(store.getState()).toMatchObject({ token, role: "ROLE_DOCTOR", userId: 5, ready: true });
  expect(localStorage.getItem("jwtToken")).toBeNull();
});

test("no login to restore: signed out, but ready", async () => {
  post().mockRejectedValue({ response: { status: 401 } });
  expect(await restoreSession()).toBeNull();
  expect(store.getState()).toMatchObject({ token: null, role: null, ready: true });
});

test("a login that ended elsewhere: the refresh is refused and this tab is signed out", async () => {
  store.getState().login(tokenFor("ROLE_PATIENT", 7, inMinutes(15)));
  post().mockRejectedValue({ response: { status: 401 } });
  expect(await store.getState().refresh()).toBeNull();
  expect(store.getState()).toMatchObject({ token: null, role: null, userId: null, ready: true });
});

test("refreshes needed at the same time share one request", async () => {
  let answer;
  post().mockReturnValue(new Promise((resolve) => (answer = resolve)));
  const first = store.getState().refresh();
  const second = store.getState().refresh();
  expect(post()).toHaveBeenCalledTimes(1);
  const token = tokenFor("ROLE_PATIENT", 7, inMinutes(15));
  answer({ data: { token } });
  expect(await first).toBe(token);
  expect(await second).toBe(token);
});

test("the access token is refreshed a minute before it runs out", async () => {
  jest.useFakeTimers();
  post().mockResolvedValue({ data: { token: tokenFor("ROLE_PATIENT", 7, inMinutes(30)) } });
  store.getState().login(tokenFor("ROLE_PATIENT", 7, inMinutes(15)));
  expect(posted).toEqual(["login"]);
  jest.advanceTimersByTime(13 * 60 * 1000);
  expect(post()).not.toHaveBeenCalled();
  jest.advanceTimersByTime(60 * 1000 + 10);
  expect(post()).toHaveBeenCalledWith(expect.stringMatching(/\/api\/auth\/refresh$/), null, expect.anything());
});

test("a token valid for years doesn't overflow the timer (which would refresh at once)", () => {
  jest.useFakeTimers();
  store.getState().login(tokenFor("ROLE_PATIENT", 7, inMinutes(60 * 24 * 365 * 70)));
  jest.advanceTimersByTime(60 * 1000);
  expect(post()).not.toHaveBeenCalled();
});

test("logout clears the login here, tells the other tabs and the server", async () => {
  post().mockResolvedValue({ data: {} });
  store.getState().login(tokenFor("ROLE_PATIENT", 7, inMinutes(15)));
  await store.getState().logout();
  expect(store.getState()).toMatchObject({ token: null, role: null, userId: null, ready: true });
  expect(posted).toEqual(["login", "logout"]);
  expect(post()).toHaveBeenCalledWith(expect.stringMatching(/\/api\/auth\/logout$/), null, expect.objectContaining({ withCredentials: true }));
});

test("news from another tab: a logout drops this tab's token, a login fetches its own", async () => {
  store.getState().login(tokenFor("ROLE_PATIENT", 7, inMinutes(15)));
  store.getState().heard("logout");
  expect(store.getState().token).toBeNull();
  expect(post()).not.toHaveBeenCalled(); // the other tab told the server already

  const token = tokenFor("ROLE_PATIENT", 7, inMinutes(15));
  post().mockResolvedValue({ data: { token } });
  store.getState().heard("login");
  await Promise.resolve();
  await Promise.resolve();
  expect(store.getState().token).toBe(token);
});

test("works without BroadcastChannel (older browsers)", () => {
  delete global.BroadcastChannel;
  jest.resetModules();
  const lonely = require("./useAuthStore").default;
  expect(() => lonely.getState().login(tokenFor("ROLE_PATIENT", 7, inMinutes(15)))).not.toThrow();
  expect(axios).toBeDefined();
});
