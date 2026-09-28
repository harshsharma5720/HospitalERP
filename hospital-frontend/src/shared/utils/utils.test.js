import { getRoleFromToken, getUserIdFromToken, isTokenExpired } from "./jwtUtils";
import { toLocalISODate, addDays } from "./dateUtils";
import { getErrorMessage } from "./apiError";

// Builds an unsigned JWT-shaped string; the frontend only decodes the payload
const fakeToken = (payload) =>
  `x.${btoa(JSON.stringify(payload)).replace(/=+$/, "").replace(/\+/g, "-").replace(/\//g, "_")}.y`;

describe("jwtUtils", () => {
  const future = Math.floor(Date.now() / 1000) + 3600;

  test("reads role and userId", () => {
    const token = fakeToken({ sub: "asha", role: "ROLE_PATIENT", userId: 42, exp: future });
    expect(getRoleFromToken(token)).toBe("ROLE_PATIENT");
    expect(getUserIdFromToken(token)).toBe(42);
    expect(isTokenExpired(token)).toBe(false);
  });

  test("does not fall back to the username when userId is missing", () => {
    expect(getUserIdFromToken(fakeToken({ sub: "asha" }))).toBeNull();
  });

  test("expired and malformed tokens are treated as expired", () => {
    expect(isTokenExpired(fakeToken({ exp: 1 }))).toBe(true);
    expect(isTokenExpired("garbage")).toBe(true);
    expect(getRoleFromToken("garbage")).toBe("");
  });
});

describe("dateUtils", () => {
  test("formats using local time, not UTC", () => {
    // 00:30 local time on 5 Jan — toISOString() would give 4 Jan in time zones ahead of UTC
    expect(toLocalISODate(new Date(2026, 0, 5, 0, 30))).toBe("2026-01-05");
  });

  test("addDays crosses month boundaries", () => {
    expect(toLocalISODate(addDays(new Date(2026, 0, 31), 1))).toBe("2026-02-01");
  });
});

describe("getErrorMessage", () => {
  test("prefers the backend message", () => {
    expect(getErrorMessage({ response: { data: { message: "Slot taken" } } }, "fallback")).toBe("Slot taken");
    expect(getErrorMessage({ response: { data: "Plain text" } }, "fallback")).toBe("Plain text");
    expect(getErrorMessage(new Error("network"), "fallback")).toBe("fallback");
  });
});
