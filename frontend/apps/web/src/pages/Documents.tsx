import { getDocuments } from "@my-rag/api";
import { useQuery } from "@tanstack/react-query";
import { Button, Card, Empty, Space, Table, Upload } from "antd";

export default function Documents() {
  const { data, isLoading } = useQuery({
    queryKey: ["documents"],
    queryFn: getDocuments,
    retry: false
  });

  return (
    <section className="page">
      <div className="page-heading">
        <div>
          <h1>文档</h1>
          <p>上传电子书、观察处理状态，后续在这里触发 index 流程。</p>
        </div>
        <Upload disabled>
          <Button type="primary">上传文档</Button>
        </Upload>
      </div>
      <Card>
        <Table
          loading={isLoading}
          rowKey="documentId"
          dataSource={data?.data ?? []}
          locale={{ emptyText: <Empty description="暂无文档，上传接口接好后这里会热闹起来。" /> }}
          columns={[
            { title: "标题", dataIndex: "title" },
            { title: "文件名", dataIndex: "fileName" },
            { title: "类型", dataIndex: "fileType" },
            { title: "状态", dataIndex: "status" },
            {
              title: "操作",
              render: () => (
                <Space>
                  <Button size="small" disabled>
                    查看
                  </Button>
                  <Button size="small" disabled>
                    Index
                  </Button>
                </Space>
              )
            }
          ]}
        />
      </Card>
    </section>
  );
}

