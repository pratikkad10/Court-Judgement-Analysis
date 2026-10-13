export type Role = "USER" | "ADMIN";
export interface User {
  id: string;
  email: string;
  role: Role;
  enabled: boolean;
}
export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: User;
}
export interface Envelope<T> {
  success: boolean;
  data: T;
  error: { code: string; message: string } | null;
}
export interface DocumentItem {
  id: string;
  title: string;
  filename: string;
  pageCount: number;
  status: "UPLOADED" | "PROCESSING" | "READY" | "FAILED";
  court?: string;
  caseNumber?: string;
  judgmentDate?: string;
  createdAt: string;
}
export interface Citation {
  source_id: string;
  document_id: string;
  document_title: string;
  page_start: number;
  page_end: number;
  section: string;
  snippet: string;
}
export interface ChatMessage {
  id: string;
  role: "USER" | "ASSISTANT";
  content: string;
  citations: Citation[];
  providerUsed?: string;
  latencyMs?: number;
  createdAt: string;
}
export interface Session {
  id: string;
  title: string;
  createdAt: string;
  updatedAt: string;
}
export interface Analysis {
  id: string;
  type: string;
  documentIds: string[];
  result: Record<string, unknown>;
  createdAt: string;
}

export interface AuditLog {
  id: string;
  ownerId?: string;
  action: string;
  correlationId?: string;
  details: unknown;
  createdAt: string;
}
