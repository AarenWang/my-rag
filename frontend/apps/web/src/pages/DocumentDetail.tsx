import { Card, Empty } from "antd";
import { useParams } from "react-router-dom";

export default function DocumentDetail() {
  const { documentId } = useParams();

  return (
    <section className="page">
      <div className="page-heading">
        <div>
          <h1>文档详情</h1>
          <p>当前文档 ID：{documentId}</p>
        </div>
      </div>
      <Card>
        <Empty description="Chunk 查询接口实现后，这里展示章节、片段和来源 metadata。" />
      </Card>
    </section>
  );
}

