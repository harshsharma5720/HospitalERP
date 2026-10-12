import { render, screen, fireEvent } from "@testing-library/react";
import ReceptionistProfile from "./ReceptionistProfile";
import useAuthStore from "../auth/useAuthStore";
import { getReceptionist, updateReceptionist } from "../../shared/profileApi";
import { ToastProvider } from "../../shared/ui";

jest.mock("../../shared/profileApi");

const readBlob = (blob) =>
  new Promise((resolve) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.readAsText(blob);
  });

const PROFILE = { id: 2, name: "Ravi Desk", userName: "desk1", email: "desk1@example.com", phone: "9000000001", gender: "MALE", age: 29 };

beforeEach(() => {
  useAuthStore.setState({ token: "t", role: "ROLE_RECEPTIONIST", userId: 5 });
  getReceptionist.mockResolvedValue({ data: PROFILE });
});
afterEach(() => jest.resetAllMocks());

test("the receptionist sees and changes their own details", async () => {
  updateReceptionist.mockImplementation(async (userId, form) => ({ data: { ...PROFILE, ...JSON.parse(await readBlob(form.get("receptionistDTO"))) } }));
  render(
    <ToastProvider>
      <ReceptionistProfile />
    </ToastProvider>
  );
  const details = await screen.findByRole("region", { name: "Your details" });
  expect(getReceptionist).toHaveBeenCalledWith(5);
  expect(details).toHaveTextContent("desk1@example.com");
  expect(details).toHaveTextContent("Male");

  fireEvent.click(screen.getByRole("button", { name: "Edit" }));
  fireEvent.change(screen.getByLabelText("Phone number"), { target: { value: "9000000002" } });
  fireEvent.click(screen.getByRole("button", { name: "Save changes" }));
  expect(await screen.findByRole("status")).toHaveTextContent("Your profile is saved.");
  const [userId, form] = updateReceptionist.mock.calls[0];
  expect(userId).toBe(5);
  expect(JSON.parse(await readBlob(form.get("receptionistDTO")))).toMatchObject({ phone: "9000000002", name: "Ravi Desk" });
});
