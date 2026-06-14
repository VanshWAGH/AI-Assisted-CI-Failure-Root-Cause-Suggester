import {
  ResponsiveContainer,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  Area,
  AreaChart,
} from 'recharts';
import { motion } from 'framer-motion';
import type { TrendDay } from '../types';

interface Props {
  data: TrendDay[];
  days: number;
  onDaysChange: (days: number) => void;
}

const LINES = [
  { key: 'infra',    color: '#f97316', label: 'Infrastructure' },
  { key: 'test',     color: '#a855f7', label: 'Test' },
  { key: 'build',    color: '#3b82f6', label: 'Build' },
  { key: 'security', color: '#ef4444', label: 'Security' },
];

// Custom tooltip
function CustomTooltip({ active, payload, label }: any) {
  if (!active || !payload?.length) return null;
  return (
    <div className="chart-tooltip">
      <div style={{ fontWeight: 700, marginBottom: 8, color: 'var(--text-secondary)', fontSize: '0.75rem' }}>
        {label}
      </div>
      {payload.map((entry: any) => (
        <div key={entry.dataKey} style={{ display: 'flex', justifyContent: 'space-between', gap: 16, marginBottom: 4 }}>
          <span style={{ color: entry.color, fontWeight: 600, fontSize: '0.8rem' }}>{entry.name}</span>
          <span style={{ fontFamily: 'var(--font-mono)', fontWeight: 700, fontSize: '0.85rem' }}>{entry.value}</span>
        </div>
      ))}
    </div>
  );
}

// Custom X-axis tick: show only MM/DD
function CustomXTick({ x, y, payload }: any) {
  const date = payload.value ? payload.value.slice(5) : '';
  return (
    <text x={x} y={y + 14} textAnchor="middle" fill="var(--text-muted)" fontSize={11} fontFamily="var(--font-mono)">
      {date}
    </text>
  );
}

export default function TrendChart({ data, days, onDaysChange }: Props) {
  return (
    <motion.div
      className="card"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay: 0.2 }}
    >
      <div className="card-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--sp-3)' }}>
          <div className="section-title-icon">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
          </div>
          <span className="card-title" style={{ fontSize: '0.9rem', textTransform: 'none', fontWeight: 700, color: 'var(--text-primary)' }}>
            Failure Trend
          </span>
        </div>
        <div style={{ display: 'flex', gap: 'var(--sp-1)' }}>
          {[7, 14, 30].map((d) => (
            <button
              key={d}
              className={days === d ? 'btn btn-primary' : 'btn btn-ghost'}
              style={{ padding: '4px 12px', fontSize: '0.78rem' }}
              onClick={() => onDaysChange(d)}
            >
              {d}d
            </button>
          ))}
        </div>
      </div>

      <ResponsiveContainer width="100%" height={260}>
        <AreaChart data={data} margin={{ top: 4, right: 4, left: -16, bottom: 0 }}>
          <defs>
            {LINES.map((line) => (
              <linearGradient key={line.key} id={`grad-${line.key}`} x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%"  stopColor={line.color} stopOpacity={0.2} />
                <stop offset="95%" stopColor={line.color} stopOpacity={0.01} />
              </linearGradient>
            ))}
          </defs>

          <CartesianGrid
            strokeDasharray="3 3"
            stroke="rgba(99,102,241,0.08)"
            vertical={false}
          />

          <XAxis
            dataKey="date"
            tick={<CustomXTick />}
            axisLine={false}
            tickLine={false}
            interval="preserveStartEnd"
          />
          <YAxis
            tick={{ fill: 'var(--text-muted)', fontSize: 11, fontFamily: 'var(--font-mono)' }}
            axisLine={false}
            tickLine={false}
            allowDecimals={false}
          />

          <Tooltip content={<CustomTooltip />} />

          <Legend
            wrapperStyle={{ paddingTop: 12, fontSize: '0.78rem' }}
            formatter={(value) => (
              <span style={{ color: 'var(--text-secondary)', fontWeight: 600 }}>{value}</span>
            )}
          />

          {LINES.map((line) => (
            <Area
              key={line.key}
              type="monotone"
              dataKey={line.key}
              name={line.label}
              stroke={line.color}
              strokeWidth={2}
              fill={`url(#grad-${line.key})`}
              dot={{ fill: line.color, r: 3, strokeWidth: 0 }}
              activeDot={{ r: 5, strokeWidth: 0 }}
            />
          ))}
        </AreaChart>
      </ResponsiveContainer>
    </motion.div>
  );
}
