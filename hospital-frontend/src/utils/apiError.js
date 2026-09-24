// Extracts the backend's error message ({ message, success }) from an axios/fetch error.
export const getErrorMessage = (error, fallback = "Something went wrong. Please try again.") =>
  error?.response?.data?.message ||
  (typeof error?.response?.data === "string" && error.response.data) ||
  fallback;
