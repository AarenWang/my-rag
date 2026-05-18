import type {
  DocumentChunkListResponse,
  DocumentEmbeddingEstimate,
  DocumentIndexResponse,
  DocumentStatus,
  DocumentSummary,
  DocumentUploadResponse
} from "@my-rag/types";
import { request } from "./client";

export function getDocuments() {
  return request<DocumentSummary[]>("/api/rag/documents");
}

export function getDocumentStatus(documentId: number) {
  return request<DocumentStatus>(`/api/rag/documents/${documentId}/status`);
}

export function uploadDocument(file: File) {
  const formData = new FormData();
  formData.append("file", file);
  return request<DocumentUploadResponse>("/api/rag/documents/upload", {
    method: "POST",
    body: formData,
    skipJsonBody: true
  });
}

export function triggerDocumentIndex(documentId: number) {
  return request<DocumentIndexResponse>(`/api/rag/documents/${documentId}/index`, {
    method: "POST"
  });
}

export function getDocumentEmbeddingEstimate(documentId: number) {
  return request<DocumentEmbeddingEstimate>(`/api/rag/documents/${documentId}/embedding/estimate`);
}

export function triggerDocumentEmbedding(documentId: number) {
  return request<DocumentIndexResponse>(`/api/rag/documents/${documentId}/embedding`, {
    method: "POST"
  });
}

export function getDocumentChunks(documentId: number) {
  return request<DocumentChunkListResponse>(`/api/rag/chunks?documentId=${documentId}`);
}
