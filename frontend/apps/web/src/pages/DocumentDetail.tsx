import { getDocumentChunks, getDocuments, getDocumentStatus, triggerDocumentIndex } from "@my-rag/api";
import type { DocumentChunk } from "@my-rag/types";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Card, Descriptions, Empty, List, Skeleton, Space, Tag, Typography, message } from "antd";
import { ArrowLeft, FileText, PlayCircle, RefreshCw } from "lucide-react";
import { useMemo } from "react";
import { useNavigate, useParams } from "react-router-dom";

const { Paragraph, Text } = Typography;

const STATUS_LABELS: Record<string, { color: string; text: string }> = {
  UPLOADED: { color: "blue", text: "Uploaded" },
  PARSING: { color: "cyan", text: "Parsing" },
  PARSED: { color: "purple", text: "Parsed" },
  CHUNKING: { color: "orange", text: "Chunking" },
  CHUNKED: { color: "geekblue", text: "Chunked" },
  EMBEDDING: { color: "gold", text: "Embedding" },
  READY: { color: "green", text: "Ready" },
  FAILED: { color: "red", text: "Failed" }
};

function StatusTag({ status }: { status?: string }) {
  if (!status) {
    return <Tag>Unknown</Tag>;
  }

  const config = STATUS_LABELS[status] ?? { color: "default", text: status };
  return <Tag color={config.color}>{config.text}</Tag>;
}

function canIndex(status?: string) {
  return status ? ["UPLOADED", "PARSED", "CHUNKED", "READY", "FAILED"].includes(status) : false;
}

function previewContent(chunk: DocumentChunk) {
  if (!chunk.content) {
    return "No content";
  }

  return chunk.content.length > 720 ? `${chunk.content.slice(0, 720)}...` : chunk.content;
}

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : "Unknown error";
}

export default function DocumentDetail() {
  const { documentId } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const numericDocumentId = Number(documentId);
  const isValidDocumentId = Number.isFinite(numericDocumentId) && numericDocumentId > 0;

  const documentsQuery = useQuery({
    queryKey: ["documents"],
    queryFn: getDocuments,
    enabled: isValidDocumentId,
    retry: false
  });

  const statusQuery = useQuery({
    queryKey: ["document-status", numericDocumentId],
    queryFn: () => getDocumentStatus(numericDocumentId),
    enabled: isValidDocumentId,
    retry: false
  });

  const chunksQuery = useQuery({
    queryKey: ["document-chunks", numericDocumentId],
    queryFn: () => getDocumentChunks(numericDocumentId),
    enabled: isValidDocumentId,
    retry: false
  });

  const indexMutation = useMutation({
    mutationFn: () => triggerDocumentIndex(numericDocumentId),
    onSuccess: (result) => {
      message.success(result.data.message || "Index task submitted");
      queryClient.invalidateQueries({ queryKey: ["documents"] });
      queryClient.invalidateQueries({ queryKey: ["document-status", numericDocumentId] });
      queryClient.invalidateQueries({ queryKey: ["document-chunks", numericDocumentId] });
    },
    onError: (error: Error) => {
      message.error(`Index failed: ${error.message}`);
    }
  });

  const document = useMemo(
    () => documentsQuery.data?.data.find((item) => item.documentId === numericDocumentId),
    [documentsQuery.data?.data, numericDocumentId]
  );

  const status = statusQuery.data?.data.status ?? document?.status;
  const chunks = chunksQuery.data?.data.chunks ?? [];
  const isLoading = documentsQuery.isLoading || statusQuery.isLoading || chunksQuery.isLoading;
  const failedQueryMessages = [
    documentsQuery.isError ? `documents: ${getErrorMessage(documentsQuery.error)}` : null,
    statusQuery.isError ? `status: ${getErrorMessage(statusQuery.error)}` : null,
    chunksQuery.isError ? `chunks: ${getErrorMessage(chunksQuery.error)}` : null
  ].filter(Boolean);
  const visibleDocumentError = status === "FAILED" ? statusQuery.data?.data.errorMessage : null;

  if (!isValidDocumentId) {
    return (
      <section className="page">
        <Card>
          <Empty description="Invalid document id" />
        </Card>
      </section>
    );
  }

  return (
    <section className="page">
      <div className="page-heading">
        <div>
          <h1>Document detail</h1>
          <p>Inspect document #{numericDocumentId}, processing status, and generated chunks.</p>
        </div>
        <Space wrap>
          <Button icon={<ArrowLeft size={16} />} onClick={() => navigate("/documents")}>
            Back
          </Button>
          <Button
            icon={<RefreshCw size={16} />}
            onClick={() => {
              documentsQuery.refetch();
              statusQuery.refetch();
              chunksQuery.refetch();
            }}
          >
            Refresh
          </Button>
          <Button
            type="primary"
            icon={<PlayCircle size={16} />}
            disabled={!canIndex(status)}
            loading={indexMutation.isPending}
            onClick={() => indexMutation.mutate()}
          >
            Re-index
          </Button>
        </Space>
      </div>

      {failedQueryMessages.length > 0 ? (
        <Alert
          type="error"
          showIcon
          message="Failed to load document detail"
          description={failedQueryMessages.join("; ")}
        />
      ) : null}

      <Card>
        <Skeleton loading={isLoading} active paragraph={{ rows: 4 }}>
          <Descriptions column={{ xs: 1, md: 2 }} bordered size="middle">
            <Descriptions.Item label="Document ID">{numericDocumentId}</Descriptions.Item>
            <Descriptions.Item label="Status">
              <StatusTag status={status} />
            </Descriptions.Item>
            <Descriptions.Item label="Title">{document?.title ?? "Document not found"}</Descriptions.Item>
            <Descriptions.Item label="File name">{document?.fileName ?? "-"}</Descriptions.Item>
            <Descriptions.Item label="File type">
              {document?.fileType ? <Tag>{document.fileType.toUpperCase()}</Tag> : "-"}
            </Descriptions.Item>
            <Descriptions.Item label="Error">
              {visibleDocumentError ? <Text type="danger">{visibleDocumentError}</Text> : "None"}
            </Descriptions.Item>
          </Descriptions>
        </Skeleton>
      </Card>

      <Card
        title={
          <Space>
            <FileText size={18} />
            <span>Chunks</span>
            <Tag color="green">{chunks.length}</Tag>
          </Space>
        }
      >
        <Skeleton loading={chunksQuery.isLoading} active paragraph={{ rows: 6 }}>
          {chunks.length === 0 ? (
            <Empty description="No chunks yet. Run index first, or refresh after processing finishes." />
          ) : (
            <List
              itemLayout="vertical"
              dataSource={chunks}
              pagination={{ pageSize: 8, size: "small" }}
              renderItem={(chunk) => (
                <List.Item key={chunk.chunkId}>
                  <List.Item.Meta
                    title={
                      <Space wrap>
                        <Text strong>Chunk #{chunk.chunkIndex}</Text>
                        <Tag color="blue">ID {chunk.chunkId}</Tag>
                        {chunk.chapterTitle ? <Tag>{chunk.chapterTitle}</Tag> : null}
                        {chunk.tokenCount ? <Tag color="gold">{chunk.tokenCount} tokens</Tag> : null}
                      </Space>
                    }
                    description={
                      <Text type="secondary">
                        Paragraph {chunk.startParagraph ?? "-"} - {chunk.endParagraph ?? "-"}
                      </Text>
                    }
                  />
                  <Paragraph style={{ whiteSpace: "pre-wrap", marginBottom: 0 }}>
                    {previewContent(chunk)}
                  </Paragraph>
                </List.Item>
              )}
            />
          )}
        </Skeleton>
      </Card>
    </section>
  );
}
