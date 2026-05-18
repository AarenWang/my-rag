import { request } from "./client";

export interface HealthResponse {
  status: string;
  service: string;
  timestamp: string;
}

export function getHealth() {
  return request<HealthResponse>("/api/rag/health");
}

