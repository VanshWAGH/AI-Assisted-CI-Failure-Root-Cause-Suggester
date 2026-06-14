import { NavLink } from 'react-router-dom';
import { Activity, BarChart3, Search, Cpu, Zap, Database, Settings } from 'lucide-react';
import { useServiceStatus } from '../hooks/useDashboardData';

export default function Navbar() {
  const online = useServiceStatus();

  return (
    <nav style={{
      background: 'rgba(10, 15, 30, 0.92)',
      borderBottom: '1px solid var(--color-border)',
      backdropFilter: 'blur(16px)',
      WebkitBackdropFilter: 'blur(16px)',
      position: 'sticky',
      top: 0,
      zIndex: 100,
    }}>
      <div style={{
        maxWidth: '1600px',
        margin: '0 auto',
        padding: '0 var(--space-8)',
        height: '60px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
      }}>
        {/* Brand */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)' }}>
          <div style={{
            width: 36,
            height: 36,
            borderRadius: 'var(--radius-md)',
            background: 'linear-gradient(135deg, #6366f1, #22d3ee)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 0 20px rgba(99,102,241,0.4)',
          }}>
            <Zap size={20} color="white" strokeWidth={2.5} />
          </div>
          <div>
            <div style={{ fontWeight: 800, fontSize: '0.95rem', letterSpacing: '-0.01em', lineHeight: 1 }}>
              RootCause<span style={{ color: 'var(--color-primary)' }}> AI</span>
            </div>
            <div style={{ fontSize: '0.65rem', color: 'var(--color-text-muted)', letterSpacing: '0.05em', textTransform: 'uppercase' }}>
              CI Failure Analyzer
            </div>
          </div>
        </div>

        {/* Navigation links */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-1)' }}>
          <NavItem to="/" icon={<BarChart3 size={15} />} label="Dashboard" />
          <NavItem to="/analyze" icon={<Search size={15} />} label="Analyze" />
          <NavItem to="/history" icon={<Activity size={15} />} label="History" />
          <NavItem to="/patterns" icon={<Database size={15} />} label="Patterns" />
          <NavItem to="/settings" icon={<Settings size={15} />} label="Settings" />
        </div>

        {/* Status indicator */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
            <div style={{
              width: 8,
              height: 8,
              borderRadius: '50%',
              background: online === null ? '#f59e0b' : online ? '#10b981' : '#ef4444',
              boxShadow: `0 0 8px ${online === null ? '#f59e0b' : online ? '#10b981' : '#ef4444'}`,
              ...(online !== false && { animation: 'pulse 2s infinite' }),
            }} />
            <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', fontWeight: 500 }}>
              {online === null ? 'Checking…' : online ? 'API Online' : 'API Offline'}
            </span>
          </div>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: 'var(--space-2)',
            padding: '4px 10px',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-full)',
            fontSize: '0.7rem',
            color: 'var(--color-text-muted)',
            fontFamily: 'var(--font-mono)',
          }}>
            <Cpu size={12} />
            v1.0.0
          </div>
        </div>
      </div>
    </nav>
  );
}

function NavItem({ to, icon, label }: { to: string; icon: React.ReactNode; label: string }) {
  return (
    <NavLink
      to={to}
      end={to === '/'}
      style={({ isActive }) => ({
        display: 'flex',
        alignItems: 'center',
        gap: 'var(--space-2)',
        padding: '6px 14px',
        borderRadius: 'var(--radius-md)',
        fontSize: '0.85rem',
        fontWeight: 600,
        textDecoration: 'none',
        transition: 'all var(--transition-fast)',
        color: isActive ? 'var(--color-primary)' : 'var(--color-text-muted)',
        background: isActive ? 'rgba(99,102,241,0.12)' : 'transparent',
        border: isActive ? '1px solid rgba(99,102,241,0.2)' : '1px solid transparent',
      })}
    >
      {icon}
      {label}
    </NavLink>
  );
}
