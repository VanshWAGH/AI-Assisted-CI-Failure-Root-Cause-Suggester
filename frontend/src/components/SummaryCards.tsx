import { motion } from 'framer-motion';
import { Server, TestTube, Hammer, Shield, TrendingUp, Target } from 'lucide-react';
import type { DashboardStats } from '../types';

interface Props {
  stats: DashboardStats;
}

interface CardConfig {
  key: keyof DashboardStats;
  label: string;
  icon: React.ReactNode;
  colorClass: string;
  colorHex: string;
  format?: (v: number) => string;
}

const CARDS: CardConfig[] = [
  { key: 'totalFailuresToday', label: 'Total Today',    icon: <TrendingUp size={18} />, colorClass: 'total',      colorHex: '#6366f1' },
  { key: 'infraFailures',      label: 'Infrastructure', icon: <Server size={18} />,     colorClass: 'infra',      colorHex: '#f97316' },
  { key: 'testFailures',       label: 'Test Failures',  icon: <TestTube size={18} />,   colorClass: 'test',       colorHex: '#a855f7' },
  { key: 'buildFailures',      label: 'Build Errors',   icon: <Hammer size={18} />,     colorClass: 'build',      colorHex: '#3b82f6' },
  { key: 'securityFailures',   label: 'Security',       icon: <Shield size={18} />,     colorClass: 'security',   colorHex: '#ef4444' },
  {
    key: 'avgConfidence',
    label: 'Avg. Confidence',
    icon: <Target size={18} />,
    colorClass: 'confidence',
    colorHex: '#22d3ee',
    format: (v) => `${(v * 100).toFixed(1)}%`,
  },
];

export default function SummaryCards({ stats }: Props) {
  return (
    <div className="grid-6" style={{ marginBottom: 'var(--space-6)' }}>
      {CARDS.map((card, idx) => {
        const rawValue = stats[card.key] as number;

        return (
          <motion.div
            key={card.key}
            className={`stat-card ${card.colorClass}`}
            initial={{ opacity: 0, y: 24 }}
            animate={{ opacity: 1, y: 0 }}
            whileHover={{ scale: 1.02, translateY: -4 }}
            transition={{ duration: 0.4, delay: idx * 0.06, ease: [0.4, 0, 0.2, 1] }}
          >
            {/* Subtle background glow orb */}
            <div style={{
              position: 'absolute',
              top: -20,
              right: -20,
              width: 80,
              height: 80,
              borderRadius: '50%',
              background: `radial-gradient(circle, ${card.colorHex}22, transparent 70%)`,
              pointerEvents: 'none',
            }} />

            <div className="stat-icon" style={{ background: `${card.colorHex}18`, color: card.colorHex }}>
              {card.icon}
            </div>

            <motion.div
              className="stat-value mono"
              style={{ color: card.colorHex }}
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ duration: 0.6, delay: idx * 0.06 + 0.2 }}
            >
              <CountUp target={rawValue} format={card.format} />
            </motion.div>

            <div className="stat-label">{card.label}</div>

            {/* Confidence bar for avgConfidence */}
            {card.key === 'avgConfidence' && (
              <div className="confidence-bar" style={{ marginTop: 'var(--space-2)' }}>
                <motion.div
                  className="confidence-bar-fill"
                  style={{ background: card.colorHex }}
                  initial={{ width: 0 }}
                  animate={{ width: `${rawValue * 100}%` }}
                  transition={{ duration: 0.8, delay: 0.4, ease: 'easeOut' }}
                />
              </div>
            )}
          </motion.div>
        );
      })}
    </div>
  );
}

// Animated count-up component
function CountUp({ target, format }: { target: number; format?: (v: number) => string }) {
  if (format) return <>{format(target)}</>;
  return <>{target}</>;
}
