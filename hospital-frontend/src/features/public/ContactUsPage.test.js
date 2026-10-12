import { render, screen, within } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import ContactUsPage from "./ContactUsPage";


const renderPage = () =>
  render(
    <MemoryRouter>
      <ContactUsPage />
    </MemoryRouter>
  );

test("the ways to reach the hospital that work: call, book online, visit", () => {
  renderPage();
  expect(screen.getByRole("heading", { level: 1, name: "We're here, day and night" })).toBeInTheDocument();
  const call = screen.getByRole("region", { name: "Call us" });
  expect(within(call).getByRole("link", { name: /Call \+91 92895 20303/ })).toHaveAttribute("href", "tel:+919289520303");
  expect(within(call).getByText("Open 24×7")).toBeInTheDocument();
  const book = screen.getByRole("region", { name: "Book online" });
  expect(within(book).getByRole("link", { name: "Book an appointment" })).toHaveAttribute("href", "/appointments");
  expect(within(book).getByRole("link", { name: "Find a doctor" })).toHaveAttribute("href", "/doctors");
  const visit = screen.getByRole("region", { name: "Visit us" });
  expect(visit).toHaveTextContent("Plot No. 837, Shalimar Garden Main Rd");
  expect(within(visit).getByRole("link", { name: /Get directions/ })).toHaveAttribute("href", expect.stringContaining("google.com/maps"));
  expect(screen.getByText("Good to know before you come")).toBeInTheDocument();
});

test("no form that sends nothing, no other hospital's details, no placeholder social links", () => {
  const { container } = renderPage();
  expect(container.querySelector("form")).toBeNull();
  expect(screen.queryByText(/maxhospital|Max Healthcare|98765 43210/i)).not.toBeInTheDocument();
  const links = Array.from(container.querySelectorAll("a")).map((a) => a.getAttribute("href"));
  expect(links.some((href) => /instagram|facebook|twitter|linkedin|youtube/.test(href))).toBe(false);
});
