import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import BillsPage from "./BillsPage";
import { getBills } from "./api";
import { toLocalISODate } from "../../shared/utils/dateUtils";

jest.mock("./api");

const TODAY = toLocalISODate();

const bill = (id, patientName, status, total, extra = {}) => ({
  id, billNumber: `B-2026-0000${id}`, appointmentId: 100 + id, patientId: 40 + id, patientName, doctorName: "Anita Rao",
  visitDate: TODAY, status, total, ...extra,
});
const pageOf = (entries, { totalPages = 1, totalEntries = entries.length } = {}) => ({
  data: { entries, page: 0, size: 20, totalEntries, totalPages },
});

function Address() {
  const location = useLocation();
  return <div data-testid="address">{location.pathname + location.search}</div>;
}

const renderAt = (entry) =>
  render(
    <MemoryRouter initialEntries={[entry]}>
      <Routes>
        {["/bills", "/admin/bills"].map((path) => (
          <Route key={path} path={path} element={<><BillsPage /><Address /></>} />
        ))}
      </Routes>
    </MemoryRouter>
  );

beforeEach(() =>
  getBills.mockResolvedValue(
    pageOf([bill(3, "Asha Rao", "UNPAID", 650), bill(2, "Kiran Rao", "PAID", 1250.5), bill(1, "Meera Nair", "REFUND_DUE", 500)])
  )
);
afterEach(() => jest.resetAllMocks());

test("today's bills, newest first, each linked to its bill", async () => {
  renderAt("/bills");

  const asha = (await screen.findByRole("link", { name: "B-2026-00003" })).closest("tr");
  expect(getBills).toHaveBeenCalledWith({ from: TODAY, to: TODAY, page: 0, size: 20 });
  for (const text of ["Today", "Asha Rao", "Dr. Anita Rao", "₹650.00", "Unpaid"]) {
    expect(within(asha).getByText(text)).toBeInTheDocument();
  }
  expect(screen.getByRole("link", { name: "B-2026-00003" })).toHaveAttribute("href", "/bills/3");
  expect(screen.getByRole("link", { name: "B-2026-00002" }).closest("tr")).toHaveTextContent("₹1,250.50");
  expect(screen.getByRole("link", { name: "B-2026-00001" }).closest("tr")).toHaveTextContent("Refund due");
  expect(screen.getByText("Page 1 of 1 · 3 bills")).toBeInTheDocument();
});

test("the filters go into the address and the request; inside the admin portal links stay there", async () => {
  renderAt("/admin/bills");
  await screen.findByRole("link", { name: "B-2026-00003" });

  fireEvent.click(screen.getByRole("button", { name: "Any date" }));
  await waitFor(() => expect(getBills).toHaveBeenLastCalledWith({ page: 0, size: 20 }));
  fireEvent.change(screen.getByLabelText("Status"), { target: { value: "refund_due" } });
  fireEvent.change(screen.getByLabelText("Patient or bill number"), { target: { value: " meera " } });
  fireEvent.click(screen.getByRole("button", { name: "Show" }));

  await waitFor(() =>
    expect(getBills).toHaveBeenLastCalledWith({ status: "refund_due", patient: "meera", page: 0, size: 20 })
  );
  expect(screen.getByTestId("address")).toHaveTextContent("/admin/bills?from=&to=&status=refund_due&patient=meera");
  expect(screen.getByRole("link", { name: "B-2026-00001" })).toHaveAttribute("href", "/admin/bills/1");
});

test("nothing found, and the server's message", async () => {
  getBills.mockResolvedValueOnce(pageOf([]));
  renderAt("/bills");
  expect(await screen.findByText(/No bills for these filters/)).toBeInTheDocument();

  getBills.mockRejectedValueOnce({ response: { status: 400, data: { message: "The start date must not be after the end date" } } });
  fireEvent.change(screen.getByLabelText("Status"), { target: { value: "paid" } });
  fireEvent.click(screen.getByRole("button", { name: "Show" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("The start date must not be after the end date");
});
