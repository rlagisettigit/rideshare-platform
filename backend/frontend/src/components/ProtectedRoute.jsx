import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

/** @param roles - optional list of roles allowed to view this route; omit to allow any authenticated user. */
export default function ProtectedRoute({ children, roles }) {
  const { isAuthenticated, roles: userRoles } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (roles && !roles.some((role) => userRoles.includes(role))) {
    return <Navigate to="/search" replace />;
  }
  return children;
}
