import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import axios from "axios";
import DoctorSchedulePage from "./DoctorSchedulePage";
import useAuthStore from "../../Store/useAuthStore";

jest.mock("axios");

const DAYS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];
const defaultWeek = DAYS.flatMap((day) => [
  { dayOfWeek: day, shift: "MORNING", working: true, startTime: "09:00:00", endTime: "12:00:00", slotMinutes: 10, usingDefault: true },
  { dayOfWeek: day, shift: "EVENING", working: true, startTime: "15:00:00", endTime: "19:00:00", slotMinutes: 10, usingDefault: true },
]);

beforeEach(() => {
  useAuthStore.setState({ token: "t", role: "ROLE_DOCTOR", userId: 5 });
  axios.get.mockResolvedValue({ data: defaultWeek });
});

afterEach(() => jest.resetAllMocks());

test("loads the week and saves a day off", async () => {
  axios.put.mockImplementation((url, body) => Promise.resolve({ data: body }));
  render(<DoctorSchedulePage />);

  const sundayEvening = await screen.findByLabelText("Sunday evening working");
  expect(axios.get).toHaveBeenCalledWith(expect.stringContaining("/api/doctor/5/schedule"));
  expect(screen.getAllByText("default")).toHaveLength(14);

  fireEvent.click(sundayEvening); // take Sunday evening off
  fireEvent.change(screen.getByLabelText("Monday morning slot length"), { target: { value: "15" } });
  fireEvent.click(screen.getByText("Save Schedule"));

  await waitFor(() => expect(axios.put).toHaveBeenCalled());
  const [url, payload] = axios.put.mock.calls[0];
  expect(url).toContain("/api/doctor/5/schedule");
  expect(payload).toHaveLength(14);
  expect(payload.find((e) => e.dayOfWeek === "SUNDAY" && e.shift === "EVENING")).toMatchObject({
    working: false,
    startTime: null,
    endTime: null,
  });
  expect(payload.find((e) => e.dayOfWeek === "MONDAY" && e.shift === "MORNING")).toMatchObject({
    slotMinutes: 15,
    startTime: "09:00",
  });
  expect(await screen.findByText(/Schedule saved/)).toBeInTheDocument();
});
