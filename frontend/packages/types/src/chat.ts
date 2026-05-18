export interface ChatRequest {
  question: string;
  documentIds?: number[];
  topK?: number;
  scoreThreshold?: number;
}

export interface ChatSource {
  documentId: number;
  documentTitle: string;
  chapterTitle: string;
  chunkId: number;
  chunkIndex: number;
  score: number;
}

export interface ChatResponse {
  answer: string;
  noAnswer: boolean;
  sources: ChatSource[];
}

