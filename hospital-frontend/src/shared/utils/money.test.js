import { formatRupees } from "./money";

test("rupees with Indian digit grouping and 2 decimals", () => {
  expect(formatRupees(500)).toBe("₹500.00");
  expect(formatRupees("1250.5")).toBe("₹1,250.50");
  expect(formatRupees(125000)).toBe("₹1,25,000.00");
  expect(formatRupees(0)).toBe("₹0.00");
  expect(formatRupees(null)).toBe("—");
});
