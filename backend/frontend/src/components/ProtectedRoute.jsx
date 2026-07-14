import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

/**
 * @param roles - optional list of roles allowed to view this route; omit to allow any authenticated user.
 * @param requireCompleteProfile - set false only on the complete-profile route itself, so a user
 *   with a missing mobile number can actually reach the screen that collects it instead of being
 *   bounced back to it forever.
 */
export default function ProtectedRoute({ children, roles, requireCompleteProfile = true }) {
  const { isAuthenticated, roles: userRoles, profileComplete } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (roles && !roles.some((role) => userRoles.includes(role))) {
    return <Navigate to="/search" replace />;
  }
  if (requireCompleteProfile && profileComplete === false) {
    return <Navigate to="/complete-profile" replace />;
  }
  return children;
}
