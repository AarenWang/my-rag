import { BookOpen, LayoutDashboard, MessageSquareText, Settings } from "lucide-react";
import { NavLink, Outlet } from "react-router-dom";

const navItems = [
  { path: "/dashboard", label: "概览", icon: LayoutDashboard },
  { path: "/documents", label: "文档", icon: BookOpen },
  { path: "/chat", label: "问答", icon: MessageSquareText },
  { path: "/settings", label: "设置", icon: Settings }
];

export default function MainLayout() {
  return (
    <div className="app-shell min-h-screen">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">R</span>
          <div>
            <strong>My RAG</strong>
            <small>中文电子书知识库</small>
          </div>
        </div>
        <nav className="nav-list">
          {navItems.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink key={item.path} to={item.path} className="nav-link">
                <Icon size={18} />
                <span>{item.label}</span>
              </NavLink>
            );
          })}
        </nav>
      </aside>
      <main className="main-panel">
        <Outlet />
      </main>
    </div>
  );
}
