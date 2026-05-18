import type { ChatSource } from "@my-rag/types";
import { Card, List, Tag } from "antd";
import { BookOpen } from "lucide-react";

interface SourceListProps {
  sources: ChatSource[];
}

export default function SourceList({ sources }: SourceListProps) {
  if (!sources || sources.length === 0) {
    return null;
  }

  return (
    <Card title="Sources" size="small" style={{ marginTop: 16 }}>
      <List
        itemLayout="horizontal"
        dataSource={sources}
        renderItem={(source) => (
          <List.Item>
            <List.Item.Meta
              avatar={<BookOpen size={20} style={{ marginTop: 4 }} />}
              title={
                <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                  <span>{source.documentTitle}</span>
                  {source.chapterTitle ? <Tag color="blue">{source.chapterTitle}</Tag> : null}
                </div>
              }
              description={
                <div style={{ display: "flex", gap: 12, fontSize: 12 }}>
                  <span>Chunk #{source.chunkIndex}</span>
                  <span>ID: {source.chunkId}</span>
                  <span>Score {(source.score * 100).toFixed(1)}%</span>
                </div>
              }
            />
          </List.Item>
        )}
      />
    </Card>
  );
}
