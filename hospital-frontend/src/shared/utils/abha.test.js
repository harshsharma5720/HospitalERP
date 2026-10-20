import { formatAbha, isAbhaOrEmpty } from "./abha";

// The ABHA number's 14 digits (docs/ABHA_NUMBER_PLAN.md)

test("written in four groups whatever the way it was typed; nothing stays nothing", () => {
  expect(formatAbha("12345678901234")).toBe("12-3456-7890-1234");
  expect(formatAbha(" 12 3456 7890 1234 ")).toBe("12-3456-7890-1234");
  expect(formatAbha("12-3456-7890-1234")).toBe("12-3456-7890-1234");
  expect(formatAbha(null)).toBe("");
  expect(formatAbha(undefined)).toBe("");
  expect(formatAbha("123")).toBe("123"); // not one: as it is
});

test("empty, or 14 digits with or without dashes and spaces", () => {
  for (const ok of ["", "  ", "12345678901234", "12-3456-7890-1234", " 12 3456 7890 1234 "]) {
    expect(isAbhaOrEmpty(ok)).toBe(true);
  }
  for (const wrong of ["1234567890123", "123456789012345", "12-3456-7890-123a", "12.3456.7890.1234"]) {
    expect(isAbhaOrEmpty(wrong)).toBe(false);
  }
});
