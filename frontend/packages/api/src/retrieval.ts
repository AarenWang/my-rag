import type { RetrievalDebugRequest, RetrievalDebugResponse } from "@my-rag/types";
import { request } from "./client";

export function debugRetrieval(payload: RetrievalDebugRequest) {
  return request<RetrievalDebugResponse>("/api/rag/retrieval/debug", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}
