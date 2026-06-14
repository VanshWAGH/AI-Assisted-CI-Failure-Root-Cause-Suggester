import { useState } from 'react';
import { motion } from 'framer-motion';
import { RefreshCw, AlertTriangle, HelpCircle, Activity, BarChart3, Clock } from 'lucide-react';
import { useDashboardStats, useTrendData, useRecentAnalyses } from '../hooks/useDashboardData';
import SummaryCards from '../components/SummaryCards';
import TrendChart from '../components/TrendChart';
import FailurePieChart from '../components/FailurePieChart';
import RecentAnalyses from '../components/RecentAnalyses';

/* ─── Subtle animated gradient mesh (shared aesthetic with AnalyzePage) ─── */
const MeshBackground = () => (
  <div
    aria-hidden
    style={{
      position: 'fixed', inset: 0, zIndex: 0, pointerEvents: 'none', overflow: 'hidden',
    }}
  >
    <div style={{
      position: 'absolute', top: '-15%', right: '-8%', width: '50%', height: '50%',
      background: 'radial-gradient(ellipse, rgba(99,102,241,0.07) 0%, transparent 70%)',
      animation: 'dmesh1 20s ease-in-out infinite alternate',
    }} />
    <div style={{
      position: 'absolute', bottom: '-12%', left: '-6%', width: '42%', height: '42%',
      background: 'radial-gradient(ellipse, rgba(20,184,166,0.05) 0%, transparent 70%)',
      animation: 'dmesh2 25s ease-in-out infinite alternate',
    }} />
    <div style={{
      position: 'absolute', top: '35%', left: '45%', width: '30%', height: '30%',
      background: 'radial-gradient(ellipse, rgba(167,139,250,0.04) 0%, transparent 70%)',
      animation: 'dmesh1 18s ease-in-out infinite alternate-reverse',
    }} />
    <style>{`
      @keyframes dmesh1 { from { transform: translate(0,0) scale(1); } to { transform: translate(25px,18px) scale(1.05); } }
      @keyframes dmesh2 { from { transform: translate(0,0) scale(1); } to { transform: translate(-20px,-12px) scale(1.04); } }
    `}</style>
  </div>
);

/* ─── Skeleton loader card ─── */
const SkeletonCard = ({ height = 112 }: { height?: number }) => (
  <div style={{
    height, borderRadius: 16,
    background: 'rgba(15,23,42,0.4)',
    border: '1px solid rgba(148,163,184,0.07)',
    overflow: 'hidden', position: 'relative',
  }}>
    <div style={{
      position: 'absolute', inset: 0,
      background: 'linear-gradient(90deg, transparent 0%, rgba(148,163,184,0.05) 50%, transparent 100%)',
      animation: 'shimmer 1.8s ease-in-out infinite',
      backgroundSize: '200% 100%',
    }} />
    <style>{`@keyframes shimmer { 0%{background-position:-200% 0} 100%{background-position:200% 0} }`}</style>
  </div>
);

/* ─── Loading spinner (small) ─── */
const Spinner = () => (
  <div style={{
    width: 28, height: 28, borderRadius: '50%',
    border: '2px solid rgba(99,102,241,0.12)',
    borderTopColor: '#818cf8',
    animation: 'spin 0.85s linear infinite',
  }}>
    <style>{`@keyframes spin{to{transform:rotate(360deg)}}`}</style>
  </div>
);

export default function DashboardPage() {
  const [days, setDays] = useState(7);
  const { data: stats, loading: statsLoading, error: statsError, refetch: refetchStats } = useDashboardStats();
  const { data: trend, loading: trendLoading, error: trendError, refetch: refetchTrend } = useTrendData(days);
  const { data: recent, loading: recentLoading, error: recentError, refetch: refetchRecent } = useRecentAnalyses(10);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const handleRefresh = async () => {
    setIsRefreshing(true);
    try {
      await Promise.all([refetchStats?.(), refetchTrend?.(), refetchRecent?.()]);
    } catch (e) {
      console.error('Failed to refresh dashboard data', e);
    } finally {
      setIsRefreshing(false);
    }
  };

  const hasError = statsError || trendError || recentError;
  const isLoading = statsLoading || trendLoading || recentLoading;

  /* ── shared card style ── */
  const card: React.CSSProperties = {
    background: 'rgba(15,23,42,0.55)',
    border: '1px solid rgba(148,163,184,0.1)',
    borderRadius: 20,
    backdropFilter: 'blur(16px)',
    WebkitBackdropFilter: 'blur(16px)',
    position: 'relative',
    overflow: 'hidden',
  };

  return (
    <div style={{ position: 'relative', maxWidth: 1100, margin: '0 auto', paddingBottom: 64 }}>
      <MeshBackground />

      <motion.div
        initial={{ opacity: 0, y: 15 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: -15 }}
        transition={{ duration: 0.4 }}
        style={{ display: 'flex', flexDirection: 'column', gap: 22, position: 'relative', zIndex: 1 }}
      >

        {/* ── HEADER ── */}
        <div style={{ ...card }}>
          {/* Gradient accent line */}
          <div style={{
            position: 'absolute', top: 0, left: 28, right: 28, height: 2,
            background: 'linear-gradient(90deg, transparent, rgba(99,102,241,0.65), rgba(167,139,250,0.4), rgba(20,184,166,0.45), transparent)',
            borderRadius: 2,
          }} />

          <div style={{
            display: 'flex', flexWrap: 'wrap', alignItems: 'center',
            justifyContent: 'space-between', gap: 16, padding: '26px 30px',
          }}>
            {/* Title group */}
            <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
              <div style={{
                width: 44, height: 44, borderRadius: 13, flexShrink: 0,
                background: 'linear-gradient(135deg, rgba(99,102,241,0.22), rgba(167,139,250,0.18))',
                border: '1px solid rgba(99,102,241,0.3)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>
                <Activity size={18} color="#818cf8" />
              </div>
              <div>
                <h1 style={{
                  margin: 0, fontSize: 24, fontWeight: 800, letterSpacing: '-0.03em',
                  background: 'linear-gradient(120deg, #c7d2fe 0%, #a78bfa 40%, #34d399 100%)',
                  WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
                }}>
                  Operations Console
                </h1>
                <p style={{ margin: '3px 0 0', fontSize: 12, color: 'rgba(148,163,184,0.6)', lineHeight: 1.4 }}>
                  Real-time AI-assisted classification and root-cause analysis for CI/CD failures.
                </p>
              </div>
            </div>

            {/* Actions */}
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              {/* Live status dot */}
              <div style={{
                display: 'flex', alignItems: 'center', gap: 7,
                padding: '7px 14px', borderRadius: 10,
                background: 'rgba(52,211,153,0.07)',
                border: '1px solid rgba(52,211,153,0.15)',
              }}>
                <span style={{
                  width: 7, height: 7, borderRadius: '50%',
                  background: '#34d399',
                  boxShadow: '0 0 8px #34d399',
                  animation: 'pulse 2s ease-in-out infinite',
                }} />
                <span style={{ fontSize: 11, fontWeight: 700, color: 'rgba(52,211,153,0.85)', letterSpacing: '0.04em' }}>
                  LIVE
                </span>
                <style>{`@keyframes pulse{0%,100%{opacity:.6}50%{opacity:1}}`}</style>
              </div>

              {/* Refresh button */}
              <motion.button
                type="button"
                onClick={handleRefresh}
                disabled={isRefreshing || isLoading}
                whileHover={{ scale: 1.03 }}
                whileTap={{ scale: 0.96 }}
                style={{
                  display: 'flex', alignItems: 'center', gap: 7,
                  padding: '8px 16px', borderRadius: 10, cursor: 'pointer',
                  background: 'rgba(148,163,184,0.06)',
                  border: '1px solid rgba(148,163,184,0.13)',
                  color: isRefreshing ? '#818cf8' : '#94a3b8',
                  fontSize: 12, fontWeight: 600,
                  transition: 'all 0.2s',
                  opacity: (isRefreshing || isLoading) ? 0.55 : 1,
                }}
              >
                <RefreshCw
                  size={13}
                  style={{ animation: isRefreshing ? 'spin 0.85s linear infinite' : 'none' }}
                />
                {isRefreshing ? 'Refreshing…' : 'Refresh'}
              </motion.button>
            </div>
          </div>

          {/* Subtle grid texture */}
          <div style={{
            position: 'absolute', inset: 0, opacity: 0.018, pointerEvents: 'none',
            backgroundImage: 'repeating-linear-gradient(rgba(148,163,184,1) 0 1px, transparent 1px 40px), repeating-linear-gradient(90deg, rgba(148,163,184,1) 0 1px, transparent 1px 40px)',
          }} />
        </div>

        {/* ── ERROR BANNER ── */}
        {hasError && (
          <motion.div
            initial={{ opacity: 0, y: -8 }}
            animate={{ opacity: 1, y: 0 }}
            style={{
              display: 'flex', alignItems: 'flex-start', gap: 12,
              padding: '14px 18px', borderRadius: 14,
              background: 'rgba(127,29,29,0.25)',
              border: '1px solid rgba(248,113,113,0.25)',
              backdropFilter: 'blur(12px)',
            }}
          >
            <div style={{
              width: 28, height: 28, borderRadius: 8, flexShrink: 0,
              background: 'rgba(248,113,113,0.12)',
              border: '1px solid rgba(248,113,113,0.2)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              <AlertTriangle size={13} color="#f87171" />
            </div>
            <div>
              <p style={{ margin: '0 0 2px', fontSize: 12, fontWeight: 700, color: '#fca5a5' }}>
                Dashboard data warning
              </p>
              <p style={{ margin: 0, fontSize: 12, color: 'rgba(252,165,165,0.7)', lineHeight: 1.5 }}>
                {statsError || trendError || recentError || 'Failed to sync with API. Check backend status.'}
              </p>
            </div>
          </motion.div>
        )}

        {/* ── SUMMARY CARDS ── */}
        {stats ? (
          <SummaryCards stats={stats} />
        ) : statsLoading ? (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(6, 1fr)', gap: 14 }}>
            {[...Array(6)].map((_, i) => (
              <SkeletonCard key={i} height={108} />
            ))}
          </div>
        ) : (
          <div style={{
            ...card, padding: '48px 24px',
            display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 10,
          }}>
            <div style={{
              width: 48, height: 48, borderRadius: 14,
              background: 'rgba(148,163,184,0.06)',
              border: '1px solid rgba(148,163,184,0.1)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              <HelpCircle size={20} color="rgba(148,163,184,0.4)" />
            </div>
            <p style={{ margin: 0, fontSize: 13, color: 'rgba(148,163,184,0.5)' }}>
              No dashboard metrics available.
            </p>
          </div>
        )}

        {/* ── CHARTS GRID ── */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 320px', gap: 20 }}>

          {/* Trend Chart */}
          {trendLoading ? (
            <div style={{
              ...card, minHeight: 380,
              display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 14,
            }}>
              <Spinner />
              <div style={{ textAlign: 'center' }}>
                <p style={{ margin: '0 0 4px', fontSize: 13, fontWeight: 600, color: 'rgba(148,163,184,0.6)' }}>
                  Loading trend data…
                </p>
                <p style={{ margin: 0, fontSize: 11, color: 'rgba(148,163,184,0.3)' }}>
                  Fetching time-series metrics
                </p>
              </div>
            </div>
          ) : (
            <div style={{ ...card }}>
              {/* Card header */}
              <div style={{
                display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                padding: '18px 22px 0',
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <div style={{
                    width: 26, height: 26, borderRadius: 7,
                    background: 'rgba(99,102,241,0.1)',
                    border: '1px solid rgba(99,102,241,0.18)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                  }}>
                    <BarChart3 size={13} color="#818cf8" />
                  </div>
                  <span style={{ fontSize: 13, fontWeight: 700, color: '#e2e8f0' }}>
                    Failure Trend
                  </span>
                </div>
                {/* Day selector pills */}
                <div style={{ display: 'flex', gap: 5 }}>
                  {[7, 14, 30].map(d => (
                    <button
                      key={d}
                      type="button"
                      onClick={() => setDays(d)}
                      style={{
                        padding: '4px 11px', borderRadius: 8, cursor: 'pointer',
                        fontSize: 11, fontWeight: 700,
                        background: days === d ? 'rgba(99,102,241,0.15)' : 'rgba(148,163,184,0.05)',
                        border: `1px solid ${days === d ? 'rgba(99,102,241,0.3)' : 'rgba(148,163,184,0.1)'}`,
                        color: days === d ? '#a5b4fc' : 'rgba(148,163,184,0.45)',
                        transition: 'all 0.18s',
                      }}
                    >
                      {d}d
                    </button>
                  ))}
                </div>
              </div>
              <div style={{ padding: '8px 6px 6px' }}>
                <TrendChart days={days} onDaysChange={setDays} data={trend} />
              </div>
            </div>
          )}

          {/* Pie Chart */}
          {stats ? (
            <div style={{ ...card }}>
              <div style={{
                display: 'flex', alignItems: 'center', gap: 8,
                padding: '18px 22px 0',
              }}>
                <div style={{
                  width: 26, height: 26, borderRadius: 7,
                  background: 'rgba(167,139,250,0.1)',
                  border: '1px solid rgba(167,139,250,0.18)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                }}>
                  <Activity size={13} color="#a78bfa" />
                </div>
                <span style={{ fontSize: 13, fontWeight: 700, color: '#e2e8f0' }}>
                  Failure Distribution
                </span>
              </div>
              <div style={{ padding: '8px 6px 6px' }}>
                <FailurePieChart stats={stats} />
              </div>
            </div>
          ) : (
            <div style={{
              ...card, minHeight: 380,
              display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 12,
            }}>
              <Spinner />
              <p style={{ margin: 0, fontSize: 12, color: 'rgba(148,163,184,0.4)' }}>
                Waiting for metrics…
              </p>
            </div>
          )}
        </div>

        {/* ── RECENT ANALYSES ── */}
        <div style={{ ...card, padding: '24px 26px' }}>
          {/* Section header */}
          <div style={{
            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
            marginBottom: 20, paddingBottom: 16,
            borderBottom: '1px solid rgba(148,163,184,0.08)',
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 9 }}>
              <div style={{
                width: 28, height: 28, borderRadius: 8,
                background: 'rgba(20,184,166,0.1)',
                border: '1px solid rgba(20,184,166,0.18)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>
                <Clock size={13} color="#34d399" />
              </div>
              <div>
                <span style={{ fontSize: 13, fontWeight: 700, color: '#e2e8f0' }}>
                  Recent Analyses
                </span>
                <span style={{
                  marginLeft: 10, fontSize: 10, fontWeight: 700,
                  padding: '2px 8px', borderRadius: 20,
                  background: 'rgba(20,184,166,0.1)',
                  border: '1px solid rgba(20,184,166,0.18)',
                  color: 'rgba(52,211,153,0.7)',
                  letterSpacing: '0.04em',
                }}>
                  LAST 10
                </span>
              </div>
            </div>

            {/* Subtle status */}
            <span style={{ fontSize: 11, color: 'rgba(148,163,184,0.35)', fontFamily: 'ui-monospace, monospace' }}>
              {new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} sync
            </span>
          </div>

          {recentLoading ? (
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 14, padding: '32px 0' }}>
              <Spinner />
              <p style={{ margin: 0, fontSize: 12, color: 'rgba(148,163,184,0.4)', fontWeight: 500 }}>
                Loading recent analyses…
              </p>
            </div>
          ) : (
            <RecentAnalyses analyses={recent} />
          )}
        </div>

      </motion.div>
    </div>
  );
}