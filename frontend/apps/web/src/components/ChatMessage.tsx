import type { ChatResponse } from "@my-rag/types";
import { Alert, Card, Spin, Typography } from "antd";
import { AlertCircle, Bot, Loader2, User } from "lucide-react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import SourceList from "./SourceList";

interface ChatMessageProps {
  role: "user" | "assistant";
  content: string;
  response?: ChatResponse;
  isLoading?: boolean;
}

export default function ChatMessage({ role, content, response, isLoading = false }: ChatMessageProps) {
  const isUser = role === "user";

  return (
    <div style={{ display: "flex", gap: 12, marginBottom: 24 }}>
      <div
        style={{
          width: 40,
          height: 40,
          borderRadius: "50%",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          backgroundColor: isUser ? "#1677ff" : "#52c41a",
          color: "white",
          flexShrink: 0,
        }}
      >
        {isLoading ? <Loader2 size={20} /> : isUser ? <User size={20} /> : <Bot size={20} />}
      </div>
      <div style={{ flex: 1 }}>
        <Typography.Text strong style={{ display: "block", marginBottom: 8 }}>
          {isUser ? "You" : "Assistant"}
        </Typography.Text>

        {isLoading ? (
          <Card size="small">
            <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
              <Spin indicator={<Loader2 size={20} />} />
              <Typography.Text>Thinking...</Typography.Text>
            </div>
          </Card>
        ) : !isUser && response?.noAnswer ? (
          <Alert
            message="No reliable answer found"
            description="Try rephrasing the question, selecting different documents, or checking that embedding has completed."
            type="warning"
            icon={<AlertCircle />}
            showIcon
          />
        ) : (
          <Card size="small">
            <div className="prose prose-slate">
              {isUser ? (
                <Typography.Paragraph style={{ marginBottom: 0, whiteSpace: "pre-wrap" }}>
                  {content}
                </Typography.Paragraph>
              ) : (
                <ReactMarkdown remarkPlugins={[remarkGfm]}>{content}</ReactMarkdown>
              )}
            </div>
          </Card>
        )}

        {!isUser && !isLoading && response && !response.noAnswer ? <SourceList sources={response.sources} /> : null}
      </div>
    </div>
  );
}
