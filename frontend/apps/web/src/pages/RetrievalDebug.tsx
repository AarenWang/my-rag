import { debugRetrieval, getCollections, getDocuments } from "@my-rag/api";
import type { RetrievalDebugCandidate, RetrievalDebugRequest, RetrievalDebugResponse } from "@my-rag/types";
import { useMutation, useQuery } from "@tanstack/react-query";
import type { ColumnsType } from "antd/es/table";
import {
  Button,
  Card,
  Checkbox,
  Descriptions,
  Empty,
  Input,
  InputNumber,
  List,
  Select,
  Space,
  Spin,
  Table,
  Tabs,
  Tag,
  Typography,
  message
} from "antd";
import { Search, SlidersHorizontal } from "lucide-react";
import { useMemo, useState } from "react";
import DocumentStatusBadge from "../components/DocumentStatusBadge";

const { Paragraph, Text } = Typography;

function formatNumber(value: number | null | undefined) {
  if (value === null || value === undefined) {
    return "-";
  }
  return Number.isInteger(value) ? String(value) : value.toFixed(6).replace(/\.?0+$/, "");
}

function sourceTags(sources: string[] | null | undefined) {
  if (!sources || sources.length === 0) {
    return <Text type="secondary">-</Text>;
  }
  return (
    <Space size={[4, 4]} wrap>
      {sources.map((source) => (
        <Tag key={source}>{source}</Tag>
      ))}
    </Space>
  );
}

function CandidateTable({ data }: { data: RetrievalDebugCandidate[] }) {
  const columns: ColumnsType<RetrievalDebugCandidate> = [
    { title: "#", dataIndex: "rank", width: 60 },
    {
      title: "Chunk",
      key: "chunk",
      width: 120,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Text strong>{record.chunkId}</Text>
          <Text type="secondary">idx {record.chunkIndex}</Text>
        </Space>
      )
    },
    {
      title: "Document",
      key: "document",
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Text>{record.documentTitle}</Text>
          <Text type="secondary">{record.chapterTitle || "-"}</Text>
        </Space>
      )
    },
    {
      title: "Sources",
      dataIndex: "retrievalSources",
      width: 150,
      render: sourceTags
    },
    {
      title: "Ranks",
      key: "ranks",
      width: 150,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Text>vector {formatNumber(record.vectorRank)}</Text>
          <Text>keyword {formatNumber(record.keywordRank)}</Text>
        </Space>
      )
    },
    {
      title: "Scores",
      key: "scores",
      width: 190,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Text>score {formatNumber(record.score)}</Text>
          <Text>rrf {formatNumber(record.rrfScore)}</Text>
          <Text>final {formatNumber(record.finalScore)}</Text>
        </Space>
      )
    }
  ];

  return (
    <Table
      rowKey={(record) => `${record.rank}-${record.chunkId}`}
      size="small"
      columns={columns}
      dataSource={data}
      pagination={false}
      scroll={{ x: 900 }}
      expandable={{
        expandedRowRender: (record) => (
          <Paragraph style={{ margin: 0, whiteSpace: "pre-wrap" }}>{record.content}</Paragraph>
        )
      }}
      locale={{ emptyText: <Empty description="No candidates." /> }}
    />
  );
}

function DebugSummary({ result }: { result: RetrievalDebugResponse }) {
  return (
    <Card size="small" title="Pipeline">
      <Descriptions size="small" column={{ xs: 1, sm: 2, lg: 4 }} bordered>
        <Descriptions.Item label="Mode">{result.mode}</Descriptions.Item>
        <Descriptions.Item label="Keyword index">
          {result.keywordIndexEnabled ? <Tag color="green">enabled</Tag> : <Tag>disabled</Tag>}
        </Descriptions.Item>
        <Descriptions.Item label="Reranker">{result.config.rerankerProvider}</Descriptions.Item>
        <Descriptions.Item label="Score threshold">{formatNumber(result.config.scoreThreshold)}</Descriptions.Item>
        <Descriptions.Item label="TopK">{result.config.topK}</Descriptions.Item>
        <Descriptions.Item label="Vector TopK">{result.config.vectorTopK}</Descriptions.Item>
        <Descriptions.Item label="Keyword TopK">{result.config.keywordTopK}</Descriptions.Item>
        <Descriptions.Item label="RRF TopK">{result.config.rrfTopK}</Descriptions.Item>
        <Descriptions.Item label="Rerank TopK">{result.config.rerankTopK}</Descriptions.Item>
        <Descriptions.Item label="Context TopK">{result.config.contextTopK}</Descriptions.Item>
        <Descriptions.Item label="RRF K">{result.config.rrfK}</Descriptions.Item>
        <Descriptions.Item label="Max context chars">{result.config.maxContextChars}</Descriptions.Item>
      </Descriptions>
      <div style={{ marginTop: 12 }}>
        <Text type="secondary">Keyword queries: </Text>
        {result.keywordQueries.length > 0 ? (
          <Space size={[4, 4]} wrap>
            {result.keywordQueries.map((query) => (
              <Tag key={query}>{query}</Tag>
            ))}
          </Space>
        ) : (
          <Text type="secondary">-</Text>
        )}
      </div>
    </Card>
  );
}

function EvidenceList({ result }: { result: RetrievalDebugResponse }) {
  if (result.evidences.length === 0) {
    return <Empty description="No evidence selected for context." />;
  }
  return (
    <Space direction="vertical" size="middle" style={{ width: "100%" }}>
      {result.evidences.map((evidence) => (
        <Card
          key={evidence.sourceId}
          size="small"
          title={
            <Space wrap>
              <Tag color="blue">{evidence.sourceId}</Tag>
              <Text>{evidence.documentTitle}</Text>
              <Text type="secondary">{evidence.chapterTitle || "-"}</Text>
            </Space>
          }
          extra={<Text type="secondary">chunk {evidence.chunkId}</Text>}
        >
          <Space direction="vertical" size="small" style={{ width: "100%" }}>
            <Space wrap>
              {sourceTags(evidence.retrievalSources)}
              <Text type="secondary">final {formatNumber(evidence.finalScore)}</Text>
            </Space>
            <Paragraph style={{ margin: 0, whiteSpace: "pre-wrap" }}>{evidence.content}</Paragraph>
          </Space>
        </Card>
      ))}
    </Space>
  );
}

export default function RetrievalDebug() {
  const [question, setQuestion] = useState("");
  const [selectedCollectionIds, setSelectedCollectionIds] = useState<number[]>([]);
  const [selectedDocumentIds, setSelectedDocumentIds] = useState<number[]>([]);
  const [topK, setTopK] = useState<number | null>(null);
  const [scoreThreshold, setScoreThreshold] = useState<number | null>(null);

  const { data: documentsData, isLoading: documentsLoading } = useQuery({
    queryKey: ["documents"],
    queryFn: getDocuments
  });

  const { data: collectionsData } = useQuery({
    queryKey: ["collections"],
    queryFn: () => getCollections(false)
  });

  const readyDocuments = useMemo(
    () => (documentsData?.data ?? []).filter((document) => document.status === "READY"),
    [documentsData]
  );

  const collections = collectionsData?.data ?? [];

  const debugMutation = useMutation({
    mutationFn: (payload: RetrievalDebugRequest) => debugRetrieval(payload),
    onError: (error: Error) => {
      message.error(`Debug failed: ${error.message}`);
    }
  });

  const toggleDocument = (documentId: number) => {
    setSelectedDocumentIds((prev) =>
      prev.includes(documentId) ? prev.filter((id) => id !== documentId) : [...prev, documentId]
    );
  };

  const handleSubmit = () => {
    const trimmedQuestion = question.trim();
    if (!trimmedQuestion) {
      message.warning("Please enter a question.");
      return;
    }
    debugMutation.mutate({
      question: trimmedQuestion,
      collectionIds: selectedCollectionIds.length > 0 ? selectedCollectionIds : undefined,
      documentIds: selectedDocumentIds.length > 0 ? selectedDocumentIds : undefined,
      topK: topK && topK > 0 ? topK : undefined,
      scoreThreshold: scoreThreshold ?? undefined
    });
  };

  const result = debugMutation.data?.data;

  return (
    <section className="page">
      <div className="page-heading">
        <div>
          <h1>Retrieval Debug</h1>
          <p>Inspect recall, ranking, reranking, and final evidence for a question.</p>
        </div>
      </div>

      <Card>
        <Space direction="vertical" size="middle" style={{ width: "100%" }}>
          <Input.TextArea
            value={question}
            onChange={(event) => setQuestion(event.target.value)}
            placeholder="Enter a question..."
            autoSize={{ minRows: 2, maxRows: 5 }}
          />
          <Space wrap>
            <InputNumber
              min={1}
              max={100}
              value={topK}
              onChange={setTopK}
              placeholder="TopK"
              prefix={<SlidersHorizontal size={14} />}
            />
            <InputNumber
              min={0}
              max={1}
              step={0.01}
              value={scoreThreshold}
              onChange={setScoreThreshold}
              placeholder="Score threshold"
            />
            <Button
              type="primary"
              icon={<Search size={16} />}
              loading={debugMutation.isPending}
              onClick={handleSubmit}
            >
              Run debug
            </Button>
          </Space>
        </Space>
      </Card>

      {collections.length > 0 && (
        <Card title="Collection scope (optional)" size="small">
          <Space direction="vertical" style={{ width: "100%" }}>
            <Text type="secondary">
              Select collections to limit search scope. When collections are selected, document selection will be ignored.
            </Text>
            <Select
              mode="multiple"
              placeholder="Select collections to search within"
              style={{ width: "100%" }}
              value={selectedCollectionIds}
              onChange={setSelectedCollectionIds}
              options={collections.map((c) => ({
                label: `${c.name} (${c.readyDocumentCount}/${c.documentCount} docs)`,
                value: c.collectionId
              }))}
            />
            {selectedCollectionIds.length > 0 && (
              <Button size="small" onClick={() => setSelectedCollectionIds([])}>
                Clear collection selection
              </Button>
            )}
          </Space>
        </Card>
      )}

      <Card title="Document scope" size="small">
        {documentsLoading ? (
          <Spin tip="Loading documents..." />
        ) : readyDocuments.length === 0 ? (
          <Empty description="No READY documents yet." />
        ) : (
          <>
            <Space style={{ marginBottom: 12 }}>
              <Button size="small" onClick={() => setSelectedDocumentIds(readyDocuments.map((item) => item.documentId))}>
                Select all
              </Button>
              <Button size="small" onClick={() => setSelectedDocumentIds([])}>
                Clear
              </Button>
              <Text type="secondary">
                Selected {selectedDocumentIds.length}/{readyDocuments.length}
              </Text>
            </Space>
            <List
              dataSource={readyDocuments}
              renderItem={(document) => (
                <List.Item>
                  <List.Item.Meta
                    avatar={
                      <Checkbox
                        checked={selectedDocumentIds.includes(document.documentId)}
                        onChange={() => toggleDocument(document.documentId)}
                      />
                    }
                    title={
                      <Space>
                        <span>{document.title}</span>
                        <DocumentStatusBadge status={document.status} />
                      </Space>
                    }
                    description={document.fileName}
                  />
                </List.Item>
              )}
            />
          </>
        )}
      </Card>

      {result ? (
        <>
          <DebugSummary result={result} />
          <Card>
            <Tabs
              items={[
                {
                  key: "vector",
                  label: `Vector (${result.vectorCandidates.length})`,
                  children: <CandidateTable data={result.vectorCandidates} />
                },
                {
                  key: "keyword",
                  label: `Keyword (${result.keywordCandidates.length})`,
                  children: <CandidateTable data={result.keywordCandidates} />
                },
                {
                  key: "rrf",
                  label: `RRF (${result.rrfCandidates.length})`,
                  children: <CandidateTable data={result.rrfCandidates} />
                },
                {
                  key: "rerank",
                  label: `Rerank (${result.rerankedCandidates.length})`,
                  children: <CandidateTable data={result.rerankedCandidates} />
                },
                {
                  key: "evidence",
                  label: `Evidence (${result.evidences.length})`,
                  children: <EvidenceList result={result} />
                }
              ]}
            />
          </Card>
        </>
      ) : null}
    </section>
  );
}
