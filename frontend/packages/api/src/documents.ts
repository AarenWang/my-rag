import type { DocumentStatus, DocumentSummary } from "@my-rag/types";
import { request } from "./client";

export function getDocuments() {
  return request<DocumentSummary[]>("/api/rag/documents");
}

export function getDocumentStatus(documentId: number) {
  return request<DocumentStatus>(`/api/rag/documents/${documentId}/status`);
}

