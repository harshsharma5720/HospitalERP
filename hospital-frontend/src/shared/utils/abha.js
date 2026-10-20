// The ABHA number (Ayushman Bharat Health Account, docs/ABHA_NUMBER_PLAN.md): 14 digits, written 12-3456-7890-1234.
// Typed with or without dashes or spaces; the server keeps the digits only and refuses anything else.

export const ABHA_HINT = "14 digits, e.g. 12-3456-7890-1234.";
export const ABHA_ERROR = "An ABHA number has 14 digits, e.g. 12-3456-7890-1234.";

const digitsOf = (text) => String(text ?? "").replace(/[\s-]/g, "");

// Empty (none) or 14 digits
export const isAbhaOrEmpty = (text) => /^(\d{14})?$/.test(digitsOf(text));

// "12-3456-7890-1234"; "" for none; anything else as it is
export const formatAbha = (number) => {
  const digits = digitsOf(number);
  return /^\d{14}$/.test(digits) ? `${digits.slice(0, 2)}-${digits.slice(2, 6)}-${digits.slice(6, 10)}-${digits.slice(10)}` : number ?? "";
};

// For the inputs: the keypad of digits on phones; the browser checks the shape before sending
export const abhaInputProps = {
  inputMode: "numeric",
  autoComplete: "off",
  placeholder: "12-3456-7890-1234",
  pattern: "[\\s\\-]*(\\d[\\s\\-]*){14}",
  title: ABHA_ERROR,
};
