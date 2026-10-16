import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import AdminDoctorAnalytics from "./AdminDoctorAnalytics";

// The analytics are tested in DoctorAnalytics.test.js; here: which doctor, and the page's title once it is known
jest.mock("../doctors/analytics/DoctorAnalytics", () => {
  const React = require("react");
  return {
    __esModule: true,
    default: ({ doctorId, onDoctor }) => {
      React.useEffect(() => onDoctor({ id: Number(doctorId), name: "Anita Rao", specialist: "CARDIOLOGY" }), [doctorId, onDoctor]);
      return React.createElement("div", { "data-testid": "analytics", "data-doctor": doctorId });
    },
  };
});

test("a doctor's analytics for an admin: the doctor from the address, named once loaded, and the way back", async () => {
  render(
    <MemoryRouter initialEntries={["/admin/doctor/7/analytics"]}>
      <Routes>
        <Route path="/admin/doctor/:doctorId/analytics" element={<AdminDoctorAnalytics />} />
      </Routes>
    </MemoryRouter>
  );
  expect(screen.getByTestId("analytics")).toHaveAttribute("data-doctor", "7");
  expect(await screen.findByRole("heading", { level: 1, name: "Dr. Anita Rao" })).toBeInTheDocument();
  expect(screen.getByText("Cardiology · Analytics")).toBeInTheDocument();
  expect(screen.getByRole("link", { name: "← Doctors" })).toHaveAttribute("href", "/admin/manage-doctor");
});
