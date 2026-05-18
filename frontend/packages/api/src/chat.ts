import type { ChatRequest, ChatResponse, ChatLogSummary, ChatLogDetail } from "@my-rag/types";
import { request } from "./client";

export function chat(payload: ChatRequest) {
  return request<ChatResponse>("/api/rag/chat", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function listChatLogs() {
  return request<ChatLogSummary[]>("/api/rag/chat/logs", {
    method: "GET"
  });
}

export function getChatLogDetail(id: number) {
  return request<ChatLogDetail>(`/api/rag/chat/logs/${id}`, {
    method: "GET"
  });
}

