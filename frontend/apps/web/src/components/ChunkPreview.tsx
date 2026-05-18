import type { DocumentChunk } from "@my-rag/types";
import { Card, Descriptions, Typography } from "antd";

interface ChunkPreviewProps {
  chunk: DocumentChunk;
}

export default function ChunkPreview({ chunk }: ChunkPreviewProps) {
  return (
    <Card size="small">
      <Descriptions column={2} size="small">
        <Descriptions.Item label="Chunk">{chunk.chunkIndex}</Descriptions.Item>
        {chunk.chapterTitle && (
          <Descriptions.Item label="章节">{chunk.chapterTitle}</Descriptions.Item>
        )}
        {chunk.tokenCount && (
          <Descriptions.Item label="Token 数">{chunk.tokenCount}</Descriptions.Item>
        )}
      </Descriptions>
      <Typography.Paragraph
        style={{
          marginTop: 12,
          marginBottom: 0,
          fontSize: 13,
          lineHeight: 1.6,
          color: "#595959",
          maxHeight: 120,
          overflow: "auto",
        }}
      >
        {chunk.contentPreview || chunk.content}
      </Typography.Paragraph>
    </Card>
  );
}
