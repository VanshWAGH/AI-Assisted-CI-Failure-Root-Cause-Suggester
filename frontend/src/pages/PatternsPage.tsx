import { useState, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Database, Search, Shield, Hammer, TestTube, HardDrive,
  HelpCircle, Eye, EyeOff, ArrowUpDown, RefreshCw, AlertTriangle,
  FileCode, Layers, Code, ChevronDown, Braces, BookOpen
} from 'lucide-react';
import { usePatterns, usePatternStats } from '../hooks/useDashboardData';
import { FAILURE_COLORS, FAILURE_LABELS } from '../types';
import type { FailureType } from '../types';

/* ─── Failure-type meta ─────────────────────────────────────────────────────── */

const FAILURE_META: Record<string, {
  icon: React.ReactNode;
  bar: string;
  pill: string;
  glow: string;
}> = {
  infra: { icon: <HardDrive className="w-3.5 h-3.5" />, bar: 'bg-amber-400', pill: 'bg-amber-400/10   text-amber-300   ring-amber-400/20', glow: '#f59e0b' },
  test: { icon: <TestTube className="w-3.5 h-3.5" />, bar: 'bg-violet-400', pill: 'bg-violet-400/10  text-violet-300  ring-violet-400/20', glow: '#a78bfa' },
  build: { icon: <Hammer className="w-3.5 h-3.5" />, bar: 'bg-sky-400', pill: 'bg-sky-400/10     text-sky-300     ring-sky-400/20', glow: '#38bdf8' },
  security: { icon: <Shield className="w-3.5 h-3.5" />, bar: 'bg-rose-400', pill: 'bg-rose-400/10    text-rose-300    ring-rose-400/20', glow: '#fb7185' },
  unknown: { icon: <HelpCircle className="w-3.5 h-3.5" />, bar: 'bg-slate-500', pill: 'bg-slate-500/10   text-slate-400   ring-slate-500/20', glow: '#64748b' },
};

const getMeta = (type: string) => FAILURE_META[type] ?? FAILURE_META.unknown;

/* ─── Stat card ─────────────────────────────────────────────────────────────── */

function StatCard({
  value, label, accent, mono = false,
}: { value: number | string; label: string; accent: string; mono?: boolean }) {
  return (
    <div className={`relative overflow-hidden rounded-xl border border-slate-800/70
                     bg-slate-900/50 backdrop-blur-sm p-4 flex flex-col gap-1`}>
      <div className={`absolute top-0 inset-x-0 h-0.5 ${accent}`} />
      <span className={`text-2xl font-black tabular-nums text-slate-100 ${mono ? 'font-mono' : ''}`}>
        {value}
      </span>
      <span className="text-[10px] font-bold uppercase tracking-widest text-slate-500">{label}</span>
    </div>
  );
}

/* ─── Pattern card ──────────────────────────────────────────────────────────── */

function PatternCard({
  pattern,
  isExpanded,
  onToggle,
}: {
  pattern: any;
  isExpanded: boolean;
  onToggle: () => void;
}) {
  const meta = getMeta(pattern.failureType);
  const label = FAILURE_LABELS[pattern.failureType as FailureType] || pattern.failureType;
  const glow = FAILURE_COLORS[pattern.failureType as FailureType] || meta.glow;

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, scale: 0.97 }}
      transition={{ duration: 0.22 }}
      className={`relative rounded-2xl border flex flex-col overflow-hidden
                  transition-all duration-200
                  ${isExpanded
          ? 'border-slate-700/80 md:col-span-2 bg-slate-900/70'
          : 'border-slate-800/70 hover:border-slate-700/60 bg-slate-900/50'}`}
    >
      {/* corner glow */}
      <div
        className="pointer-events-none absolute top-0 right-0 w-24 h-24 opacity-15"
        style={{ background: `radial-gradient(circle at top right, ${glow} 0%, transparent 70%)` }}
      />

      {/* ── Header ── */}
      <div className="p-5 pb-4 space-y-3 relative">
        <div className="flex items-center justify-between gap-3 flex-wrap">
          {/* type pill */}
          <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px]
                            font-semibold ring-1 ${meta.pill}`}>
            {meta.icon}
            {label}
          </span>

          <div className="flex items-center gap-2">
            {pattern.active ? (
              <span className="text-[10px] font-bold text-emerald-400 bg-emerald-950/30
                               ring-1 ring-emerald-900/50 px-2 py-0.5 rounded-md">
                Active
              </span>
            ) : (
              <span className="text-[10px] font-bold text-slate-500 bg-slate-800/60
                               ring-1 ring-slate-700/40 px-2 py-0.5 rounded-md">
                Inactive
              </span>
            )}
            <span className="text-[10px] font-mono text-slate-500 bg-slate-800/40
                             ring-1 ring-slate-700/30 px-2 py-0.5 rounded-md">
              P{pattern.priority}
            </span>
          </div>
        </div>

        <h3 className="text-sm font-bold text-slate-100 leading-snug tracking-tight">
          {pattern.name}
        </h3>

        {/* Regex display */}
        <div className="flex items-start gap-2 px-3 py-2.5 rounded-xl
                        bg-slate-950/60 border border-slate-800/70 overflow-hidden">
          <Braces className="w-3.5 h-3.5 text-indigo-400/70 shrink-0 mt-0.5" />
          <span className="font-mono text-[10.5px] text-indigo-300 break-all
                           whitespace-pre-wrap select-all leading-relaxed">
            {pattern.regexPattern}
          </span>
        </div>
      </div>

      {/* ── Expanded details ── */}
      <AnimatePresence initial={false}>
        {isExpanded && (
          <motion.div
            key="detail"
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            transition={{ duration: 0.28 }}
            className="overflow-hidden"
          >
            <div className="px-5 pb-5 pt-4 border-t border-slate-800/60 bg-slate-950/20 space-y-4">

              {/* Explanation */}
              <div className="space-y-1.5">
                <h4 className="flex items-center gap-1.5 text-[10px] font-bold uppercase
                               tracking-widest text-slate-500">
                  <Layers className="w-3 h-3 text-violet-400" />
                  Explanation Template
                </h4>
                <p className="text-xs text-slate-300 leading-relaxed whitespace-pre-line
                              p-3 bg-slate-900/40 border border-slate-800/50 rounded-xl">
                  {pattern.explanationTemplate}
                </p>
              </div>

              {/* Suggested action */}
              <div className="space-y-1.5">
                <h4 className="flex items-center gap-1.5 text-[10px] font-bold uppercase
                               tracking-widest text-slate-500">
                  <FileCode className="w-3 h-3 text-emerald-400" />
                  Suggested Action Template
                </h4>
                <div className="rounded-xl border border-slate-800/60 overflow-hidden">
                  <div className="flex items-center gap-1.5 px-3 py-2 bg-slate-800/30 border-b border-slate-800/50">
                    {['bg-rose-500/50', 'bg-amber-500/50', 'bg-emerald-500/50'].map((c) => (
                      <span key={c} className={`w-2 h-2 rounded-full ${c}`} />
                    ))}
                    <span className="ml-1 text-[9px] font-mono text-slate-600">template</span>
                  </div>
                  <pre className="font-mono text-[10.5px] text-slate-300 leading-relaxed
                                  p-3 bg-slate-950/50 whitespace-pre-wrap overflow-x-auto">
                    {pattern.suggestedActionTemplate}
                  </pre>
                </div>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* ── Footer ── */}
      <div className="mt-auto flex items-center justify-between px-5 py-3
                      border-t border-slate-800/50 bg-slate-950/10">
        <span className="text-[10px] text-slate-600">
          Added{' '}
          <span className="font-mono text-slate-500">
            {new Date(pattern.createdAt).toLocaleDateString()}
          </span>
        </span>
        <button
          onClick={onToggle}
          className="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg text-[11px] font-semibold
                     bg-indigo-500/10 hover:bg-indigo-500/20 text-indigo-300
                     ring-1 ring-indigo-500/20 hover:ring-indigo-500/40
                     active:scale-95 transition-all cursor-pointer"
        >
          {isExpanded
            ? <><EyeOff className="w-3.5 h-3.5" /> Hide Details</>
            : <><Eye className="w-3.5 h-3.5" /> Show Details</>
          }
        </button>
      </div>
    </motion.div>
  );
}

/* ─── Main page ─────────────────────────────────────────────────────────────── */

export default function PatternsPage() {
  const [activeTab, setActiveTab] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [sortBy, setSortBy] = useState<'priority' | 'name'>('priority');
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const { data: patterns, loading: patternsLoading, error: patternsError, refetch: refetchPatterns } = usePatterns();
  const { data: stats, loading: statsLoading, error: statsError, refetch: refetchStats } = usePatternStats();

  const handleRefresh = async () => {
    setIsRefreshing(true);
    try { await Promise.all([refetchPatterns?.(), refetchStats?.()]); }
    catch (e) { console.error('Failed to refresh patterns data', e); }
    finally { setIsRefreshing(false); }
  };

  const filteredPatterns = useMemo(() => {
    if (!patterns) return [];
    return patterns
      .filter((p) => {
        const matchesTab = activeTab === 'all' || p.failureType === activeTab;
        const q = searchQuery.toLowerCase();
        const matchesQuery = !q ||
          p.name.toLowerCase().includes(q) ||
          p.regexPattern.toLowerCase().includes(q) ||
          p.explanationTemplate.toLowerCase().includes(q);
        return matchesTab && matchesQuery;
      })
      .sort((a, b) =>
        sortBy === 'priority' ? b.priority - a.priority : a.name.localeCompare(b.name)
      );
  }, [patterns, activeTab, searchQuery, sortBy]);

  const tabs = [
    { value: 'all', label: 'All' },
    { value: 'infra', label: 'Infrastructure' },
    { value: 'test', label: 'Test' },
    { value: 'build', label: 'Build' },
    { value: 'security', label: 'Security' },
  ];

  const statCards = [
    { value: stats?.total ?? '—', label: 'Total Rules', accent: 'bg-indigo-500' },
    { value: stats?.active ?? '—', label: 'Active Matchers', accent: 'bg-emerald-500' },
    { value: stats?.infra ?? '—', label: 'Infrastructure', accent: 'bg-amber-400' },
    { value: stats?.test ?? '—', label: 'Test Failures', accent: 'bg-violet-400' },
    { value: stats?.build ?? '—', label: 'Build Errors', accent: 'bg-sky-400' },
    { value: stats?.security ?? '—', label: 'Security', accent: 'bg-rose-400' },
  ];

  /* ── render ── */
  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -12 }}
      transition={{ duration: 0.35 }}
      className="space-y-6 pb-16 max-w-6xl mx-auto px-4 sm:px-6 font-sans"
    >

      {/* ── Page header ─────────────────────────────────────────────────────── */}
      <div className="relative overflow-hidden rounded-2xl border border-slate-800/70
                      bg-slate-900/60 backdrop-blur-xl px-6 py-7 md:px-8">
        {/* blobs */}
        <div className="pointer-events-none absolute -top-16 -left-12 w-64 h-64
                        rounded-full bg-amber-600/8 blur-3xl" />
        <div className="pointer-events-none absolute -bottom-12 right-0 w-56 h-56
                        rounded-full bg-orange-600/8 blur-3xl" />

        <div className="relative flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-2">
              <span className="inline-flex items-center justify-center w-7 h-7 rounded-lg
                               bg-amber-500/15 ring-1 ring-amber-500/30">
                <BookOpen className="w-3.5 h-3.5 text-amber-400" />
              </span>
              <span className="text-[11px] font-semibold uppercase tracking-widest text-amber-400/70">
                Pattern Catalogue
              </span>
            </div>
            <h1 className="text-2xl md:text-3xl font-extrabold tracking-tight text-white">
              Failure Pattern Catalogue
            </h1>
            <p className="text-slate-400 text-sm mt-1 max-w-lg">
              Browse, audit, and manage regex templates that match failure signatures across your pipelines.
            </p>
          </div>

          <button
            onClick={handleRefresh}
            disabled={isRefreshing || patternsLoading || statsLoading}
            className="shrink-0 inline-flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-semibold
                       border border-slate-700/60 bg-slate-800/40 text-slate-300
                       hover:bg-slate-700/50 hover:text-slate-100
                       disabled:opacity-40 active:scale-95 transition cursor-pointer"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin text-amber-400' : ''}`} />
            Sync Catalogue
          </button>
        </div>
      </div>

      {/* ── Error banner ─────────────────────────────────────────────────────── */}
      <AnimatePresence>
        {(patternsError || statsError) && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            className="overflow-hidden"
          >
            <div className="flex items-start gap-3 p-4 bg-rose-950/30 border border-rose-500/25
                            rounded-xl text-rose-300 text-xs leading-relaxed">
              <AlertTriangle className="w-4 h-4 shrink-0 text-rose-400 mt-0.5" />
              <div>
                <span className="font-bold">Catalogue service error — </span>
                {patternsError || statsError || 'Failed to sync failure patterns from API.'}
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* ── Stat bar ─────────────────────────────────────────────────────────── */}
      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-6 gap-3">
        {statsLoading
          ? [...Array(6)].map((_, i) => (
            <div key={i} className="h-[72px] rounded-xl border border-slate-800/50 bg-slate-800/20 animate-pulse" />
          ))
          : statCards.map(({ value, label, accent }) => (
            <StatCard key={label} value={value} label={label} accent={accent} mono />
          ))
        }
      </div>

      {/* ── Filter & search bar ───────────────────────────────────────────────── */}
      <div className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-4
                      rounded-xl border border-slate-800/60 bg-slate-900/40 backdrop-blur-sm px-4 py-3">

        {/* Category tabs */}
        <div className="flex flex-wrap items-center gap-1">
          {tabs.map(({ value, label }) => {
            const active = activeTab === value;
            const m = getMeta(value);
            return (
              <button
                key={value}
                onClick={() => setActiveTab(value)}
                className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-[11px] font-semibold
                            transition-all active:scale-95 cursor-pointer ${active
                    ? 'bg-indigo-500/15 ring-1 ring-indigo-500/35 text-indigo-300'
                    : 'text-slate-500 hover:text-slate-200 hover:bg-slate-800/40'
                  }`}
              >
                {value !== 'all' && (
                  <span className={active ? 'text-indigo-400' : 'text-slate-600'}>
                    {m.icon}
                  </span>
                )}
                {label}
              </button>
            );
          })}
        </div>

        {/* Search + sort */}
        <div className="flex items-center gap-2.5 shrink-0">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-500 pointer-events-none" />
            <input
              type="text"
              placeholder="Search patterns…"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-8 pr-3 py-2 w-52 rounded-xl border border-slate-800 bg-slate-950/60
                         text-xs text-slate-200 placeholder:text-slate-600
                         focus:outline-none focus:ring-1 focus:ring-indigo-500/40 focus:border-indigo-500/40
                         transition-colors"
            />
          </div>

          <div className="relative flex items-center">
            <ArrowUpDown className="absolute left-3 w-3.5 h-3.5 text-slate-500 pointer-events-none" />
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value as 'priority' | 'name')}
              className="pl-8 pr-7 py-2 rounded-xl border border-slate-800 bg-slate-950/60
                         text-[11px] font-semibold text-slate-300 appearance-none cursor-pointer
                         focus:outline-none focus:ring-1 focus:ring-indigo-500/40
                         transition-colors"
            >
              <option value="priority" className="bg-slate-900">Priority</option>
              <option value="name" className="bg-slate-900">A – Z</option>
            </select>
            <ChevronDown className="absolute right-2.5 w-3 h-3 text-slate-500 pointer-events-none" />
          </div>
        </div>
      </div>

      {/* ── Catalogue grid ───────────────────────────────────────────────────── */}
      {patternsLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="h-44 rounded-2xl border border-slate-800/50 bg-slate-800/10 animate-pulse" />
          ))}
        </div>
      ) : filteredPatterns.length === 0 ? (
        <div className="py-28 flex flex-col items-center gap-3 rounded-2xl
                        border border-slate-800/50 bg-slate-900/20">
          <Database className="w-10 h-10 text-slate-700" />
          <p className="text-sm text-slate-500">No matching failure signatures found in catalogue.</p>
          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="text-xs text-indigo-400 hover:underline cursor-pointer"
            >
              Clear search
            </button>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          <AnimatePresence mode="popLayout">
            {filteredPatterns.map((pattern) => (
              <PatternCard
                key={pattern.id}
                pattern={pattern}
                isExpanded={expandedId === pattern.id}
                onToggle={() => setExpandedId(expandedId === pattern.id ? null : pattern.id)}
              />
            ))}
          </AnimatePresence>
        </div>
      )}

      {/* result count */}
      {!patternsLoading && filteredPatterns.length > 0 && (
        <p className="text-center text-[11px] text-slate-600">
          Showing <span className="text-slate-400 font-semibold">{filteredPatterns.length}</span>
          {' '}of{' '}
          <span className="text-slate-400 font-semibold">{patterns?.length ?? 0}</span> patterns
        </p>
      )}
    </motion.div>
  );
}