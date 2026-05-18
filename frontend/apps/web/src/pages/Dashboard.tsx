import { getHealth } from "@my-rag/api";
import { useQuery } from "@tanstack/react-query";
import { Card, Col, Row, Statistic, Tag } from "antd";

export default function Dashboard() {
  const { data, isLoading } = useQuery({
    queryKey: ["health"],
    queryFn: getHealth,
    retry: false
  });

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
            <Statistic title="后端状态" value={isLoading ? "检查中" : data?.data.status ?? "未知"} />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card>
            <Statistic title="READY 文档" value={0} />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card>
            <Statistic title="失败任务" value={0} />
          </Card>
        </Col>
      </Row>
    </section>
  );
}

