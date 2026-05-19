export interface CollectionSummary {
  collectionId: number;
  name: string;
  description: string | null;
  tags: string | null;
  archived: boolean;
  documentCount: number;
  readyDocumentCount: number;
  chunkCount: number;
}

export interface CollectionDetail {
  collectionId: number;
  name: string;
  description: string | null;
  tags: string | null;
  archived: boolean;
  documentCount: number;
  readyDocumentCount: number;
  chunkCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCollectionRequest {
  name: string;
  description?: string;
  tags?: string;
}

export interface UpdateCollectionRequest {
  name?: string;
  description?: string;
  tags?: string;
}

export interface CollectionDocument {
  documentId: number;
  title: string;
  fileName: string;
  status: string;
}
