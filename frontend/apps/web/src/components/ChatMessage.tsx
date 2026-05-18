import type { ChatResponse } from "@my-rag/types";
import { Alert, Card, Typography, Spin } from "antd";
import { CheckCircle, AlertCircle, Bot, User, Loader2 } from "lucide-react";
import SourceList from "./SourceList";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";

interface ChatMessageProps {
  role: "user" | "assistant";
  content: string;
  response?: ChatResponse;
  isLoading?: boolean;
}

export default function ChatMessage({
  role,
  content,
  response,
  isLoading = false,
}: ChatMessageProps) {
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
        {isLoading ? (
          <Loader2 size={20} className="animate-spin" />
        ) : isUser ? (
          <User size={20} />
        ) : (
          <Bot size={20} />
        )}
      </div>
      <div style={{ flex: 1 }}>
        <Typography.Text strong style={{ display: "block", marginBottom: 8 }}>
          {isUser ? "用户" : "AI 助手"}
        </Typography.Text>

        {isLoading ? (
          <Card size="small">
            <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
              <Spin indicator={<Loader2 size={20} className="animate-spin" />} />
              <Typography.Text>正在思考中...</Typography.Text>
            </div>
          </Card>
        ) : !isUser && response?.noAnswer ? (
          <Alert
            message="当前资料中没有找到明确依据"
            description="请尝试换一种问法，或检查文档是否已成功索引。"
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

        {!isUser && !isLoading && response && !response.noAnswer && (
          <SourceList sources={response.sources} />
        )}
      </div>
    </div>
  );
}
