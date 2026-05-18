import type { ApiResponse } from "@my-rag/types";

let apiBaseUrl = "";

export function configureApiClient(options: { baseUrl?: string }) {
  apiBaseUrl = options.baseUrl ?? "";
}

interface RequestOptions extends RequestInit {
  skipJsonBody?: boolean;
}

export async function request<T>(path: string, init?: RequestOptions): Promise<ApiResponse<T>> {
  const headers: Record<string, string> = {};
  
  if (!init?.skipJsonBody) {
    headers["Content-Type"] = "application/json";
  }
  
  const response = await fetch(`${apiBaseUrl}${path}`, {
    headers: {
      ...headers,
      ...init?.headers
    },
    ...init
  });

  if (!response.ok) {
    let errorMessage = `Request failed: ${response.status}`;
    try {
      const errorData = await response.json();
      errorMessage = errorData.message || errorMessage;
    } catch {
      // Ignore if error response is not JSON
    }
    throw new Error(errorMessage);
  }

  return response.json() as Promise<ApiResponse<T>>;
}
