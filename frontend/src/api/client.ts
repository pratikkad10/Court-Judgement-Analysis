import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";
import { clearSession, getSession, saveSession } from "../auth/session";
import type { Envelope, TokenResponse } from "./types";
export const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "/api",
});
client.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getSession()?.accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  config.headers["X-Correlation-Id"] = crypto.randomUUID();
  return config;
});
let refreshing: Promise<string> | null = null;
client.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as InternalAxiosRequestConfig & {
      retried?: boolean;
    };
    if (
      error.response?.status !== 401 ||
      original.retried ||
      original.url?.includes("/auth/refresh")
    )
      return Promise.reject(error);
    original.retried = true;
    const session = getSession();
    if (!session) return Promise.reject(error);
    refreshing ??= axios
      .post<Envelope<TokenResponse>>(
        `${client.defaults.baseURL}/auth/refresh`,
        { refreshToken: session.refreshToken },
      )
      .then(({ data }) => {
        saveSession(data.data);
        return data.data.accessToken;
      })
      .finally(() => {
        refreshing = null;
      });
    try {
      original.headers.Authorization = `Bearer ${await refreshing}`;
      return client(original);
    } catch (refreshError) {
      clearSession();
      window.dispatchEvent(new Event("auth-expired"));
      return Promise.reject(refreshError);
    }
  },
);
export const unwrap = <T>(response: { data: Envelope<T> }): T => {
  if (!response.data.success)
    throw new Error(response.data.error?.message ?? "Request failed");
  return response.data.data;
};
