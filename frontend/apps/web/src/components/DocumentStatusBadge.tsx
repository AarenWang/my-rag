import { Badge, Tag, Tooltip } from "antd";
import { CheckCircle, Clock, AlertCircle, FileText, Loader2 } from "lucide-react";

interface DocumentStatusBadgeProps {
  status: string;
  showIcon?: boolean;
}

const STATUS_CONFIG: Record<
  string,
  { color: string; icon: React.ReactNode; label: string }
> = {
  UPLOADED: {
    color: "blue",
    icon: <FileText size={14} />,
    label: "已上传",
  },
  PARSING: {
    color: "cyan",
    icon: <Loader2 size={14} className="animate-spin" />,
    label: "解析中",
  },
  PARSED: {
    color: "purple",
    icon: <FileText size={14} />,
    label: "已解析",
  },
  CHUNKING: {
    color: "orange",
    icon: <Loader2 size={14} className="animate-spin" />,
    label: "切分中",
  },
  CHUNKED: {
    color: "indigo",
    icon: <FileText size={14} />,
    label: "已切分",
  },
  EMBEDDING: {
    color: "gold",
    icon: <Loader2 size={14} className="animate-spin" />,
    label: "向量化中",
  },
  READY: {
    color: "green",
    icon: <CheckCircle size={14} />,
    label: "就绪",
  },
  FAILED: {
    color: "red",
    icon: <AlertCircle size={14} />,
    label: "失败",
  },
  NOT_FOUND: {
    color: "default",
    icon: <AlertCircle size={14} />,
    label: "未找到",
  },
};

export default function DocumentStatusBadge({
  status,
  showIcon = true,
}: DocumentStatusBadgeProps) {
  const config = STATUS_CONFIG[status] || {
    color: "default",
    icon: <Clock size={14} />,
    label: status,
  };

  const content = (
    <Tag color={config.color} icon={showIcon ? config.icon : undefined}>
      {config.label}
    </Tag>
  );

  if (status === "FAILED") {
    return (
      <Tooltip title="点击查看详情或重新索引">
        {content}
      </Tooltip>
    );
  }

  return content;
}
