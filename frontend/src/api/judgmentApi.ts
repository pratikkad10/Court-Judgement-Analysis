import { client, unwrap } from "./client";
import type {
  Analysis,
  AuditLog,
  ChatMessage,
  DocumentItem,
  Session,
  TokenResponse,
  User,
} from "./types";
export const authApi = {
  login: (email: string, password: string) =>
    client.post("/auth/login", { email, password }).then(unwrap<TokenResponse>),
  register: (email: string, password: string) =>
    client
      .post("/auth/register", { email, password })
      .then(unwrap<TokenResponse>),
  me: () => client.get("/auth/me").then(unwrap<User>),
  users: () => client.get("/auth/admin/users").then(unwrap<User[]>),
  setEnabled: (id: string, value: boolean) =>
    client
      .patch(`/auth/admin/users/${id}/enabled?value=${value}`)
      .then(unwrap<User>),
};
export const documentsApi = {
  list: () => client.get("/documents").then(unwrap<DocumentItem[]>),
  upload: (file: File) => {
    const form = new FormData();
    form.append("file", file);
    return client.post("/documents", form).then(unwrap<DocumentItem>);
  },
  remove: (id: string) => client.delete(`/documents/${id}`).then(unwrap<void>),
  pdf: (id: string) =>
    client
      .get(`/documents/${id}/download`, { responseType: "blob" })
      .then((r) => r.data as Blob),
};
export const researchApi = {
  sessions: () => client.get("/chat/sessions").then(unwrap<Session[]>),
  createSession: (title: string) =>
    client.post("/chat/sessions", { title }).then(unwrap<Session>),
  messages: (id: string) =>
    client.get(`/chat/sessions/${id}/messages`).then(unwrap<ChatMessage[]>),
  ask: (sessionId: string, documentIds: string[], question: string) =>
    client
      .post(`/chat/sessions/${sessionId}/messages`, { documentIds, question })
      .then(unwrap<ChatMessage>),
  analyse: (
    type: "summary" | "arguments" | "compare",
    documentIds: string[],
    issue = "",
  ) =>
    client
      .post(`/analyses/${type}`, { documentIds, issue })
      .then(unwrap<Analysis>),
  history: () => client.get("/history").then(unwrap<Analysis[]>),
  dashboard: () =>
    client
      .get("/dashboard")
      .then(unwrap<{ sessions: number; messages: number; analyses: number }>),
  adminDashboard: () =>
    client.get("/dashboard/admin").then(unwrap<Record<string, number>>),
  audit: () => client.get("/dashboard/admin/audit").then(unwrap<AuditLog[]>),
};
