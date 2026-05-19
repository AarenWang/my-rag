import { useState } from "react";
import {
  archiveCollection,
  createCollection,
  getCollections,
  updateCollection
} from "@my-rag/api";
import type { CollectionDetail, CollectionSummary, CreateCollectionRequest, UpdateCollectionRequest } from "@my-rag/types";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Button,
  Card,
  Descriptions,
  Empty,
  Form,
  Input,
  Modal,
  Space,
  Table,
  Tag,
  Typography,
  message
} from "antd";
import { Archive, Edit, Folder, Plus } from "lucide-react";
import type { ColumnsType } from "antd/es/table";

const { TextArea } = Input;
const { Text } = Typography;

export default function Collections() {
  const queryClient = useQueryClient();
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [editingCollection, setEditingCollection] = useState<CollectionDetail | null>(null);
  const [form] = Form.useForm();

  const { data, isLoading } = useQuery({
    queryKey: ["collections"],
    queryFn: () => getCollections(false),
    retry: false
  });

  const createMutation = useMutation({
    mutationFn: (data: CreateCollectionRequest) => createCollection(data),
    onSuccess: () => {
      message.success("Collection created");
      queryClient.invalidateQueries({ queryKey: ["collections"] });
      handleModalClose();
    },
    onError: (error: Error) => {
      message.error(`Failed to create collection: ${error.message}`);
    }
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateCollectionRequest }) =>
      updateCollection(id, data),
    onSuccess: () => {
      message.success("Collection updated");
      queryClient.invalidateQueries({ queryKey: ["collections"] });
      handleModalClose();
    },
    onError: (error: Error) => {
      message.error(`Failed to update collection: ${error.message}`);
    }
  });

  const archiveMutation = useMutation({
    mutationFn: (collectionId: number) => archiveCollection(collectionId),
    onSuccess: () => {
      message.success("Collection archived");
      queryClient.invalidateQueries({ queryKey: ["collections"] });
    },
    onError: (error: Error) => {
      message.error(`Failed to archive collection: ${error.message}`);
    }
  });

  const handleCreate = () => {
    setEditingCollection(null);
    form.resetFields();
    setEditModalOpen(true);
  };

  const handleEdit = (collection: CollectionSummary) => {
    getCollections(true).then((response) => {
      const detail = response.data.find((c) => c.collectionId === collection.collectionId);
      if (detail) {
        setEditingCollection(detail as CollectionDetail);
        form.setFieldsValue({
          name: detail.name,
          description: detail.description || "",
          tags: detail.tags || ""
        });
        setEditModalOpen(true);
      }
    });
  };

  const handleArchive = (collectionId: number) => {
    Modal.confirm({
      title: "Confirm archive",
      content: "Archived collections will be hidden from the default list but can still be accessed. Are you sure?",
      okText: "Archive",
      okButtonProps: { danger: true },
      cancelText: "Cancel",
      onOk: () => archiveMutation.mutate(collectionId)
    });
  };

  const handleModalClose = () => {
    setEditModalOpen(false);
    setEditingCollection(null);
    form.resetFields();
  };

  const handleModalSubmit = () => {
    form.validateFields().then((values) => {
      if (editingCollection) {
        const data: UpdateCollectionRequest = {
          name: values.name,
          description: values.description || undefined,
          tags: values.tags || undefined
        };
        updateMutation.mutate({ id: editingCollection.collectionId, data });
      } else {
        const data: CreateCollectionRequest = {
          name: values.name,
          description: values.description || undefined,
          tags: values.tags || undefined
        };
        createMutation.mutate(data);
      }
    });
  };

  const columns: ColumnsType<CollectionSummary> = [
    { title: "Name", dataIndex: "name", key: "name" },
    { title: "Description", dataIndex: "description", key: "description", render: (desc) => desc || "-" },
    {
      title: "Documents",
      dataIndex: "readyDocumentCount",
      key: "readyDocumentCount",
      render: (count, record) => `${count} / ${record.documentCount}`
    },
    { title: "Chunks", dataIndex: "chunkCount", key: "chunkCount" },
    {
      title: "Actions",
      key: "actions",
      render: (_: unknown, record: CollectionSummary) => (
        <Space size="small">
          <Button
            size="small"
            icon={<Edit size={14} />}
            onClick={() => handleEdit(record)}
          >
            Edit
          </Button>
          <Button
            size="small"
            danger
            icon={<Archive size={14} />}
            onClick={() => handleArchive(record.collectionId)}
          >
            Archive
          </Button>
        </Space>
      )
    }
  ];

  return (
    <section className="page">
      <div className="page-heading">
        <div>
          <h1>Collections</h1>
          <p>Organize your documents into knowledge bases for better retrieval and management.</p>
        </div>
        <Button type="primary" icon={<Plus size={16} />} onClick={handleCreate}>
          New collection
        </Button>
      </div>
      <Card>
        <Table
          loading={isLoading}
          rowKey="collectionId"
          dataSource={data?.data ?? []}
          locale={{ emptyText: <Empty description="No collections yet." /> }}
          columns={columns}
        />
      </Card>

      <Modal
        title={editingCollection ? "Edit Collection" : "New Collection"}
        open={editModalOpen}
        onOk={handleModalSubmit}
        onCancel={handleModalClose}
        confirmLoading={createMutation.isPending || updateMutation.isPending}
        width={560}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label="Name"
            name="name"
            rules={[{ required: true, message: "Please enter a collection name" }]}
          >
            <Input placeholder="e.g., Product Documentation" />
          </Form.Item>
          <Form.Item label="Description" name="description">
            <TextArea
              rows={3}
              placeholder="Optional description of this collection's purpose"
            />
          </Form.Item>
          <Form.Item label="Tags" name="tags">
            <Input placeholder="Optional comma-separated tags" />
          </Form.Item>
          {!editingCollection && (
            <div style={{ marginTop: 16, padding: 12, backgroundColor: "#f5f5f5", borderRadius: 4 }}>
              <Text type="secondary">
                <Folder size={14} style={{ marginRight: 8 }} />
                A default collection will be created automatically. You can also create additional
                collections to organize your documents by topic, project, or purpose.
              </Text>
            </div>
          )}
        </Form>
      </Modal>
    </section>
  );
}
