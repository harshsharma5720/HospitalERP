import { render, screen, fireEvent } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import AddRelativePage from "./AddRelativePage";
import * as patientsApi from "./api";
import { ToastProvider } from "../../shared/ui";

jest.mock("./api");

const renderAt = (path, state) =>
  render(
    <ToastProvider>
      <MemoryRouter initialEntries={[{ pathname: path, state }]}>
        <Routes>
          <Route path="/add-relative" element={<AddRelativePage />} />
          <Route path="/edit-relative" element={<AddRelativePage />} />
          <Route path="/edit-profile" element={<p>my account</p>} />
          <Route path="/appointments" element={<p>booking page</p>} />
        </Routes>
      </MemoryRouter>
    </ToastProvider>
  );

const fill = (label, value) => fireEvent.change(screen.getByLabelText(label), { target: { value } });

afterEach(() => jest.resetAllMocks());

test("adding from the booking page goes back there", async () => {
  patientsApi.addRelative.mockResolvedValue({ data: {} });
  renderAt("/add-relative", { from: "/appointments" });
  fill("Full name", " Ravi Verma ");
  fill("Relationship", "FATHER");
  fill("Gender", "MALE");
  fill("Date of birth", "1958-01-01");
  fireEvent.click(screen.getByRole("button", { name: "Add family member" }));
  expect(await screen.findByText("booking page")).toBeInTheDocument();
  expect(patientsApi.addRelative).toHaveBeenCalledWith({
    name: "Ravi Verma", relationship: "FATHER", gender: "MALE", dob: "1958-01-01", patientAadharNo: null,
  });
  expect(screen.getByRole("status")).toHaveTextContent("Ravi Verma added.");
});

test("editing starts from their details and goes back to My account", async () => {
  patientsApi.updateRelative.mockResolvedValue({ data: {} });
  renderAt("/edit-relative", {
    relative: { id: 5, name: "Ravi Verma", relationship: "FATHER", gender: "MALE", dob: "1958-01-01", patientAadharNo: 123456789012 },
  });
  expect(screen.getByRole("heading", { name: "Edit Ravi Verma" })).toBeInTheDocument();
  expect(screen.getByLabelText("Aadhaar number (optional)")).toHaveValue("123456789012");
  fill("Date of birth", "1957-12-31");
  fireEvent.click(screen.getByRole("button", { name: "Save changes" }));
  expect(await screen.findByText("my account")).toBeInTheDocument();
  expect(patientsApi.updateRelative).toHaveBeenCalledWith(5, expect.objectContaining({ dob: "1957-12-31", patientAadharNo: 123456789012 }));
});

test("a refused save stays on the form and says why", async () => {
  patientsApi.addRelative.mockRejectedValue({ response: { data: { message: "Date of birth can't be in the future" } } });
  renderAt("/add-relative");
  fill("Full name", "Ravi Verma");
  fill("Relationship", "FATHER");
  fill("Gender", "MALE");
  fill("Date of birth", "1958-01-01");
  fireEvent.click(screen.getByRole("button", { name: "Add family member" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("Date of birth can't be in the future");
  expect(screen.getByLabelText("Full name")).toHaveValue("Ravi Verma");
  expect(screen.getByRole("link", { name: "Cancel" })).toHaveAttribute("href", "/edit-profile");
});
