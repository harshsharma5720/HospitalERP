import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import Fees from "./Fees";
import * as adminApi from "./api";

jest.mock("./api");

const RAO = { doctorId: 3, doctorName: "Anita Rao", specialist: "CARDIOLOGY", active: true, consultationFee: 500 };
const SHAH = { doctorId: 4, doctorName: "Vikram Shah", specialist: "NOT_ASSIGNED", active: true, consultationFee: null };
const GONE = { doctorId: 5, doctorName: "Old Doctor", specialist: null, active: false, consultationFee: 300 };

const rowOf = (name) => screen.getByText(`Dr. ${name}`).closest("tr");

beforeEach(() => adminApi.getFees.mockResolvedValue({ data: { defaultFee: 250, doctors: [RAO, SHAH, GONE] } }));
afterEach(() => jest.resetAllMocks());

test("every doctor with their fee; the default fee for those without", async () => {
  render(<Fees />);

  expect(await screen.findByLabelText("Fee of Dr. Anita Rao")).toHaveValue(500);
  expect(rowOf("Anita Rao")).toHaveTextContent("Cardiology");
  expect(screen.getByLabelText("Fee of Dr. Vikram Shah")).toHaveValue(null);
  expect(rowOf("Vikram Shah")).toHaveTextContent("Not set: the default fee applies");
  expect(rowOf("Old Doctor")).toHaveTextContent("Deactivated");
  expect(screen.getByText(/the default fee of ₹250\.00/)).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Save the fee of Dr. Anita Rao" })).toBeDisabled(); // nothing changed
});

test("a changed fee is saved", async () => {
  adminApi.setFee.mockResolvedValue({ data: { ...SHAH, consultationFee: 1250.5 } });
  render(<Fees />);

  fireEvent.change(await screen.findByLabelText("Fee of Dr. Vikram Shah"), { target: { value: "1250.50" } });
  fireEvent.click(screen.getByRole("button", { name: "Save the fee of Dr. Vikram Shah" }));

  await waitFor(() => expect(adminApi.setFee).toHaveBeenCalledWith(4, 1250.5));
  expect(await screen.findByRole("status")).toHaveTextContent("Fee of Dr. Vikram Shah saved: ₹1,250.50.");
  expect(within(rowOf("Vikram Shah")).queryByText(/Not set/)).not.toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Save the fee of Dr. Vikram Shah" })).toBeDisabled();
});

test("the server's message when a fee is refused, and an empty fee isn't sent", async () => {
  adminApi.setFee.mockRejectedValue({
    response: { status: 400, data: { message: "The consultation fee can have at most 2 decimals (paise)" } },
  });
  render(<Fees />);

  fireEvent.change(await screen.findByLabelText("Fee of Dr. Anita Rao"), { target: { value: "12.345" } });
  fireEvent.click(screen.getByRole("button", { name: "Save the fee of Dr. Anita Rao" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("at most 2 decimals");

  fireEvent.change(screen.getByLabelText("Fee of Dr. Anita Rao"), { target: { value: "" } });
  fireEvent.click(screen.getByRole("button", { name: "Save the fee of Dr. Anita Rao" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("Please enter the fee of Dr. Anita Rao.");
  expect(adminApi.setFee).toHaveBeenCalledTimes(1);
});
