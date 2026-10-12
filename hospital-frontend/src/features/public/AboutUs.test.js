import { render, screen, within, act } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import AboutUs from "./AboutUs";
import { getAllDoctors } from "../doctors/api";

jest.mock("../doctors/api");
jest.mock("../../shared/ProfilePage", () => () => null);

const renderPage = () =>
  render(
    <MemoryRouter>
      <AboutUs />
    </MemoryRouter>
  );

afterEach(() => jest.resetAllMocks());

test("the hospital's story, mission and vision, with its own photo", async () => {
  getAllDoctors.mockResolvedValue({ data: [] });
  const { container } = renderPage();
  expect(screen.getByRole("region", { name: "Welcome to Shreya Hospital" })).toHaveTextContent("personal, accessible, and compassionate");
  const mission = screen.getByRole("region", { name: "Mission and vision" });
  expect(within(mission).getByRole("heading", { name: "Our mission" })).toBeInTheDocument();
  expect(within(mission).getByRole("heading", { name: "Our vision" })).toBeInTheDocument();
  const sources = Array.from(container.querySelectorAll("img")).map((img) => img.getAttribute("src"));
  expect(sources).toContain("/shreya.jpg");
  expect(sources.some((src) => /Shreyahospital|randomuser/.test(src))).toBe(false); // watermarked stock photo, made-up team
  await act(async () => {});
  expect(screen.queryByRole("region", { name: "Meet our doctors" })).not.toBeInTheDocument(); // no doctors: no empty section
});

test("the team is the hospital's real doctors", async () => {
  getAllDoctors.mockResolvedValue({
    data: [
      { id: 1, name: "Anita Rao", specialist: "CARDIOLOGY", active: true },
      { id: 2, name: "Old Doctor", specialist: "NEUROLOGY", active: false },
    ],
  });
  renderPage();
  const team = await screen.findByRole("region", { name: "Meet our doctors" });
  expect(within(team).getByText("Dr. Anita Rao")).toBeInTheDocument();
  expect(within(team).queryByText("Dr. Old Doctor")).not.toBeInTheDocument();
  expect(screen.queryByText(/Priya Sharma|Arjun Mehta|Neha Kapoor/)).not.toBeInTheDocument();
});

test("without the directory there is no team section, and nothing invented in its place", async () => {
  getAllDoctors.mockRejectedValue(new Error("offline"));
  renderPage();
  await act(async () => {});
  expect(screen.queryByRole("region", { name: "Meet our doctors" })).not.toBeInTheDocument();
  expect(screen.getByRole("heading", { name: "Need medical assistance?" })).toBeInTheDocument();
  expect(screen.getAllByRole("link", { name: /Call \+91 92895 20303/ }).length).toBeGreaterThan(0);
});
