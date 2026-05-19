import { useState } from "react";
import {
  getCollections,
  getDocumentEmbeddingEstimate,
  getDocuments,
  triggerDocumentEmbedding,
  triggerDocumentIndex,
  uploadDocument
} from "@my-rag/api";
import type { CollectionSummary, DocumentEmbeddingEstimate, DocumentSummary } from "@my-rag/types";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button, Card, Descriptions, Empty, Modal, Select, Space, Table, Tag, Typography, Upload, message } from "antd";
import { Eye, Inbox, PlayCircle, Sparkles } from "lucide-react";
import { useNavigate } from "react-router-dom";
import type { UploadProps } from "antd";
import DocumentStatusBadge from "../components/DocumentStatusBadge";

const { Text } = Typography;

function formatCost(value: number) {
  return value.toFixed(8).replace(/\.?0+$/, "");
}

function EstimateDetails({ estimate }: { estimate: DocumentEmbeddingEstimate }) {
  return (
    <Space direction="vertical" size="middle" style={{ width: "100%" }}>
      <Descriptions column={1} size="small" bordered>
        <Descriptions.Item label="Chunks">{estimate.chunkCount}</Descriptions.Item>
        <Descriptions.Item label="Estimated tokens">{estimate.estimatedTokens.toLocaleString()}</Descriptions.Item>
        <Descriptions.Item label="Price">CNY {estimate.pricePer1kTokens} / 1K tokens</Descriptions.Item>
        <Descriptions.Item label="Estimated cost">
          CNY {formatCost(Number(estimate.estimatedCostCny))}
        </Descriptions.Item>
        <Descriptions.Item label="Model">{estimate.model}</Descriptions.Item>
        <Descriptions.Item label="Dimension">{estimate.dimension}</Descriptions.Item>
      </Descriptions>
      <Text type="secondary">
        This is an estimate based on local chunk token counts. The final charge is subject to the DashScope bill.
      </Text>
    </Space>
  );
}

export default function Documents() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [loadingAction, setLoadingAction] = useState<string | null>(null);
  const [selectedCollectionId, setSelectedCollectionId] = useState<number | undefined>();

  const { data: collectionsData } = useQuery({
    queryKey: ["collections"],
    queryFn: () => getCollections(false),
    retry: false
  });

  const { data, isLoading } = useQuery({
    queryKey: ["documents"],
    queryFn: getDocuments,
    retry: false
  });

  const uploadMutation = useMutation({
    mutationFn: ({ file, collectionId }: { file: File; collectionId?: number }) =>
      uploadDocument(file, collectionId),
    onSuccess: (result) => {
      message.success(result.data.duplicate ? "Document already exists" : "Document uploaded");
      queryClient.invalidateQueries({ queryKey: ["documents"] });
      setSelectedCollectionId(undefined);
    },
    onError: (error: Error) => {
      message.error(`Upload failed: ${error.message}`);
    }
  });

  const indexMutation = useMutation({
    mutationFn: (documentId: number) => triggerDocumentIndex(documentId),
    onMutate: (documentId) => setLoadingAction(`index-${documentId}`),
    onSuccess: (result) => {
      message.success(result.data.message || "Index task submitted");
      queryClient.invalidateQueries({ queryKey: ["documents"] });
    },
    onError: (error: Error) => {
      message.error(`Index failed: ${error.message}`);
    },
    onSettled: () => setLoadingAction(null)
  });

  const embeddingMutation = useMutation({
    mutationFn: (documentId: number) => triggerDocumentEmbedding(documentId),
    onMutate: (documentId) => setLoadingAction(`embedding-${documentId}`),
    onSuccess: (result) => {
      message.success(result.data.message || "Embedding task submitted");
      queryClient.invalidateQueries({ queryKey: ["documents"] });
    },
    onError: (error: Error) => {
      message.error(`Embedding failed: ${error.message}`);
    },
    onSettled: () => setLoadingAction(null)
  });

  const uploadProps: UploadProps = {
    name: "file",
    showUploadList: false,
    beforeUpload: (file) => {
      uploadMutation.mutate({ file, collectionId: selectedCollectionId });
      return false;
    },
    accept: ".txt,.md,.markdown,.epub"
  };

  const allDocuments = data?.data ?? [];
  const collections = collectionsData?.data ?? [];

  const filteredDocuments = allDocuments.filter((doc) => {
    if (selectedCollectionId === undefined) {
      return true;
    }
    return doc.collectionId === selectedCollectionId;
  });

  const handleIndex = (documentId: number) => {
    Modal.confirm({
      title: "Confirm re-index",
      content: "This will parse the document and rebuild chunks. Embedding will require a separate confirmation.",
      okText: "Re-index",
      cancelText: "Cancel",
      onOk: () => indexMutation.mutate(documentId)
    });
  };

  const handleEmbedding = async (documentId: number) => {
    setLoadingAction(`estimate-${documentId}`);
    try {
      const result = await getDocumentEmbeddingEstimate(documentId);
      const estimate = result.data;
      if (estimate.chunkCount === 0) {
        message.warning("No chunks found. Run re-index first.");
        return;
      }

      Modal.confirm({
        title: "Confirm embedding cost",
        content: <EstimateDetails estimate={estimate} />,
        okText: "Run embedding",
        cancelText: "Cancel",
        width: 560,
        onOk: () => embeddingMutation.mutate(documentId)
      });
    } catch (error) {
      message.error(`Failed to estimate embedding cost: ${error instanceof Error ? error.message : "Unknown error"}`);
    } finally {
      setLoadingAction(null);
    }
  };

  const canIndex = (status: string) => ["UPLOADED", "PARSED", "CHUNKED", "READY", "FAILED"].includes(status);
  const canEmbed = (status: string) => ["CHUNKED", "READY", "FAILED"].includes(status);

  const columns = [
    { title: "Title", dataIndex: "title", key: "title" },
    { title: "File name", dataIndex: "fileName", key: "fileName" },
    {
      title: "Type",
      dataIndex: "fileType",
      key: "fileType",
      render: (type: string) => <Tag>{type.toUpperCase()}</Tag>
    },
    {
      title: "Status",
      dataIndex: "status",
      key: "status",
      render: (status: string) => <DocumentStatusBadge status={status} />
    },
    {
      title: "Actions",
      key: "actions",
      render: (_: unknown, record: DocumentSummary) => (
        <Space size="small" wrap>
          <Button size="small" icon={<Eye size={14} />} onClick={() => navigate(`/documents/${record.documentId}`)}>
            View
          </Button>
          <Button
            size="small"
            icon={<PlayCircle size={14} />}
            disabled={!canIndex(record.status)}
            loading={loadingAction === `index-${record.documentId}`}
            onClick={() => handleIndex(record.documentId)}
          >
            Re-index
          </Button>
          <Button
            size="small"
            type="primary"
            ghost
            icon={<Sparkles size={14} />}
            disabled={!canEmbed(record.status)}
            loading={
              loadingAction === `estimate-${record.documentId}` || loadingAction === `embedding-${record.documentId}`
            }
            onClick={() => handleEmbedding(record.documentId)}
          >
            Embedding
          </Button>
        </Space>
      )
    }
  ];

  return (
    <section className="page">
      <div className="page-heading">
        <div>
          <h1>Documents</h1>
          <p>Upload ebooks, rebuild chunks, and confirm embedding cost before vector generation.</p>
        </div>
        <Space>
          {collections.length > 0 && (
            <Select
              placeholder="Select collection"
              style={{ width: 200 }}
              allowClear
              value={selectedCollectionId}
              onChange={setSelectedCollectionId}
              options={[
                { label: "Default Collection", value: undefined },
                ...collections.map((c) => ({ label: c.name, value: c.collectionId }))
              ]}
            />
          )}
          <Upload {...uploadProps}>
            <Button type="primary" icon={<Inbox size={16} />} loading={uploadMutation.isPending}>
              {uploadMutation.isPending ? "Uploading..." : "Upload document"}
            </Button>
          </Upload>
        </Space>
      </div>
      <Card>
        <Table
          loading={isLoading}
          rowKey="documentId"
          dataSource={filteredDocuments}
          locale={{ emptyText: <Empty description="No documents yet." /> }}
          columns={columns}
        />
      </Card>
    </section>
  );
}
