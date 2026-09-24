import { Navigate, useLocation } from "react-router-dom";
import useAuthStore from "../Store/useAuthStore";

// Renders children only for a logged-in user with one of the allowed roles.
// This is a UX guard; the backend enforces the same rules on every request.
export default function ProtectedRoute({ roles, children }) {
  const role = useAuthStore((s) => s.role);
  const location = useLocation();

  if (!role) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  if (roles && !roles.includes(role)) {
    return <Navigate to="/" replace />;
  }
  return children;
}
