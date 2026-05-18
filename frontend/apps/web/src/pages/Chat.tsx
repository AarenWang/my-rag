import { chat } from "@my-rag/api";
import { useMutation } from "@tanstack/react-query";
import { Button, Card, Input, List, Space, Typography } from "antd";
import { useState } from "react";

export default function Chat() {
  const [question, setQuestion] = useState("");
  const chatMutation = useMutation({
    mutationFn: chat
  });

  return (
    <section className="page">
      <div className="page-heading">
        <div>
          <h1>问答</h1>
          <p>所有回答都应该基于召回片段，并且带来源。</p>
        </div>
      </div>
      <Card>
        <Space.Compact style={{ width: "100%" }}>
          <Input
            value={question}
            onChange={(event) => setQuestion(event.target.value)}
            placeholder="问问这本书如何解释上下文管理"
          />
          <Button
            type="primary"
            loading={chatMutation.isPending}
            onClick={() => chatMutation.mutate({ question })}
          >
            提问
          </Button>
        </Space.Compact>
        {chatMutation.data && (
          <div className="answer-card">
            <Typography.Paragraph>{chatMutation.data.data.answer}</Typography.Paragraph>
            <List
              size="small"
              header="引用来源"
              dataSource={chatMutation.data.data.sources}
              renderItem={(source) => (
                <List.Item>
                  {source.documentTitle} / {source.chapterTitle} / chunk #{source.chunkIndex}
                </List.Item>
              )}
            />
          </div>
        )}
      </Card>
    </section>
  );
}

