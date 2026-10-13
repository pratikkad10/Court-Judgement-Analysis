import type { TokenResponse, User } from "../api/types";
const KEY = "judgment.session";
export interface SessionState {
  accessToken: string;
  refreshToken: string;
  user: User;
}
export const getSession = (): SessionState | null => {
  const raw = localStorage.getItem(KEY);
  return raw ? (JSON.parse(raw) as SessionState) : null;
};
export const saveSession = (tokens: TokenResponse) =>
  localStorage.setItem(
    KEY,
    JSON.stringify({
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      user: tokens.user,
    }),
  );
export const clearSession = () => localStorage.removeItem(KEY);
