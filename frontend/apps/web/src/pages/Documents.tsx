import { useState } from "react";
import { getDocuments, uploadDocument, triggerDocumentIndex } from "@my-rag/api";
import type { DocumentSummary } from "@my-rag/types";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Button, Card, Empty, Space, Table, Upload, message, Tag, Modal } from "antd";
import { Inbox, Eye, PlayCircle, CheckCircle, AlertCircle, Loader2 } from "lucide-react";
import { useNavigate } from "react-router-dom";
import type { UploadProps } from "antd";

const StatusTag = ({ status }: { status: string }) => {
  const statusConfig: Record<string, { color: string; icon: React.ReactNode; text: string }> = {
    UPLOADED: { color: "blue", icon: <Inbox size={12} />, text: "已上传" },
    PARSING: { color: "cyan", icon: <Loader2 size={12} className="animate-spin" />, text: "解析中" },
    PARSED: { color: "purple", icon: <CheckCircle size={12} />, text: "已解析" },
    CHUNKING: { color: "orange", icon: <Loader2 size={12} className="animate-spin" />, text: "切分中" },
    CHUNKED: { color: "geekblue", icon: <CheckCircle size={12} />, text: "已切分" },
    EMBEDDING: { color: "gold", icon: <Loader2 size={12} className="animate-spin" />, text: "向量化中" },
    READY: { color: "green", icon: <CheckCircle size={12} />, text: "就绪" },
    FAILED: { color: "red", icon: <AlertCircle size={12} />, text: "失败" }
  };

  const config = statusConfig[status] || { color: "default", icon: null, text: status };

  return (
    <Tag color={config.color} icon={config.icon}>
      {config.text}
    </Tag>
  );
};

export default function Documents() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [confirmLoading, setConfirmLoading] = useState<number | null>(null);

  const { data, isLoading, refetch } = useQuery({
    queryKey: ["documents"],
    queryFn: getDocuments,
    retry: false
  });

  const uploadMutation = useMutation({
    mutationFn: (file: File) => uploadDocument(file),
    onSuccess: (result) => {
      message.success(result.data.duplicate ? "文件已存在，无需重复上传" : "文档上传成功");
      queryClient.invalidateQueries({ queryKey: ["documents"] });
    },
    onError: (error: Error) => {
      message.error(`上传失败: ${error.message}`);
    }
  });

  const indexMutation = useMutation({
    mutationFn: (documentId: number) => triggerDocumentIndex(documentId),
    onSuccess: () => {
      message.success("Index 任务已触发");
      queryClient.invalidateQueries({ queryKey: ["documents"] });
    },
    onError: (error: Error) => {
      message.error(`触发 Index 失败: ${error.message}`);
    },
    onSettled: () => {
      setConfirmLoading(null);
    }
  });

  const uploadProps: UploadProps = {
    name: "file",
    showUploadList: false,
    beforeUpload: (file) => {
      uploadMutation.mutate(file);
      return false;
    },
    accept: ".txt,.md,.markdown,.epub"
  };

  const handleIndex = (documentId: number) => {
    Modal.confirm({
      title: "确认触发 Index",
      content: "确定要对此文档进行解析、切分和向量化处理吗？",
      okText: "确定",
      cancelText: "取消",
      onOk: () => {
        setConfirmLoading(documentId);
        indexMutation.mutate(documentId);
      }
    });
  };

  const canIndex = (status: string) => {
    return ["UPLOADED", "PARSED", "CHUNKED", "FAILED"].includes(status);
  };

  const columns = [
    { title: "标题", dataIndex: "title", key: "title" },
    { title: "文件名", dataIndex: "fileName", key: "fileName" },
    { 
      title: "类型", 
      dataIndex: "fileType", 
      key: "fileType",
      render: (type: string) => (
        <Tag>{type.toUpperCase()}</Tag>
      )
    },
    { 
      title: "状态", 
      dataIndex: "status", 
      key: "status",
      render: (status: string) => <StatusTag status={status} />
    },
    {
      title: "操作",
      key: "actions",
      render: (_: any, record: DocumentSummary) => (
        <Space size="small">
          <Button 
            size="small" 
            icon={<Eye size={14} />}
            onClick={() => navigate(`/documents/${record.documentId}`)}
          >
            查看
          </Button>
          <Button 
            size="small" 
            type="primary"
            ghost
            icon={<PlayCircle size={14} />}
            disabled={!canIndex(record.status) || confirmLoading === record.documentId}
            loading={confirmLoading === record.documentId}
            onClick={() => handleIndex(record.documentId)}
          >
            Index
          </Button>
        </Space>
      )
    }
  ];

  return (
    <section className="page">
      <div className="page-heading">
        <div>
          <h1>文档</h1>
          <p>上传电子书、观察处理状态，在这里触发 index 流程。</p>
        </div>
        <Upload {...uploadProps}>
          <Button 
            type="primary" 
            icon={<Inbox size={16} />}
            loading={uploadMutation.isPending}
          >
            {uploadMutation.isPending ? "上传中..." : "上传文档"}
          </Button>
        </Upload>
      </div>
      <Card>
        <Table
          loading={isLoading}
          rowKey="documentId"
          dataSource={data?.data ?? []}
          locale={{ emptyText: <Empty description="暂无文档，点击上方按钮上传吧！" /> }}
          columns={columns}
        />
      </Card>
    </section>
  );
}

