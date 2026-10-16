import { formatRupees, formatWholeRupees } from "./money";

test("rupees with Indian digit grouping and 2 decimals", () => {
  expect(formatRupees(500)).toBe("₹500.00");
  expect(formatRupees("1250.5")).toBe("₹1,250.50");
  expect(formatRupees(125000)).toBe("₹1,25,000.00");
  expect(formatRupees(0)).toBe("₹0.00");
  expect(formatRupees(null)).toBe("—");
});

test("whole rupees for headline figures, rounded", () => {
  expect(formatWholeRupees(109200)).toBe("₹1,09,200");
  expect(formatWholeRupees("550.50")).toBe("₹551");
  expect(formatWholeRupees(0)).toBe("₹0");
  expect(formatWholeRupees(null)).toBe("—");
});
