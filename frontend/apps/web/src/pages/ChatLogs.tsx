import { listChatLogs } from "@my-rag/api";
import type { ChatLogSummary } from "@my-rag/types";
import { Card, List, Typography, Empty, Spin, Tag } from "antd";
import { useQuery } from "@tanstack/react-query";
import { MessageSquare, Calendar, Eye } from "lucide-react";
import { useNavigate } from "react-router-dom";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";

const { Title, Text } = Typography;

export default function ChatLogs() {
  const navigate = useNavigate();

  const { data: logsData, isLoading: logsLoading } = useQuery({
    queryKey: ["chat-logs"],
    queryFn: listChatLogs,
  });

  const logs = logsData?.data ?? [];

  return (
    <div style={{ maxWidth: 900, margin: "0 auto", padding: "24px 0" }}>
      <Title level={2}>
        <MessageSquare style={{ marginRight: 12, display: "inline-block", verticalAlign: "middle" }} />
        历史问答
      </Title>

      {logsLoading ? (
        <div style={{ textAlign: "center", padding: "48px" }}>
          <Spin size="large" />
        </div>
      ) : logs.length === 0 ? (
        <Card style={{ marginTop: 24 }}>
          <Empty
            description="暂无历史问答记录"
          />
        </Card>
      ) : (
        <List
          style={{ marginTop: 24 }}
          itemLayout="horizontal"
          dataSource={logs}
          renderItem={(log) => (
            <List.Item
              actions={[
                <a key="view" onClick={() => navigate(`/chat-logs/${log.id}`)}>
                  <Eye size={16} style={{ marginRight: 4 }} />
                  查看详情
                </a>
              ]}
            >
              <List.Item.Meta
                avatar={<MessageSquare size={32} style={{ color: "#1677ff" }} />}
                title={
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <Text strong>{log.question}</Text>
                    <Tag icon={<Calendar size={12} />}>
                      {new Date(log.createdAt).toLocaleString("zh-CN")}
                    </Tag>
                  </div>
                }
                description={
                  <div className="prose prose-sm" style={{ marginTop: 8 }}>
                    <ReactMarkdown remarkPlugins={[remarkGfm]}>
                      {log.answerPreview}
                    </ReactMarkdown>
                  </div>
                }
              />
            </List.Item>
          )}
        />
      )}
    </div>
  );
}
