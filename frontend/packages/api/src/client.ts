import type { ApiResponse } from "@my-rag/types";

let apiBaseUrl = "";

export function configureApiClient(options: { baseUrl?: string }) {
  apiBaseUrl = options.baseUrl ?? "";
}

export async function request<T>(path: string, init?: RequestInit): Promise<ApiResponse<T>> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...init?.headers
    },
    ...init
  });

  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`);
  }

  return response.json() as Promise<ApiResponse<T>>;
}
