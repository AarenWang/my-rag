import type { ChatRequest, ChatResponse } from "@my-rag/types";
import { request } from "./client";

export function chat(payload: ChatRequest) {
  return request<ChatResponse>("/api/rag/chat", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

