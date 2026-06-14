import { motion } from 'framer-motion';
import { formatDistanceToNow } from 'date-fns';
import { ChevronDown, ChevronUp } from 'lucide-react';
import { useState } from 'react';
import type { RecentAnalysis, FailureType } from '../types';
import { FAILURE_COLORS, FAILURE_LABELS } from '../types';

interface Props {
  analyses: RecentAnalysis[];
}

export default function RecentAnalyses({ analyses }: Props) {
  const [expanded, setExpanded] = useState<string | null>(null);

  if (analyses.length === 0) {
    return (
      <div style={{ textAlign: 'center', padding: 'var(--sp-12)', color: 'var(--text-muted)' }}>
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1" opacity="0.4" style={{ margin: '0 auto var(--sp-4)' }}>
          <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <div style={{ fontWeight: 600 }}>No analyses yet</div>
        <div style={{ fontSize: '0.85rem', marginTop: 4 }}>Submit a log to see results here</div>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--sp-2)' }}>
      {analyses.map((a, idx) => {
        const isExpanded = expanded === a.analysisId;
        const color = FAILURE_COLORS[a.failureType as FailureType] ?? '#6b7280';
        const label = FAILURE_LABELS[a.failureType as FailureType] ?? a.failureType;

        return (
          <motion.div
            layout
            key={a.analysisId}
            initial={{ opacity: 0, x: -12 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.3, delay: idx * 0.04 }}
            style={{
              border: '1px solid var(--border)',
              borderLeft: `3px solid ${color}`,
              borderRadius: 'var(--r-md)',
              background: 'rgba(30, 33, 48, 0.6)',
              backdropFilter: 'blur(16px)',
              overflow: 'hidden',
              transition: 'border-color var(--t-fast)',
            }}
          >
            {/* Row summary */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 'var(--sp-4)',
                padding: 'var(--sp-3) var(--sp-4)',
                cursor: 'pointer',
              }}
              onClick={() => setExpanded(isExpanded ? null : a.analysisId)}
            >
              {/* Failure type badge */}
              <span
                className={`badge badge-${a.failureType}`}
                style={{ minWidth: 90, justifyContent: 'center', flexShrink: 0 }}
              >
                {label}
              </span>

              {/* Project + branch */}
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontWeight: 600, fontSize: '0.875rem', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {a.projectName}
                </div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  #{a.externalJobId}{a.branchName ? ` · ${a.branchName}` : ''}
                </div>
              </div>

              {/* Confidence */}
              <div style={{ textAlign: 'right', flexShrink: 0 }}>
                <div style={{ fontFamily: 'var(--font-mono)', fontWeight: 700, fontSize: '0.9rem', color }}>
                  {(a.confidence * 100).toFixed(0)}%
                </div>
                <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                  {a.classifierMode.replace('_', ' ')}
                </div>
              </div>

              {/* Time */}
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', flexShrink: 0, minWidth: 72, textAlign: 'right' }}>
                {formatDistanceToNow(new Date(a.analyzedAt), { addSuffix: true })}
              </div>

              <div style={{ color: 'var(--text-muted)', flexShrink: 0 }}>
                {isExpanded ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
              </div>
            </div>

            {/* Expanded detail */}
            {isExpanded && (
              <motion.div
                initial={{ opacity: 0, height: 0 }}
                animate={{ opacity: 1, height: 'auto' }}
                exit={{ opacity: 0, height: 0 }}
                transition={{ duration: 0.2 }}
                style={{
                  borderTop: '1px solid var(--border)',
                  padding: 'var(--sp-4)',
                  background: 'rgba(10, 15, 30, 0.4)',
                  display: 'flex',
                  gap: 'var(--sp-6)',
                  flexWrap: 'wrap',
                }}
              >
                <div style={{ flex: 1, minWidth: 240 }}>
                  <div style={{ fontSize: '0.7rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.06em', color: 'var(--text-muted)', marginBottom: 6 }}>
                    Explanation
                  </div>
                  <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', lineHeight: 1.6, margin: 0 }}>
                    {a.explanation ?? '—'}
                  </p>
                </div>
                <div style={{ flex: 1, minWidth: 240 }}>
                  <div style={{ fontSize: '0.7rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.06em', color: 'var(--text-muted)', marginBottom: 6 }}>
                    Suggested Action
                  </div>
                  <p style={{ fontSize: '0.85rem', color: 'var(--green)', lineHeight: 1.6, margin: 0 }}>
                    {a.suggestedAction ?? '—'}
                  </p>
                </div>
                {a.matchedPatternName && (
                  <div style={{ flexShrink: 0 }}>
                    <div style={{ fontSize: '0.7rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.06em', color: 'var(--text-muted)', marginBottom: 6 }}>
                      Pattern
                    </div>
                    <span className="badge badge-info" style={{ fontFamily: 'var(--font-mono)' }}>
                      {a.matchedPatternName}
                    </span>
                  </div>
                )}
              </motion.div>
            )}
          </motion.div>
        );
      })}
    </div>
  );
}
