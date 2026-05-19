import type {
  CollectionSummary,
  CollectionDetail,
  CreateCollectionRequest,
  UpdateCollectionRequest,
  CollectionDocument
} from "@my-rag/types";
import { request } from "./client";

export function getCollections(includeArchived = false) {
  const url = new URL("/api/rag/collections", window.location.origin);
  if (includeArchived) {
    url.searchParams.append("includeArchived", "true");
  }
  return request<CollectionSummary[]>(url.pathname + url.search);
}

export function getCollection(collectionId: number) {
  return request<CollectionDetail>(`/api/rag/collections/${collectionId}`);
}

export function createCollection(data: CreateCollectionRequest) {
  return request<CollectionDetail>("/api/rag/collections", {
    method: "POST",
    body: JSON.stringify(data)
  });
}

export function updateCollection(collectionId: number, data: UpdateCollectionRequest) {
  return request<CollectionDetail>(`/api/rag/collections/${collectionId}`, {
    method: "PATCH",
    body: JSON.stringify(data)
  });
}

export function archiveCollection(collectionId: number) {
  return request<void>(`/api/rag/collections/${collectionId}/archive`, {
    method: "POST"
  });
}

export function getCollectionDocuments(collectionId: number) {
  return request<CollectionDocument[]>(`/api/rag/collections/${collectionId}/documents`);
}
