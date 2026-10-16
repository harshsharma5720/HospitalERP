import axios from "axios";
import { getAnalytics, getEarnings } from "./api";

jest.mock("axios");

afterEach(() => jest.resetAllMocks());

test("the doctor's own figures, or any doctor's through the admin's address", () => {
  getAnalytics(30);
  getEarnings(7);
  getAnalytics(90, 12);
  getEarnings(30, 12);
  expect(axios.get.mock.calls.map(([url, options]) => `${url.replace(/^.*\/api/, "/api")} ${options.params.days}`)).toEqual([
    "/api/doctor/analytics 30",
    "/api/doctor/earnings 7",
    "/api/admin/doctors/12/analytics 90",
    "/api/admin/doctors/12/earnings 30",
  ]);
});
