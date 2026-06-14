import { HashRouter as Router, Routes, Route, Navigate, NavLink, useLocation } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  LayoutDashboard, Search, Clock, Database, Settings,
  Zap, GitBranch, Activity
} from 'lucide-react';
import DashboardPage from './pages/DashboardPage';
import AnalyzePage from './pages/AnalyzePage';
import HistoryPage from './pages/HistoryPage';
import PatternsPage from './pages/PatternsPage';
import SettingsPage from './pages/SettingsPage';
import { useServiceStatus } from './hooks/useDashboardData';
import './index.css';

const NAV = [
  { to: '/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/analyze',   icon: Search,          label: 'Analyze Log' },
  { to: '/history',   icon: Clock,           label: 'History' },
  { to: '/patterns',  icon: Database,        label: 'Patterns' },
];

const PAGE_META: Record<string, { title: string; subtitle: string }> = {
  '/dashboard': { title: 'Operations Console',   subtitle: 'Real-time CI/CD failure metrics and monitoring' },
  '/analyze':   { title: 'Log Analyzer',         subtitle: 'Run AI classification on raw pipeline logs' },
  '/history':   { title: 'Analysis History',     subtitle: 'Browse and audit past failure classification records' },
  '/patterns':  { title: 'Failure Patterns',     subtitle: 'Regex rule catalogue used by the heuristic classifier' },
  '/settings':  { title: 'Settings & Integrations', subtitle: 'Configure API connections and webhook integrations' },
};

function Sidebar({ online }: { online: boolean | null }) {
  return (
    <aside className="sidebar">
      {/* Brand */}
      <div className="sidebar-brand">
        <div className="sidebar-brand-icon">
          <Zap size={18} color="#fff" strokeWidth={2.5} />
        </div>
        <div>
          <div className="sidebar-brand-name">RootCause<span style={{ color: 'var(--accent)' }}>AI</span></div>
          <div className="sidebar-brand-sub">CI Failure Analyzer</div>
        </div>
      </div>

      {/* Nav */}
      <nav className="sidebar-nav">
        <div className="sidebar-section-label">Navigation</div>
        {NAV.map(({ to, icon: Icon, label }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) => `sidebar-item${isActive ? ' active' : ''}`}
          >
            <Icon size={16} className="sidebar-item-icon" />
            <span>{label}</span>
          </NavLink>
        ))}

        <div className="sidebar-section-label" style={{ marginTop: 12 }}>System</div>
        <NavLink
          to="/settings"
          className={({ isActive }) => `sidebar-item${isActive ? ' active' : ''}`}
        >
          <Settings size={16} className="sidebar-item-icon" />
          <span>Settings</span>
        </NavLink>
      </nav>

      {/* Footer status */}
      <div className="sidebar-footer">
        <div className="sidebar-status">
          <div className={`status-dot ${online === null ? 'pending' : online ? 'online' : 'offline'}`} />
          <span>{online === null ? 'Checking API…' : online ? 'API Online' : 'API Offline'}</span>
        </div>
        <div className="sidebar-status" style={{ opacity: 0.45 }}>
          <GitBranch size={11} />
          <span style={{ fontFamily: 'var(--font-mono)', fontSize: '0.68rem' }}>v1.0.0</span>
        </div>
      </div>
    </aside>
  );
}

function Topbar() {
  const { pathname } = useLocation();
  const meta = PAGE_META[pathname] ?? { title: 'RootCause AI', subtitle: '' };
  return (
    <header className="topbar">
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 10 }}>
        <span className="topbar-title">{meta.title}</span>
        {meta.subtitle && (
          <span className="topbar-subtitle">— {meta.subtitle}</span>
        )}
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <Activity size={13} style={{ color: 'var(--text-muted)' }} />
        <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
          {new Date().toLocaleTimeString()}
        </span>
      </div>
    </header>
  );
}

function AnimatedPage({ children }: { children: React.ReactNode }) {
  const { pathname } = useLocation();
  return (
    <AnimatePresence mode="wait">
      <motion.div
        key={pathname}
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: -8 }}
        transition={{ duration: 0.22 }}
        className="page-content"
      >
        {children}
      </motion.div>
    </AnimatePresence>
  );
}

export default function App() {
  const online = useServiceStatus();

  return (
    <Router>
      <div className="app-shell">
        <Sidebar online={online} />

        <div className="main-area">
          <Topbar />

          <Routes>
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<AnimatedPage><DashboardPage /></AnimatedPage>} />
            <Route path="/analyze"   element={<AnimatedPage><AnalyzePage /></AnimatedPage>} />
            <Route path="/history"   element={<AnimatedPage><HistoryPage /></AnimatedPage>} />
            <Route path="/patterns"  element={<AnimatedPage><PatternsPage /></AnimatedPage>} />
            <Route path="/settings"  element={<AnimatedPage><SettingsPage /></AnimatedPage>} />
            <Route path="*"          element={<Navigate to="/dashboard" replace />} />
          </Routes>

          <footer style={{
            padding: '12px 24px',
            borderTop: '1px solid var(--border)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            fontSize: '0.72rem',
            color: 'var(--text-muted)',
          }}>
            <span>RootCause AI Ops Console © {new Date().getFullYear()}</span>
            <span style={{ fontFamily: 'var(--font-mono)' }}>Advanced Agentic Coding</span>
          </footer>
        </div>
      </div>
    </Router>
  );
}
