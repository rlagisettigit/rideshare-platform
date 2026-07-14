import { createContext, useContext, useState, useCallback, useEffect } from "react";
import * as authApi from "../api/auth";
import { getMyProfile } from "../api/users";

const AuthContext = createContext(null);

function decodeJwtPayload(token) {
  try {
    const payload = token.split(".")[1];
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    const padded = base64 + "=".repeat((4 - (base64.length % 4)) % 4);
    return JSON.parse(atob(padded));
  } catch {
    return null;
  }
}

function hasValidAccessToken() {
  const token = localStorage.getItem("accessToken");
  if (!token) return false;
  const payload = decodeJwtPayload(token);
  const expiryMs = payload?.exp ? payload.exp * 1000 : null;
  return !!expiryMs && Date.now() < expiryMs;
}

// The backend bakes roles (PASSENGER/DRIVER/ADMIN, see AuthService.issueTokens) into the JWT
// at issue time - a role granted after the token was issued (e.g. onboarding as a driver
// mid-session) only shows up here after the next login/refresh, matching backend behavior.
function currentRoles() {
  const token = localStorage.getItem("accessToken");
  if (!token) return [];
  const payload = decodeJwtPayload(token);
  return Array.isArray(payload?.roles) ? payload.roles : [];
}

export function AuthProvider({ children }) {
  const [isAuthenticated, setIsAuthenticated] = useState(() => {
    if (hasValidAccessToken()) return true;
    localStorage.clear();
    return false;
  });
  const [roles, setRoles] = useState(() => (hasValidAccessToken() ? currentRoles() : []));
  // null = not checked yet (e.g. right after login, before the profile fetch below resolves).
  // Mobile is the signal: it's the one field required at normal signup but left null for
  // Google/Apple accounts (see AuthService.provisionSocialUser), so it's null only for a social
  // signup that hasn't completed its profile yet - never for a LOCAL account or the bootstrap admin.
  const [profileComplete, setProfileComplete] = useState(null);

  const refreshProfileStatus = useCallback(async () => {
    try {
      const res = await getMyProfile();
      const complete = !!res.data.mobile;
      setProfileComplete(complete);
      return complete;
    } catch {
      // Leave as-is; a 401 here already triggers client.js's redirect-to-login flow.
      return null;
    }
  }, []);

  useEffect(() => {
    if (isAuthenticated) {
      refreshProfileStatus();
    } else {
      setProfileComplete(null);
    }
  }, [isAuthenticated, refreshProfileStatus]);

  // Catches the case where a tab is left open across expiry without any API
  // call happening to trigger the axios 401 -> refresh/redirect flow. Also re-syncs `roles`
  // here, since client.js's silent-refresh interceptor replaces tokens in localStorage
  // directly, bypassing loginWithTokens below.
  useEffect(() => {
    const interval = setInterval(() => {
      if (isAuthenticated && !hasValidAccessToken() && !localStorage.getItem("refreshToken")) {
        localStorage.clear();
        setIsAuthenticated(false);
        setRoles([]);
      } else if (isAuthenticated) {
        setRoles((prev) => {
          const next = currentRoles();
          return prev.length === next.length && prev.every((r) => next.includes(r)) ? prev : next;
        });
      }
    }, 30000);
    return () => clearInterval(interval);
  }, [isAuthenticated]);

  const loginWithTokens = useCallback((tokenResponse) => {
    localStorage.setItem("accessToken", tokenResponse.accessToken);
    localStorage.setItem("refreshToken", tokenResponse.refreshToken);
    setIsAuthenticated(true);
    setRoles(currentRoles());
  }, []);

  const login = useCallback(async (payload) => {
    const res = await authApi.login(payload);
    loginWithTokens(res.data);
    return res.data;
  }, [loginWithTokens]);

  const register = useCallback(async (payload) => {
    const res = await authApi.register(payload);
    loginWithTokens(res.data);
    return res.data;
  }, [loginWithTokens]);

  const logout = useCallback(() => {
    localStorage.clear();
    setIsAuthenticated(false);
    setRoles([]);
  }, []);

  const hasRole = useCallback((role) => roles.includes(role), [roles]);

  return (
    <AuthContext.Provider value={{
      isAuthenticated, roles, hasRole,
      isPassenger: roles.includes("PASSENGER"),
      isDriver: roles.includes("DRIVER"),
      isAdmin: roles.includes("ADMIN"),
      profileComplete, refreshProfileStatus,
      login, register, logout
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
