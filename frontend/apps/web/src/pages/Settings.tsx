import { Card, Descriptions } from "antd";

export default function Settings() {
  return (
    <section className="page">
      <div className="page-heading">
        <div>
          <h1>设置</h1>
          <p>先展示默认配置，后续再做可编辑。</p>
        </div>
      </div>
      <Card>
        <Descriptions column={1} bordered>
          <Descriptions.Item label="API Base URL">VITE_API_BASE_URL 或 /api</Descriptions.Item>
          <Descriptions.Item label="Chunk">300 - 1000 字，overlap 150 字</Descriptions.Item>
          <Descriptions.Item label="Retrieval">topK 20，contextTopK 8，scoreThreshold 0.35</Descriptions.Item>
        </Descriptions>
      </Card>
    </section>
  );
}

