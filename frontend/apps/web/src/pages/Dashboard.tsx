import { getHealth, getDocuments } from "@my-rag/api";
import { useQuery } from "@tanstack/react-query";
import { Card, Col, Row, Statistic, Tag, Empty, Typography } from "antd";
import {
  FileText,
  CheckCircle,
  AlertCircle,
  Scissors,
  Cpu,
  Database,
} from "lucide-react";

export default function Dashboard() {
  const { data: healthData, isLoading: healthLoading } = useQuery({
    queryKey: ["health"],
    queryFn: getHealth,
    retry: false,
  });

  const { data: documentsData, isLoading: documentsLoading } = useQuery({
    queryKey: ["documents"],
    queryFn: getDocuments,
    retry: false,
  });

  const documents = documentsData?.data ?? [];

  const total = documents.length;
  const ready = documents.filter((d) => d.status === "READY").length;
  const failed = documents.filter((d) => d.status === "FAILED").length;
  const chunked = documents.filter((d) => d.status === "CHUNKED").length;
  const processing = documents.filter((d) =>
    ["PARSING", "CHUNKING", "EMBEDDING"].includes(d.status)
  ).length;

  return (
    <section className="page">
      <div className="hero-card">
        <Tag color="green">MVP</Tag>
        <h1>把电子书变成可引用的知识库</h1>
        <p>先让上传、解析、切片、检索、回答这条链路安静地跑起来。后面再加更花的魔法。</p>
      </div>
      <Row gutter={[16, 16]}>
        <Col xs={24} md={8}>
          <Card>
            <Statistic
              title="后端状态"
              value={healthLoading ? "检查中" : healthData?.data.status ?? "未知"}
              prefix={<Database size={20} />}
            />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card>
            <Statistic
              title="文档总数"
              value={total}
              prefix={<FileText size={20} />}
              loading={documentsLoading}
            />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card>
            <Statistic
              title="READY"
              value={ready}
              valueStyle={{ color: "#3f8600" }}
              prefix={<CheckCircle size={20} />}
              loading={documentsLoading}
            />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card>
            <Statistic
              title="CHUNKED"
              value={chunked}
              valueStyle={{ color: "#1677ff" }}
              prefix={<Scissors size={20} />}
              loading={documentsLoading}
            />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card>
            <Statistic
              title="处理中"
              value={processing}
              valueStyle={{ color: "#faad14" }}
              prefix={<Cpu size={20} />}
              loading={documentsLoading}
            />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card>
            <Statistic
              title="失败"
              value={failed}
              valueStyle={{ color: "#cf1322" }}
              prefix={<AlertCircle size={20} />}
              loading={documentsLoading}
            />
          </Card>
        </Col>
      </Row>
      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24}>
          <Card title="最近问答">
            <Empty description="Chat log API not available yet" />
          </Card>
        </Col>
      </Row>
    </section>
  );
}
