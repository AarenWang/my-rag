import type { ChatResponse } from "@my-rag/types";
import { Alert, Card, Typography } from "antd";
import { CheckCircle, AlertCircle, Bot, User } from "lucide-react";
import SourceList from "./SourceList";

interface ChatMessageProps {
  role: "user" | "assistant";
  content: string;
  response?: ChatResponse;
}

export default function ChatMessage({
  role,
  content,
  response,
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
        {isUser ? <User size={20} /> : <Bot size={20} />}
      </div>
      <div style={{ flex: 1 }}>
        <Typography.Text strong style={{ display: "block", marginBottom: 8 }}>
          {isUser ? "用户" : "AI 助手"}
        </Typography.Text>

        {!isUser && response?.noAnswer ? (
          <Alert
            message="当前资料中没有找到明确依据"
            description="请尝试换一种问法，或检查文档是否已成功索引。"
            type="warning"
            icon={<AlertCircle />}
            showIcon
          />
        ) : (
          <Card size="small">
            <Typography.Paragraph style={{ marginBottom: 0, whiteSpace: "pre-wrap" }}>
              {content}
            </Typography.Paragraph>
          </Card>
        )}

        {!isUser && response && !response.noAnswer && (
          <SourceList sources={response.sources} />
        )}
      </div>
    </div>
  );
}
