export const getRoleFromToken = (token) => {

// ✅ Decode JWT token to extract role
  try {
    const base64Url = token.split(".")[1];
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split("")
        .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join("")
    );
    const payload = JSON.parse(jsonPayload);
    return payload.role || "";
  } catch (err) {
    console.error("Error decoding token:", err);
    return "";
  }
};

// Extract userId from JWT token
export const getUserIdFromToken = (token) => {
  try {
    const base64Url = token.split(".")[1];
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split("")
        .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join("")
    );
    const payload = JSON.parse(jsonPayload);
    // Try multiple possible fields for userId
    return payload.userId || payload.id || payload.sub || null;
  } catch (err) {
    console.error("Error decoding token for userId:", err);
    return null;
  }
};
