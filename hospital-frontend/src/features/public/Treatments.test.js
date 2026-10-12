import { render, screen, within } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import Treatments from "./Treatments";
import { getAllDoctors } from "../doctors/api";

jest.mock("../doctors/api");
jest.mock("../../shared/ProfilePage", () => () => null);

const renderPage = () =>
  render(
    <MemoryRouter>
      <Treatments />
    </MemoryRouter>
  );

afterEach(() => jest.resetAllMocks());

test("each department says what it looks after and leads to its doctors", async () => {
  getAllDoctors.mockResolvedValue({
    data: [
      { id: 1, name: "Anita Rao", specialist: "CARDIOLOGY", active: true },
      { id: 2, name: "Meera Nair", specialist: "CARDIOLOGY", active: true },
      { id: 3, name: "Old Doctor", specialist: "NEUROLOGY", active: false },
    ],
  });
  renderPage();
  expect(screen.getByRole("heading", { level: 1, name: "Specialist care under one roof" })).toBeInTheDocument();
  const cardiology = screen.getByRole("article", { name: "Cardiology" });
  expect(within(cardiology).getByText("High blood pressure")).toBeInTheDocument();
  expect(await within(cardiology).findByRole("link", { name: "See 2 doctors" })).toHaveAttribute("href", "/doctors?speciality=CARDIOLOGY");
  // A deactivated doctor doesn't count
  expect(within(screen.getByRole("article", { name: "Neurology" })).getByRole("link", { name: "See doctors" })).toHaveAttribute(
    "href",
    "/doctors?speciality=NEUROLOGY"
  );
  // Oncology has no doctors in the app: arranged by phone
  const oncology = screen.getByRole("article", { name: "Oncology" });
  expect(within(oncology).getByRole("link", { name: "Call to arrange a visit" })).toHaveAttribute("href", "tel:+919289520303");
  expect(within(oncology).queryByRole("link", { name: /doctors/ })).not.toBeInTheDocument();
});

test("without the directory the departments still show", async () => {
  getAllDoctors.mockRejectedValue(new Error("offline"));
  renderPage();
  expect(screen.getAllByRole("article")).toHaveLength(7);
  expect(await within(screen.getByRole("article", { name: "Dentistry" })).findByRole("link", { name: "See doctors" })).toBeInTheDocument();
});
