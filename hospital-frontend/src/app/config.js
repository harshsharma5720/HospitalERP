// Backend base URL. Override with REACT_APP_API_URL in hospital-frontend/.env (e.g. for a deployed API).
export const API_BASE_URL = (process.env.REACT_APP_API_URL || "http://localhost:8080").replace(/\/$/, "");
