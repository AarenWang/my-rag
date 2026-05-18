import { getChatLogDetail } from "@my-rag/api";
import type { ChatLogDetail } from "@my-rag/types";
import { Card, Typography, Descriptions, Divider, Spin, Tag, Button, Result } from "antd";
import { useQuery } from "@tanstack/react-query";
import { useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, MessageSquare, Calendar, Clock, FileText } from "lucide-react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";

const { Title, Text } = Typography;

export default function ChatLogDetailPage() {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();

  const { data: logData, isLoading: logLoading, error } = useQuery({
    queryKey: ["chat-log-detail", id],
    queryFn: () => getChatLogDetail(Number(id)),
    enabled: !!id,
  });

  const log = logData?.data as ChatLogDetail | undefined;

  if (error) {
    return (
      <div style={{ maxWidth: 900, margin: "0 auto", padding: "48px 0" }}>
        <Result
          status="error"
          title="加载失败"
          subTitle={`无法加载问答记录: ${(error as Error).message}`}
          extra={
            <Button type="primary" onClick={() => navigate("/chat-logs")}>
              <ArrowLeft size={16} style={{ marginRight: 8 }} />
              返回列表
            </Button>
          }
        />
      </div>
    );
  }

  return (
    <div style={{ maxWidth: 900, margin: "0 auto", padding: "24px 0" }}>
      <div style={{ marginBottom: 24 }}>
        <Button type="text" icon={<ArrowLeft size={16} />} onClick={() => navigate("/chat-logs")}>
          返回列表
        </Button>
      </div>

      {logLoading ? (
        <div style={{ textAlign: "center", padding: "48px" }}>
          <Spin size="large" />
        </div>
      ) : !log ? (
        <Result status="warning" title="记录不存在" />
      ) : (
        <>
          <Title level={3}>
            <MessageSquare style={{ marginRight: 12, display: "inline-block", verticalAlign: "middle" }} />
            问答详情
          </Title>

          <Descriptions column={2} size="small" style={{ marginTop: 24, marginBottom: 24 }}>
            <Descriptions.Item label="创建时间" labelStyle={{ fontWeight: 600 }}>
              <Tag icon={<Calendar size={12} />}>
                {new Date(log.createdAt).toLocaleString("zh-CN")}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="耗时" labelStyle={{ fontWeight: 600 }}>
              {log.latencyMs ? (
                <Tag icon={<Clock size={12} />} color="blue">
                  {log.latencyMs} ms
                </Tag>
              ) : (
                "-"
              )}
            </Descriptions.Item>
            <Descriptions.Item label="检索参数" labelStyle={{ fontWeight: 600 }}>
              <Tag>TopK: {log.topK ?? "-"}</Tag>
              {log.minScore && <Tag color="orange">MinScore: {log.minScore.toFixed(2)}</Tag>}
            </Descriptions.Item>
            <Descriptions.Item label="文档ID" labelStyle={{ fontWeight: 600 }}>
              <Tag icon={<FileText size={12} />}>{log.documentIds || "-"}</Tag>
            </Descriptions.Item>
          </Descriptions>

          <Divider />

          <Card title="用户问题" size="small" style={{ marginBottom: 16 }}>
            <Text strong style={{ fontSize: 16 }}>{log.question}</Text>
          </Card>

          <Card title="AI 回答" size="small">
            <div className="prose prose-slate">
              <ReactMarkdown remarkPlugins={[remarkGfm]}>{log.answer}</ReactMarkdown>
            </div>
          </Card>

          {log.retrievedChunkIds && log.retrievedChunkIds.length > 0 && (
            <>
              <Divider />
              <Card title="检索到的 Chunk" size="small">
                <div style={{ display: "flex", flexWrap: "wrap", gap: 8 }}>
                  {log.retrievedChunkIds.map((chunkId, idx) => (
                    <Tag key={idx} color="purple">Chunk #{chunkId}</Tag>
                  ))}
                </div>
              </Card>
            </>
          )}
        </>
      )}
    </div>
  );
}
