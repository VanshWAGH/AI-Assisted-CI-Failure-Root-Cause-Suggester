import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  FileText, Shield, Hammer, TestTube, HardDrive, HelpCircle,
  ChevronLeft, ChevronRight, X, Copy, Check, Filter,
  Activity, Clock, Database, TrendingUp
} from 'lucide-react';
import { useHistory } from '../hooks/useDashboardData';
import api from '../api/client';
import type { FailureType } from '../types';

/* ─── Tiny helpers ─────────────────────────────────────────────────────────── */

const FAILURE_META: Record<
  FailureType | 'unknown',
  { icon: React.ReactNode; label: string; bar: string; pill: string; dot: string }
> = {
  infra: { icon: <HardDrive className="w-3.5 h-3.5" />, label: 'Infrastructure', bar: 'bg-amber-400', pill: 'bg-amber-400/10 text-amber-300   ring-amber-400/20', dot: 'bg-amber-400' },
  test: { icon: <TestTube className="w-3.5 h-3.5" />, label: 'Test Failure', bar: 'bg-violet-400', pill: 'bg-violet-400/10 text-violet-300 ring-violet-400/20', dot: 'bg-violet-400' },
  build: { icon: <Hammer className="w-3.5 h-3.5" />, label: 'Build Error', bar: 'bg-sky-400', pill: 'bg-sky-400/10    text-sky-300    ring-sky-400/20', dot: 'bg-sky-400' },
  security: { icon: <Shield className="w-3.5 h-3.5" />, label: 'Security', bar: 'bg-rose-400', pill: 'bg-rose-400/10   text-rose-300   ring-rose-400/20', dot: 'bg-rose-400' },
  unknown: { icon: <HelpCircle className="w-3.5 h-3.5" />, label: 'Unknown', bar: 'bg-slate-500', pill: 'bg-slate-500/10  text-slate-400  ring-slate-500/20', dot: 'bg-slate-500' },
};

const getMeta = (type: FailureType) =>
  FAILURE_META[type as keyof typeof FAILURE_META] ?? FAILURE_META.unknown;

const engineLabel = (mode: string) =>
  mode === 'ml_onnx' ? 'ML Model' : mode === 'rule_based' ? 'Rules' : 'Hybrid';

const FILTER_OPTIONS = [
  { value: '', label: 'All Categories' },
  { value: 'infra', label: 'Infrastructure' },
  { value: 'test', label: 'Test Failures' },
  { value: 'build', label: 'Build Errors' },
  { value: 'security', label: 'Security' },
];

/* ─── Row component ────────────────────────────────────────────────────────── */

function HistoryRow({
  item,
  onOpen,
}: {
  item: any;
  onOpen: (jobId: string, projectName: string) => void;
}) {
  const meta = getMeta(item.failureType);
  const pct = Math.round(item.confidence * 100);

  return (
    <motion.div
      initial={{ opacity: 0, y: 6 }}
      animate={{ opacity: 1, y: 0 }}
      className="group grid grid-cols-1 md:grid-cols-[minmax(0,2fr)_1fr_1fr_1fr_auto] gap-x-6 gap-y-2 px-6 py-4
                 border-b border-slate-800/50 last:border-b-0
                 hover:bg-white/[0.025] transition-colors duration-150"
    >
      {/* Project & job */}
      <div className="min-w-0 space-y-1">
        <p className="font-semibold text-slate-100 text-sm truncate leading-tight">
          {item.projectName}
        </p>
        <div className="flex flex-wrap items-center gap-x-2 gap-y-0.5 text-[11px]">
          <span className="font-mono text-slate-500 truncate">
            {item.externalJobId || item.jobId.slice(0, 8)}
          </span>
          {item.branchName && (
            <>
              <span className="text-slate-700">·</span>
              <span className="font-mono text-indigo-400/80">{item.branchName}</span>
            </>
          )}
        </div>
      </div>

      {/* Category */}
      <div className="flex items-center">
        <span
          className={`inline-flex items-center gap-1.5 text-[11px] font-semibold
                      px-2.5 py-1 rounded-full ring-1 ${meta.pill}`}
        >
          {meta.icon}
          {meta.label}
        </span>
      </div>

      {/* Confidence */}
      <div className="flex items-center gap-2">
        <div className="relative flex-1 max-w-[72px] h-1.5 bg-slate-800 rounded-full overflow-hidden">
          <motion.div
            className={`absolute inset-y-0 left-0 rounded-full ${meta.bar}`}
            initial={{ width: 0 }}
            animate={{ width: `${pct}%` }}
            transition={{ duration: 0.6, ease: 'easeOut', delay: 0.1 }}
          />
        </div>
        <span className="font-mono text-xs text-slate-300 tabular-nums w-9 text-right">{pct}%</span>
      </div>

      {/* Engine */}
      <div className="flex items-center">
        <span className="text-[11px] font-mono text-slate-500 group-hover:text-indigo-400 transition-colors">
          {engineLabel(item.classifierMode)}
        </span>
      </div>

      {/* Action */}
      <div className="flex items-center justify-start md:justify-end">
        <button
          onClick={() => onOpen(item.jobId, item.projectName)}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-[11px] font-semibold
                     bg-indigo-500/10 hover:bg-indigo-500/20 text-indigo-300 hover:text-indigo-200
                     ring-1 ring-indigo-500/20 hover:ring-indigo-500/40
                     active:scale-95 transition-all duration-150 cursor-pointer"
        >
          <FileText className="w-3.5 h-3.5" />
          Report
        </button>
      </div>
    </motion.div>
  );
}

/* ─── Main page ────────────────────────────────────────────────────────────── */

export default function HistoryPage() {
  const [page, setPage] = useState(0);
  const [filterType, setFilterType] = useState<string>('');
  const { data, loading, error } = useHistory(page, 10, filterType || undefined);

  /* report drawer */
  const [selectedJobId, setSelectedJobId] = useState<string | null>(null);
  const [selectedJobProject, setSelectedJobProject] = useState('');
  const [reportText, setReportText] = useState('');
  const [loadingReport, setLoadingReport] = useState(false);
  const [copiedReport, setCopiedReport] = useState(false);

  const handleOpenReport = async (jobId: string, projectName: string) => {
    setSelectedJobId(jobId);
    setSelectedJobProject(projectName);
    setLoadingReport(true);
    setReportText('');
    try {
      const res = await api.get<string>(`/jobs/${jobId}/report`, { responseType: 'text' });
      setReportText(res.data);
    } catch (err) {
      setReportText(`### Error\nFailed to fetch the Markdown report: ${(err as Error).message}`);
    } finally {
      setLoadingReport(false);
    }
  };

  const copyReport = () => {
    if (!reportText) return;
    navigator.clipboard.writeText(reportText).then(() => {
      setCopiedReport(true);
      setTimeout(() => setCopiedReport(false), 2000);
    });
  };

  /* ── render ── */
  return (
    <div className="min-h-screen pb-16 max-w-6xl mx-auto px-4 sm:px-6 space-y-6 font-sans">

      {/* ── Page header ─────────────────────────────────────────────────────── */}
      <motion.div
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        className="relative overflow-hidden rounded-2xl border border-slate-800/70
                   bg-slate-900/60 backdrop-blur-xl p-6 md:p-8"
      >
        {/* subtle gradient blob */}
        <div className="pointer-events-none absolute -top-20 -left-16 w-72 h-72
                        rounded-full bg-indigo-600/10 blur-3xl" />
        <div className="pointer-events-none absolute -bottom-10 -right-10 w-56 h-56
                        rounded-full bg-purple-600/8 blur-3xl" />

        <div className="relative flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
          {/* title */}
          <div>
            <div className="flex items-center gap-2 mb-2">
              <span className="inline-flex items-center justify-center w-7 h-7 rounded-lg bg-indigo-500/15 ring-1 ring-indigo-500/30">
                <Activity className="w-3.5 h-3.5 text-indigo-400" />
              </span>
              <span className="text-[11px] font-semibold uppercase tracking-widest text-indigo-400/70">
                Pipeline Intelligence
              </span>
            </div>
            <h1 className="text-2xl md:text-3xl font-extrabold tracking-tight text-white leading-tight">
              Analysis History
            </h1>
            <p className="text-slate-400 text-sm mt-1 max-w-md">
              Browse, filter, and audit past pipeline failure classification records.
            </p>
          </div>

          {/* stat chips */}
          <div className="flex flex-wrap gap-3 shrink-0">
            {[
              { icon: <Database className="w-3.5 h-3.5" />, label: 'Total Analyses', value: data?.totalElements ?? '—' },
              { icon: <TrendingUp className="w-3.5 h-3.5" />, label: 'Current Page', value: data ? `${page + 1} / ${data.totalPages || 1}` : '—' },
            ].map(({ icon, label, value }) => (
              <div
                key={label}
                className="flex items-center gap-2.5 px-3.5 py-2 rounded-xl
                           bg-slate-800/50 ring-1 ring-slate-700/50"
              >
                <span className="text-indigo-400">{icon}</span>
                <div>
                  <p className="text-[10px] text-slate-500 leading-none mb-0.5">{label}</p>
                  <p className="text-sm font-bold text-slate-200 tabular-nums">{value}</p>
                </div>
              </div>
            ))}

            {/* Filter */}
            <div className="flex items-center gap-2 pl-3 pr-2 py-2 rounded-xl
                            bg-slate-800/50 ring-1 ring-slate-700/50 cursor-pointer">
              <Filter className="w-3.5 h-3.5 text-slate-400 shrink-0" />
              <select
                value={filterType}
                onChange={(e) => { setFilterType(e.target.value); setPage(0); }}
                className="bg-transparent text-slate-200 text-xs font-semibold
                           focus:outline-none cursor-pointer pr-1"
              >
                {FILTER_OPTIONS.map((t) => (
                  <option key={t.value} value={t.value} className="bg-slate-900 text-slate-300">
                    {t.label}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>
      </motion.div>

      {/* ── Table card ──────────────────────────────────────────────────────── */}
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.05 }}
        className="rounded-2xl border border-slate-800/70 bg-slate-900/50 backdrop-blur-xl overflow-hidden"
      >
        {loading ? (
          <div className="py-24 flex flex-col items-center gap-3">
            <div className="w-7 h-7 rounded-full border-2 border-indigo-500/15 border-t-indigo-500 animate-spin" />
            <span className="text-xs text-slate-500 animate-pulse font-medium tracking-wide">
              Fetching history records…
            </span>
          </div>
        ) : error ? (
          <div className="py-24 text-center">
            <p className="text-sm text-rose-400">Failed to load analysis history</p>
            <p className="text-xs text-slate-600 mt-1">{error}</p>
          </div>
        ) : !data || data.content.length === 0 ? (
          <div className="py-24 flex flex-col items-center gap-2 text-center">
            <Clock className="w-8 h-8 text-slate-700" />
            <p className="text-sm text-slate-500">No history records found for the selected category.</p>
          </div>
        ) : (
          <>
            {/* Column headers */}
            <div className="hidden md:grid grid-cols-[minmax(0,2fr)_1fr_1fr_1fr_auto] gap-x-6
                            px-6 py-3 bg-slate-950/40 border-b border-slate-800/60">
              {['Project & Job', 'Category', 'Confidence', 'Engine', 'Action'].map((h) => (
                <p key={h} className="text-[10px] font-bold uppercase tracking-widest text-slate-500">
                  {h}
                </p>
              ))}
            </div>

            {/* Rows */}
            <AnimatePresence mode="wait">
              <motion.div
                key={`${page}-${filterType}`}
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                transition={{ duration: 0.15 }}
              >
                {data.content.map((item: any) => (
                  <HistoryRow key={item.analysisId} item={item} onOpen={handleOpenReport} />
                ))}
              </motion.div>
            </AnimatePresence>

            {/* Pagination */}
            <div className="flex items-center justify-between px-6 py-3.5
                            bg-slate-950/30 border-t border-slate-800/60">
              <p className="text-xs text-slate-500">
                Page{' '}
                <span className="text-slate-300 font-semibold">{page + 1}</span>
                {' '}of{' '}
                <span className="text-slate-300 font-semibold">{data.totalPages || 1}</span>
                <span className="hidden sm:inline text-slate-600">
                  {' '}· {data.totalElements} total
                </span>
              </p>
              <div className="flex items-center gap-1.5">
                <button
                  disabled={data.first}
                  onClick={() => setPage((p) => Math.max(p - 1, 0))}
                  className="p-1.5 rounded-lg border border-slate-800 bg-slate-900/60
                             text-slate-400 hover:text-slate-200 hover:border-slate-700
                             disabled:opacity-25 disabled:pointer-events-none transition cursor-pointer"
                >
                  <ChevronLeft className="w-4 h-4" />
                </button>
                <button
                  disabled={data.last}
                  onClick={() => setPage((p) => p + 1)}
                  className="p-1.5 rounded-lg border border-slate-800 bg-slate-900/60
                             text-slate-400 hover:text-slate-200 hover:border-slate-700
                             disabled:opacity-25 disabled:pointer-events-none transition cursor-pointer"
                >
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          </>
        )}
      </motion.div>

      {/* ── Report Drawer ────────────────────────────────────────────────────── */}
      <AnimatePresence>
        {selectedJobId && (
          <div className="fixed inset-0 z-50 flex items-stretch justify-end">
            {/* Backdrop */}
            <motion.div
              key="backdrop"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setSelectedJobId(null)}
              className="absolute inset-0 bg-black/65 backdrop-blur-sm"
            />

            {/* Panel */}
            <motion.aside
              key="drawer"
              initial={{ x: '100%' }}
              animate={{ x: 0 }}
              exit={{ x: '100%' }}
              transition={{ type: 'spring', damping: 28, stiffness: 220 }}
              className="relative w-full max-w-2xl h-full flex flex-col
                         bg-slate-950 border-l border-slate-800/80 shadow-2xl"
            >
              {/* Drawer header */}
              <div className="flex items-center justify-between gap-4 px-6 py-4
                              border-b border-slate-800/80 bg-slate-900/60 backdrop-blur-sm shrink-0">
                <div className="min-w-0">
                  <h3 className="text-base font-bold text-slate-100 leading-tight">
                    Root-Cause Report
                  </h3>
                  <p className="text-[11px] font-mono text-slate-500 truncate mt-0.5">
                    {selectedJobProject}
                  </p>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <motion.button
                    whileTap={{ scale: 0.92 }}
                    onClick={copyReport}
                    disabled={loadingReport || !reportText}
                    className="p-2 rounded-lg border border-slate-800 bg-slate-900/70
                               text-slate-400 hover:text-slate-200 hover:border-slate-700
                               disabled:opacity-30 transition cursor-pointer"
                    title="Copy Markdown"
                  >
                    <AnimatePresence mode="wait" initial={false}>
                      {copiedReport
                        ? <motion.span key="check" initial={{ scale: 0.6 }} animate={{ scale: 1 }} exit={{ scale: 0.6 }}>
                          <Check className="w-4 h-4 text-emerald-400" />
                        </motion.span>
                        : <motion.span key="copy" initial={{ scale: 0.6 }} animate={{ scale: 1 }} exit={{ scale: 0.6 }}>
                          <Copy className="w-4 h-4" />
                        </motion.span>
                      }
                    </AnimatePresence>
                  </motion.button>
                  <button
                    onClick={() => setSelectedJobId(null)}
                    className="p-2 rounded-lg border border-slate-800 bg-slate-900/70
                               text-slate-400 hover:text-slate-200 hover:border-slate-700 transition cursor-pointer"
                  >
                    <X className="w-4 h-4" />
                  </button>
                </div>
              </div>

              {/* Drawer body */}
              <div className="flex-1 overflow-y-auto p-6 scrollbar-thin scrollbar-thumb-slate-800 scrollbar-track-transparent">
                {loadingReport ? (
                  <div className="h-full flex flex-col items-center justify-center gap-3">
                    <div className="w-7 h-7 rounded-full border-2 border-indigo-500/15 border-t-indigo-500 animate-spin" />
                    <span className="text-xs text-slate-500 animate-pulse tracking-wide">
                      Generating markdown dossier…
                    </span>
                  </div>
                ) : (
                  <motion.div
                    initial={{ opacity: 0, y: 6 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="rounded-xl border border-slate-800/70 bg-slate-900/60 overflow-hidden"
                  >
                    {/* code-block top bar */}
                    <div className="flex items-center gap-1.5 px-4 py-2.5 border-b border-slate-800/70 bg-slate-800/30">
                      {['bg-rose-500/60', 'bg-amber-500/60', 'bg-emerald-500/60'].map((c) => (
                        <span key={c} className={`w-2.5 h-2.5 rounded-full ${c}`} />
                      ))}
                      <span className="ml-2 text-[10px] font-mono text-slate-600">markdown</span>
                    </div>
                    <pre className="p-5 text-xs font-mono text-slate-300 leading-relaxed
                                   whitespace-pre-wrap break-words overflow-x-auto">
                      {reportText}
                    </pre>
                  </motion.div>
                )}
              </div>
            </motion.aside>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}