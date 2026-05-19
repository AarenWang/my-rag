import { Navigate, Route, Routes } from "react-router-dom";
import MainLayout from "./layouts/MainLayout";
import Chat from "./pages/Chat";
import ChatLogs from "./pages/ChatLogs";
import ChatLogDetail from "./pages/ChatLogDetail";
import Collections from "./pages/Collections";
import Dashboard from "./pages/Dashboard";
import DocumentDetail from "./pages/DocumentDetail";
import Documents from "./pages/Documents";
import RetrievalDebug from "./pages/RetrievalDebug";
import Settings from "./pages/Settings";

export default function App() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/collections" element={<Collections />} />
        <Route path="/documents" element={<Documents />} />
        <Route path="/documents/:documentId" element={<DocumentDetail />} />
        <Route path="/chat" element={<Chat />} />
        <Route path="/retrieval-debug" element={<RetrievalDebug />} />
        <Route path="/chat-logs" element={<ChatLogs />} />
        <Route path="/chat-logs/:id" element={<ChatLogDetail />} />
        <Route path="/settings" element={<Settings />} />
      </Route>
    </Routes>
  );
}
