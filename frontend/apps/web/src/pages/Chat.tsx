import { chat, getDocuments } from "@my-rag/api";
import type { ChatRequest, ChatResponse, DocumentSummary } from "@my-rag/types";
import { useMutation, useQuery } from "@tanstack/react-query";
import {
  Button,
  Card,
  Input,
  Space,
  Typography,
  List,
  Checkbox,
  Spin,
  Alert,
  Empty,
} from "antd";
import { Send, MessageSquare } from "lucide-react";
import { useState } from "react";
import DocumentStatusBadge from "../components/DocumentStatusBadge";
import ChatMessage from "../components/ChatMessage";

const { Text } = Typography;

export default function Chat() {
  const [question, setQuestion] = useState("");
  const [selectedDocumentIds, setSelectedDocumentIds] = useState<number[]>([]);
  const [messages, setMessages] = useState<
    Array<{
      role: "user" | "assistant";
      content: string;
      response?: ChatResponse;
    }>
  >([]);

  const { data: documentsData, isLoading: documentsLoading } = useQuery({
    queryKey: ["documents"],
    queryFn: getDocuments,
  });

  const documents = documentsData?.data ?? [];
  const readyDocuments = documents.filter((d) => d.status === "READY");

  const chatMutation = useMutation({
    mutationFn: (payload: ChatRequest) => chat(payload),
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
          content: `抱歉，出错了：${error.message}`,
        },
      ]);
    },
  });

  const handleSubmit = () => {
    if (!question.trim()) {
      return;
    }

    const userMessage = question.trim();
    setMessages((prev) => [...prev, { role: "user", content: userMessage }]);
    setQuestion("");

    const payload: ChatRequest = {
      question: userMessage,
      documentIds: selectedDocumentIds.length > 0 ? selectedDocumentIds : undefined,
    };

    chatMutation.mutate(payload);
  };

  const toggleDocument = (docId: number) => {
    setSelectedDocumentIds((prev) =>
      prev.includes(docId) ? prev.filter((id) => id !== docId) : [...prev, docId]
    );
  };

  const selectAll = () => {
    setSelectedDocumentIds(readyDocuments.map((d) => d.documentId));
  };

  const clearSelection = () => {
    setSelectedDocumentIds([]);
  };

  return (
    <section className="page">
      <div className="page-heading">
        <div>
          <h1>问答</h1>
          <p>所有回答都应该基于召回片段，并且带来源。</p>
        </div>
      </div>

      <Card title="选择文档（仅显示 READY 状态的文档）" size="small" style={{ marginBottom: 16 }}>
        {documentsLoading ? (
          <Spin tip="加载文档列表中..." />
        ) : readyDocuments.length === 0 ? (
          <Empty description="暂无 READY 文档，请先上传并索引文档。" />
        ) : (
            <>
          <Space style={{ marginBottom: 12 }}>
            <Button size="small" onClick={selectAll}>
              全选
            </Button>
            <Button size="small" onClick={clearSelection}>
              清除选择
            </Button>
            <Text type="secondary" style={{ marginLeft: 8 }}>
              已选择 {selectedDocumentIds.length}/{readyDocuments.length} 个文档
            </Text>
          </Space>
          <List
            dataSource={readyDocuments}
            renderItem={(doc) => (
              <List.Item>
                <List.Item.Meta
                  avatar={
                    <Checkbox
                      checked={selectedDocumentIds.includes(doc.documentId)}
                      onChange={() => toggleDocument(doc.documentId)}
                    />
                  }
                  title={
                    <Space>
                      <span>{doc.title}</span>
                      <DocumentStatusBadge status={doc.status} />
                    </Space>
                  }
                  description={doc.fileName}
                />
              </List.Item>
            )}
          />
        </>
      )}
      </Card>

      <Card>
        <Space.Compact style={{ width: "100%", marginBottom: 24 }}>
          <Input
            value={question}
            onChange={(event) => setQuestion(event.target.value)}
            placeholder="问问这本书如何解释上下文管理"
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
            提问
          </Button>
        </Space.Compact>

        {messages.map((msg, idx) => (
          <ChatMessage
            key={idx}
            role={msg.role}
            content={msg.content}
            response={msg.response}
          />
        ))}
      </Card>
    </section>
  );
}
