import { createContext, useContext, useState, useCallback, useEffect } from "react";
import * as authApi from "../api/auth";

const AuthContext = createContext(null);

function decodeJwtExpiryMs(token) {
  try {
    const payload = token.split(".")[1];
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    const padded = base64 + "=".repeat((4 - (base64.length % 4)) % 4);
    const json = JSON.parse(atob(padded));
    return json.exp ? json.exp * 1000 : null;
  } catch {
    return null;
  }
}

function hasValidAccessToken() {
  const token = localStorage.getItem("accessToken");
  if (!token) return false;
  const expiryMs = decodeJwtExpiryMs(token);
  return !!expiryMs && Date.now() < expiryMs;
}

export function AuthProvider({ children }) {
  const [isAuthenticated, setIsAuthenticated] = useState(() => {
    if (hasValidAccessToken()) return true;
    localStorage.clear();
    return false;
  });

  // Catches the case where a tab is left open across expiry without any API
  // call happening to trigger the axios 401 -> refresh/redirect flow.
  useEffect(() => {
    const interval = setInterval(() => {
      if (isAuthenticated && !hasValidAccessToken() && !localStorage.getItem("refreshToken")) {
        localStorage.clear();
        setIsAuthenticated(false);
      }
    }, 30000);
    return () => clearInterval(interval);
  }, [isAuthenticated]);

  const loginWithTokens = useCallback((tokenResponse) => {
    localStorage.setItem("accessToken", tokenResponse.accessToken);
    localStorage.setItem("refreshToken", tokenResponse.refreshToken);
    setIsAuthenticated(true);
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
  }, []);

  return (
    <AuthContext.Provider value={{ isAuthenticated, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
