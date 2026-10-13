import { createContext, useContext, useEffect, useMemo, useState } from "react";
import type { User } from "../api/types";
import { clearSession, getSession } from "./session";
interface AuthValue {
  user: User | null;
  signOut: () => void;
  refresh: () => void;
}
const AuthContext = createContext<AuthValue | null>(null);
export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(getSession()?.user ?? null);
  const refresh = () => setUser(getSession()?.user ?? null);
  const signOut = () => {
    clearSession();
    setUser(null);
  };
  useEffect(() => {
    window.addEventListener("auth-expired", signOut);
    return () => window.removeEventListener("auth-expired", signOut);
  }, []);
  return (
    <AuthContext.Provider
      value={useMemo(() => ({ user, signOut, refresh }), [user])}
    >
      {children}
    </AuthContext.Provider>
  );
}
export const useAuth = () => {
  const value = useContext(AuthContext);
  if (!value) throw new Error("AuthProvider is required");
  return value;
};
