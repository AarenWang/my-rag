export interface RetrievalDebugRequest {
  question: string;
  documentIds?: number[];
  topK?: number;
  scoreThreshold?: number;
}

export interface RetrievalDebugConfig {
  topK: number;
  vectorTopK: number;
  keywordTopK: number;
  rrfTopK: number;
  rerankTopK: number;
  contextTopK: number;
  scoreThreshold: number;
  rrfK: number;
  maxContextChars: number;
  rerankerProvider: string;
}

export interface RetrievalDebugCandidate {
  rank: number;
  documentId: number;
  documentTitle: string;
  chapterTitle: string | null;
  chunkId: number;
  chunkIndex: number;
  startParagraph: number | null;
  endParagraph: number | null;
  content: string;
  score: number | null;
  vectorScore: number | null;
  keywordScore: number | null;
  rrfScore: number | null;
  rerankScore: number | null;
  finalScore: number | null;
  vectorRank: number | null;
  keywordRank: number | null;
  retrievalSources: string[];
}

export interface RetrievalDebugEvidence {
  sourceId: string;
  documentId: number;
  documentTitle: string;
  chapterTitle: string | null;
  chunkId: number;
  chunkIndex: number;
  content: string;
  finalScore: number | null;
  retrievalSources: string[];
}

export interface RetrievalDebugResponse {
  question: string;
  mode: string;
  keywordIndexEnabled: boolean;
  keywordQueries: string[];
  config: RetrievalDebugConfig;
  vectorCandidates: RetrievalDebugCandidate[];
  keywordCandidates: RetrievalDebugCandidate[];
  rrfCandidates: RetrievalDebugCandidate[];
  rerankedCandidates: RetrievalDebugCandidate[];
  evidences: RetrievalDebugEvidence[];
}
