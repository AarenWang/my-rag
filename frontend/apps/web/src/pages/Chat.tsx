import { chat, getCollections, getDocuments } from "@my-rag/api";
import type { ChatRequest, ChatResponse } from "@my-rag/types";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Button, Card, Checkbox, Empty, Input, List, Select, Space, Spin, Typography, message } from "antd";
import { MessageSquare, Send } from "lucide-react";
import { useState } from "react";
import ChatMessage from "../components/ChatMessage";
import DocumentStatusBadge from "../components/DocumentStatusBadge";

const { Text } = Typography;

interface ChatTurn {
  role: "user" | "assistant";
  content: string;
  response?: ChatResponse;
}

export default function Chat() {
  const [question, setQuestion] = useState("");
  const [selectedCollectionIds, setSelectedCollectionIds] = useState<number[]>([]);
  const [selectedDocumentIds, setSelectedDocumentIds] = useState<number[]>([]);
  const [messages, setMessages] = useState<ChatTurn[]>([]);
  const [isTyping, setIsTyping] = useState(false);

  const { data: documentsData, isLoading: documentsLoading } = useQuery({
    queryKey: ["documents"],
    queryFn: getDocuments,
  });

  const { data: collectionsData } = useQuery({
    queryKey: ["collections"],
    queryFn: () => getCollections(false),
  });

  const readyDocuments = (documentsData?.data ?? []).filter((document) => document.status === "READY");
  const collections = collectionsData?.data ?? [];

  const chatMutation = useMutation({
    mutationFn: (payload: ChatRequest) => chat(payload),
    onMutate: () => {
      setIsTyping(true);
    },
    onSuccess: (result) => {
      setMessages((prev) => [
        ...prev,
        {
          role: "assistant",
          content: result.data.answer,
          response: result.data,
        },
      ]);
    },
    onError: (error: Error) => {
      setMessages((prev) => [
        ...prev,
        {
          role: "assistant",
          content: `Sorry, something went wrong: ${error.message}`,
        },
      ]);
    },
    onSettled: () => {
      setIsTyping(false);
    },
  });

  const handleSubmit = () => {
    const userMessage = question.trim();
    if (!userMessage) {
      message.warning("Please enter a question.");
      return;
    }

    setMessages((prev) => [...prev, { role: "user", content: userMessage }]);
    setQuestion("");

    const payload: ChatRequest = {
      question: userMessage,
      collectionIds: selectedCollectionIds.length > 0 ? selectedCollectionIds : undefined,
      documentIds: selectedDocumentIds.length > 0 ? selectedDocumentIds : undefined,
    };

    chatMutation.mutate(payload);
  };

  const toggleDocument = (documentId: number) => {
    setSelectedDocumentIds((prev) =>
      prev.includes(documentId) ? prev.filter((id) => id !== documentId) : [...prev, documentId],
    );
  };

  const selectAll = () => {
    setSelectedDocumentIds(readyDocuments.map((document) => document.documentId));
  };

  const clearSelection = () => {
    setSelectedDocumentIds([]);
  };

  const inputBar = (
    <Space.Compact style={{ width: "100%", marginTop: messages.length > 0 || isTyping ? 8 : 0 }}>
      <Input
        value={question}
        onChange={(event) => setQuestion(event.target.value)}
        placeholder="Ask a question about your indexed documents..."
        onPressEnter={handleSubmit}
        disabled={chatMutation.isPending}
        prefix={<MessageSquare size={16} />}
      />
      <Button
        type="primary"
        icon={<Send size={16} />}
        loading={chatMutation.isPending}
        onClick={handleSubmit}
        disabled={!question.trim()}
      >
        Ask
      </Button>
    </Space.Compact>
  );

  return (
    <section className="page">
      <div className="page-heading">
        <div>
          <h1>Chat</h1>
          <p>Answers are generated from retrieved document chunks and include source references.</p>
        </div>
      </div>

      {collections.length > 0 && (
        <Card title="Select collections (optional)" size="small" style={{ marginBottom: 16 }}>
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

      <Card title="Select documents (READY only)" size="small" style={{ marginBottom: 16 }}>
        {documentsLoading ? (
          <Spin tip="Loading documents..." />
        ) : readyDocuments.length === 0 ? (
          <Empty description="No READY documents yet. Upload documents and finish embedding first." />
        ) : (
          <>
            <Space style={{ marginBottom: 12 }}>
              <Button size="small" onClick={selectAll} disabled={selectedCollectionIds.length > 0}>
                Select all
              </Button>
              <Button size="small" onClick={clearSelection} disabled={selectedCollectionIds.length > 0}>
                Clear
              </Button>
              <Text type="secondary" style={{ marginLeft: 8 }}>
                Selected {selectedDocumentIds.length}/{readyDocuments.length} documents
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

      <Card>
        {messages.length > 0 || isTyping ? (
          <div style={{ marginBottom: 16 }}>
            {messages.map((chatMessage, index) => (
              <ChatMessage
                key={`${chatMessage.role}-${index}`}
                role={chatMessage.role}
                content={chatMessage.content}
                response={chatMessage.response}
              />
            ))}
            {isTyping ? <ChatMessage key="loading" role="assistant" content="" isLoading /> : null}
          </div>
        ) : null}
        {inputBar}
      </Card>
    </section>
  );
}
