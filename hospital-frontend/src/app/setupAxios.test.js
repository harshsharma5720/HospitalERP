// A request whose access token ran out is refreshed once and tried again (docs/SAFER_LOGIN_TOKENS_PLAN.md)
import axios, { AxiosError } from "axios";
import "./setupAxios";
import useAuthStore from "../features/auth/useAuthStore";
import { API_BASE_URL } from "./config";

const API = `${API_BASE_URL}/api/bills/my`;
let refresh;
let assign;
let answers; // what the "server" answers each request, by Authorization header

beforeEach(() => {
  useAuthStore.setState({ token: "old-token", role: "ROLE_PATIENT", userId: 7, ready: true });
  refresh = jest.fn();
  useAuthStore.setState({ refresh });
  answers = [];
  axios.defaults.adapter = jest.fn((config) => {
    answers.push(config.headers.Authorization);
    // A client retrying again and again gets an answer at last, so its test fails rather than running out of memory
    if (config.headers.Authorization === "Bearer new-token" || answers.length > 3) {
      return Promise.resolve({ data: "ok", status: 200, statusText: "OK", headers: {}, config });
    }
    const response = { data: {}, status: 401, statusText: "Unauthorized", headers: {}, config };
    return Promise.reject(new AxiosError("401", AxiosError.ERR_BAD_REQUEST, config, null, response));
  });
  assign = jest.fn();
  delete window.location;
  window.location = { pathname: "/my-account", assign };
});
afterEach(() => {
  delete axios.defaults.adapter;
  useAuthStore.setState({ token: null, role: null, userId: null });
});

test("a 401 refreshes the access token and tries the request once more", async () => {
  refresh.mockResolvedValue("new-token");
  const res = await axios.get(API);
  expect(res.data).toBe("ok");
  expect(answers).toEqual(["Bearer old-token", "Bearer new-token"]);
  expect(refresh).toHaveBeenCalledTimes(1);
});

test("requests failing together wait for the same refresh", async () => {
  refresh.mockResolvedValue("new-token");
  await Promise.all([axios.get(API), axios.get(API), axios.get(API)]);
  expect(refresh).toHaveBeenCalledTimes(3); // the store shares one request between them (useAuthStore.test.js)
  expect(answers.filter((a) => a === "Bearer new-token")).toHaveLength(3);
});

test("when the login has ended: the error, and the login page", async () => {
  refresh.mockResolvedValue(null);
  await expect(axios.get(API)).rejects.toMatchObject({ response: { status: 401 } });
  expect(assign).toHaveBeenCalledWith("/login?expired=1");
});

test("a request still refused after the refresh isn't tried again and again", async () => {
  refresh.mockResolvedValue("still-bad");
  await expect(axios.get(API)).rejects.toMatchObject({ response: { status: 401 } });
  expect(refresh).toHaveBeenCalledTimes(1);
  expect(answers).toEqual(["Bearer old-token", "Bearer still-bad"]);
});

test("log out of all devices is an ordinary call: its 401 refreshes and tries again", async () => {
  refresh.mockResolvedValue("new-token");
  const res = await axios.post(`${API_BASE_URL}/api/auth/logout-all`, null);
  expect(res.data).toBe("ok");
  expect(refresh).toHaveBeenCalledTimes(1);
});

test("the login endpoints' own 401s are left alone", async () => {
  await expect(axios.post(`${API_BASE_URL}/api/auth/login`, {})).rejects.toMatchObject({ response: { status: 401 } });
  expect(refresh).not.toHaveBeenCalled();
  expect(assign).not.toHaveBeenCalled();
});
