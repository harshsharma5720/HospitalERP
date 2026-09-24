// Decodes the (unverified) payload of a JWT. Only used for UI decisions;
// the backend verifies the signature on every request.
const decodePayload = (token) => {
  const base64Url = token.split(".")[1];
  const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
  const jsonPayload = decodeURIComponent(
    atob(base64)
      .split("")
      .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
      .join("")
  );
  return JSON.parse(jsonPayload);
};

// ✅ Decode JWT token to extract role, e.g. "ROLE_PATIENT"
export const getRoleFromToken = (token) => {
  try {
    return decodePayload(token).role || "";
  } catch (err) {
    return "";
  }
};

// Extract userId from JWT token
export const getUserIdFromToken = (token) => {
  try {
    return decodePayload(token).userId ?? null;
  } catch (err) {
    return null;
  }
};

export const isTokenExpired = (token) => {
  try {
    return Date.now() > decodePayload(token).exp * 1000;
  } catch (e) {
    return true;
  }
};
