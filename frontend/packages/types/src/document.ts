export interface DocumentSummary {
  documentId: number;
  title: string;
  fileName: string;
  fileType: string;
  status: string;
}

export interface DocumentStatus {
  documentId: number;
  status: string;
  errorMessage?: string | null;
}

