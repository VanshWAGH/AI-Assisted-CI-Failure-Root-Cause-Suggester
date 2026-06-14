import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Settings, Server, Key, Check, Workflow, Copy, Terminal,
  Activity, Save, Info, ShieldCheck, Cpu, RotateCcw, Zap,
  RefreshCw, AlignJustify, ChevronDown
} from 'lucide-react';
import api from '../api/client';

/* ─── SVG icons ─────────────────────────────────────────────────────────────── */

const GithubIcon = ({ className }: { className?: string }) => (
  <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" strokeWidth="2"
    fill="none" strokeLinecap="round" strokeLinejoin="round" className={className}>
    <path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22" />
  </svg>
);

const GitlabIcon = ({ className }: { className?: string }) => (
  <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" strokeWidth="2"
    fill="none" strokeLinecap="round" strokeLinejoin="round" className={className}>
    <path d="M22.65 14.39L12 22.13 1.35 14.39a.84.84 0 0 1-.3-.94l2.07-6.38a.84.84 0 0 1 .8-.58h3.48l3.7-11.64a.78.78 0 0 1 1.5 0l3.7 11.64h3.48a.84.84 0 0 1 .8.58l2.07 6.38a.84.84 0 0 1-.3.94z" />
  </svg>
);

/* ─── Shared primitives ─────────────────────────────────────────────────────── */

function SectionHeading({ icon, children }: { icon: React.ReactNode; children: React.ReactNode }) {
  return (
    <h2 className="flex items-center gap-2 text-[11px] font-bold uppercase tracking-widest text-slate-400
                   pb-3 border-b border-slate-800/70">
      <span className="text-emerald-400">{icon}</span>
      {children}
    </h2>
  );
}

function FieldLabel({ children }: { children: React.ReactNode }) {
  return (
    <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-wider mb-1.5">
      {children}
    </label>
  );
}

const inputCls = `w-full px-3.5 py-2.5 rounded-xl border border-slate-800 bg-slate-950/70
                  text-slate-200 text-xs font-mono placeholder:text-slate-600
                  focus:outline-none focus:ring-1 focus:ring-emerald-500/40 focus:border-emerald-500/40
                  transition-colors`;

const selectCls = `${inputCls} appearance-none cursor-pointer font-sans font-semibold`;

function StyledSelect({ value, onChange, options }: {
  value: string | number;
  onChange: (v: string) => void;
  options: { value: string | number; label: string }[];
}) {
  return (
    <div className="relative">
      <select value={value} onChange={(e) => onChange(e.target.value)} className={selectCls}>
        {options.map((o) => (
          <option key={o.value} value={o.value} className="bg-slate-900">{o.label}</option>
        ))}
      </select>
      <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-500 pointer-events-none" />
    </div>
  );
}

/* ─── Copy button ─────────────────────────────────────────────────────────── */

function CopyBtn({ text, id, copiedId, onCopy, label = 'Copy' }: {
  text: string; id: string; copiedId: string | null;
  onCopy: (text: string, id: string) => void; label?: string;
}) {
  const copied = copiedId === id;
  return (
    <button
      onClick={() => onCopy(text, id)}
      className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-[10px] font-semibold
                 bg-indigo-500/10 hover:bg-indigo-500/20 text-indigo-300 ring-1 ring-indigo-500/20
                 hover:ring-indigo-500/40 active:scale-95 transition-all cursor-pointer"
    >
      <AnimatePresence mode="wait" initial={false}>
        {copied
          ? <motion.span key="c" initial={{ scale: 0.7 }} animate={{ scale: 1 }} exit={{ scale: 0.7 }}><Check className="w-3 h-3 text-emerald-400" /></motion.span>
          : <motion.span key="d" initial={{ scale: 0.7 }} animate={{ scale: 1 }} exit={{ scale: 0.7 }}><Copy className="w-3 h-3" /></motion.span>
        }
      </AnimatePresence>
      {copied ? 'Copied!' : label}
    </button>
  );
}

/* ─── Code block ─────────────────────────────────────────────────────────── */

function CodeBlock({ code, id, lang, copiedId, onCopy }: {
  code: string; id: string; lang: string; copiedId: string | null;
  onCopy: (text: string, id: string) => void;
}) {
  return (
    <div className="rounded-xl border border-slate-800/70 overflow-hidden">
      <div className="flex items-center justify-between px-4 py-2 bg-slate-800/40 border-b border-slate-800/70">
        <div className="flex items-center gap-2">
          <div className="flex gap-1">
            {['bg-rose-500/50', 'bg-amber-500/50', 'bg-emerald-500/50'].map((c) => (
              <span key={c} className={`w-2 h-2 rounded-full ${c}`} />
            ))}
          </div>
          <span className="text-[10px] font-mono text-slate-600">{lang}</span>
        </div>
        <CopyBtn text={code} id={id} copiedId={copiedId} onCopy={onCopy} label="Copy" />
      </div>
      <pre className="p-4 bg-slate-950/60 font-mono text-[10.5px] text-slate-300
                     overflow-x-auto whitespace-pre-wrap leading-relaxed select-all">
        {code}
      </pre>
    </div>
  );
}

/* ─── Toast ─────────────────────────────────────────────────────────────── */

interface ToastState { show: boolean; message: string; type: 'success' | 'error' | 'info' }

const toastConfig = {
  success: { bg: 'bg-emerald-950/90 border-emerald-500/25 text-emerald-300', icon: <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" /> },
  error: { bg: 'bg-rose-950/90 border-rose-500/25 text-rose-300', icon: <Zap className="w-3.5 h-3.5 text-rose-400" /> },
  info: { bg: 'bg-indigo-950/90 border-indigo-500/25 text-indigo-300', icon: <Info className="w-3.5 h-3.5 text-indigo-400" /> },
};

/* ─── Main Page ─────────────────────────────────────────────────────────── */

export default function SettingsPage() {
  const [apiUrl, setApiUrl] = useState('');
  const [apiKey, setApiKey] = useState('');
  const [isTesting, setIsTesting] = useState(false);
  const [connectionStatus, setConnectionStatus] = useState<'idle' | 'success' | 'failed'>('idle');
  const [testResult, setTestResult] = useState('');
  const [refreshInterval, setRefreshInterval] = useState(30);
  const [classifierPref, setClassifierPref] = useState('hybrid');
  const [maxLogLines, setMaxLogLines] = useState(300);
  const [activeIntTab, setActiveIntTab] = useState<'github' | 'gitlab' | 'jenkins'>('github');
  const [copiedText, setCopiedText] = useState<string | null>(null);
  const [toast, setToast] = useState<ToastState>({ show: false, message: '', type: 'success' });

  useEffect(() => {
    setApiUrl(localStorage.getItem('ROOTCAUSE_API_URL') || import.meta.env.VITE_API_BASE_URL || window.location.origin + '/api/v1');
    setApiKey(localStorage.getItem('ROOTCAUSE_API_KEY') || '');
    const si = localStorage.getItem('ROOTCAUSE_REFRESH_INTERVAL'); if (si) setRefreshInterval(Number(si));
    const cp = localStorage.getItem('ROOTCAUSE_CLASSIFIER_PREF'); if (cp) setClassifierPref(cp);
    const ml = localStorage.getItem('ROOTCAUSE_MAX_LOG_LINES'); if (ml) setMaxLogLines(Number(ml));
  }, []);

  const triggerToast = (message: string, type: ToastState['type']) => {
    setToast({ show: true, message, type });
    setTimeout(() => setToast((p) => ({ ...p, show: false })), 3000);
  };

  const handleTestConnection = async () => {
    setIsTesting(true); setConnectionStatus('idle'); setTestResult('');
    try {
      localStorage.setItem('ROOTCAUSE_API_URL', apiUrl);
      localStorage.setItem('ROOTCAUSE_API_KEY', apiKey);
      await api.get('/status');
      setConnectionStatus('success');
      setTestResult('Successfully contacted RootCause API. Server version 1.0.0.');
      triggerToast('API connection test passed.', 'success');
    } catch (err) {
      setConnectionStatus('failed');
      setTestResult((err as Error).message || 'Endpoint unreachable. Ensure CORS is enabled and port is open.');
      triggerToast('API connection test failed.', 'error');
    } finally {
      setIsTesting(false);
    }
  };

  const handleSaveSettings = () => {
    localStorage.setItem('ROOTCAUSE_API_URL', apiUrl);
    localStorage.setItem('ROOTCAUSE_API_KEY', apiKey);
    localStorage.setItem('ROOTCAUSE_REFRESH_INTERVAL', String(refreshInterval));
    localStorage.setItem('ROOTCAUSE_CLASSIFIER_PREF', classifierPref);
    localStorage.setItem('ROOTCAUSE_MAX_LOG_LINES', String(maxLogLines));
    triggerToast('All configuration overrides saved successfully.', 'success');
  };

  const handleResetSettings = () => {
    ['ROOTCAUSE_API_URL', 'ROOTCAUSE_API_KEY', 'ROOTCAUSE_REFRESH_INTERVAL',
      'ROOTCAUSE_CLASSIFIER_PREF', 'ROOTCAUSE_MAX_LOG_LINES'].forEach((k) => localStorage.removeItem(k));
    setApiUrl(import.meta.env.VITE_API_BASE_URL || window.location.origin + '/api/v1');
    setApiKey(''); setRefreshInterval(30); setClassifierPref('hybrid'); setMaxLogLines(300);
    triggerToast('Configuration reset to defaults.', 'info');
  };

  const handleCopy = (text: string, id: string) => {
    navigator.clipboard.writeText(text).then(() => {
      setCopiedText(id);
      setTimeout(() => setCopiedText(null), 2000);
      triggerToast('Copied to clipboard.', 'success');
    });
  };

  /* webhook URLs */
  const origin = window.location.origin;
  const githubWebhookUrl = `${origin}/webhooks/github/workflow-run`;
  const gitlabWebhookUrl = `${origin}/webhooks/gitlab/pipeline`;
  const jenkinsWebhookUrl = `${origin}/webhooks/jenkins/build`;

  const githubCurl = `curl -X POST "${githubWebhookUrl}" \\
  -H "Content-Type: application/json" \\
  -H "X-Hub-Signature-256: sha256=MOCK_SHA256_VALUE" \\
  -d '{
    "action": "completed",
    "workflow_run": {
      "id": 12345678,
      "name": "Production Build",
      "head_branch": "main",
      "head_sha": "a1b2c3d4e5f6",
      "conclusion": "failure",
      "repository": { "full_name": "acme-corp/main-application" }
    }
  }'`;

  const gitlabCurl = `curl -X POST "${gitlabWebhookUrl}" \\
  -H "Content-Type: application/json" \\
  -H "X-Gitlab-Token: YOUR_GITLAB_SECRET_TOKEN" \\
  -d '{
    "object_kind": "pipeline",
    "object_attributes": {
      "id": 876543,
      "ref": "release-v2.1",
      "sha": "f6e5d4c3b2a1",
      "status": "failed"
    },
    "project": {
      "path_with_namespace": "devops-group/build-deployer"
    }
  }'`;

  const jenkinsPipeline = `post {
    failure {
        sh """
        curl -X POST "${jenkinsWebhookUrl}" \\
          -H "Content-Type: application/json" \\
          -d '{
            "projectName": "\\${env.JOB_NAME}",
            "buildNumber": \\${env.BUILD_NUMBER},
            "buildUrl": "\\${env.BUILD_URL}"
          }'
        """
    }
}`;

  /* tab config */
  const tabs = [
    { id: 'github' as const, label: 'GitHub', icon: <GithubIcon /> },
    { id: 'gitlab' as const, label: 'GitLab', icon: <GitlabIcon /> },
    { id: 'jenkins' as const, label: 'Jenkins', icon: <Cpu className="w-3.5 h-3.5" /> },
  ];

  /* ── render ── */
  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -12 }}
      transition={{ duration: 0.35 }}
      className="space-y-6 pb-16 max-w-6xl mx-auto px-4 sm:px-6 relative font-sans"
    >

      {/* ── Toast ──────────────────────────────────────────────────────────── */}
      <AnimatePresence>
        {toast.show && (
          <motion.div
            key="toast"
            initial={{ opacity: 0, y: -16, scale: 0.92 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -16, scale: 0.92 }}
            className={`fixed top-4 right-4 z-[999] inline-flex items-center gap-2.5
                        px-4 py-2.5 rounded-xl border shadow-xl backdrop-blur-xl
                        text-xs font-semibold ${toastConfig[toast.type].bg}`}
          >
            {toastConfig[toast.type].icon}
            {toast.message}
          </motion.div>
        )}
      </AnimatePresence>

      {/* ── Page header ─────────────────────────────────────────────────────── */}
      <div className="relative overflow-hidden rounded-2xl border border-slate-800/70
                      bg-slate-900/60 backdrop-blur-xl px-6 py-7 md:px-8">
        {/* blobs */}
        <div className="pointer-events-none absolute -top-16 -left-12 w-64 h-64
                        rounded-full bg-emerald-600/8 blur-3xl" />
        <div className="pointer-events-none absolute -bottom-12 -right-12 w-56 h-56
                        rounded-full bg-teal-600/8 blur-3xl" />

        <div className="relative flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-2">
              <span className="inline-flex items-center justify-center w-7 h-7 rounded-lg
                               bg-emerald-500/15 ring-1 ring-emerald-500/30">
                <Settings className="w-3.5 h-3.5 text-emerald-400" />
              </span>
              <span className="text-[11px] font-semibold uppercase tracking-widest text-emerald-400/70">
                System Control
              </span>
            </div>
            <h1 className="text-2xl md:text-3xl font-extrabold tracking-tight text-white">
              Settings & Integrations
            </h1>
            <p className="text-slate-400 text-sm mt-1 max-w-lg">
              Configure API environments, UI preferences, and CI/CD webhook connections.
            </p>
          </div>

          {/* quick-action buttons */}
          <div className="flex items-center gap-2.5 shrink-0">
            <button
              onClick={handleResetSettings}
              className="inline-flex items-center gap-2 px-3.5 py-2 rounded-xl text-xs font-semibold
                         border border-slate-800 bg-slate-900/60 text-slate-400
                         hover:text-slate-200 hover:border-slate-700 active:scale-95 transition cursor-pointer"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              Reset
            </button>
            <button
              onClick={handleSaveSettings}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold
                         bg-emerald-500 hover:bg-emerald-400 text-white
                         shadow-lg shadow-emerald-500/20 active:scale-95 transition cursor-pointer"
            >
              <Save className="w-3.5 h-3.5" />
              Save Changes
            </button>
          </div>
        </div>
      </div>

      {/* ── Main grid ──────────────────────────────────────────────────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

        {/* ── Left column ── */}
        <div className="lg:col-span-1 space-y-5">

          {/* API Connection */}
          <div className="rounded-2xl border border-slate-800/70 bg-slate-900/50 backdrop-blur-xl p-5 space-y-4">
            <SectionHeading icon={<Server className="w-3.5 h-3.5" />}>API Connection</SectionHeading>

            <div className="space-y-3">
              <div>
                <FieldLabel>Server Base URL</FieldLabel>
                <input
                  type="text"
                  value={apiUrl}
                  onChange={(e) => setApiUrl(e.target.value)}
                  className={inputCls}
                  placeholder="http://localhost:8080/api/v1"
                />
              </div>

              <div>
                <FieldLabel>API Key Override (X-API-Key)</FieldLabel>
                <div className="relative">
                  <Key className="absolute left-3.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-600 pointer-events-none" />
                  <input
                    type="password"
                    value={apiKey}
                    onChange={(e) => setApiKey(e.target.value)}
                    className={`${inputCls} pl-9`}
                    placeholder="Enter security key…"
                  />
                </div>
              </div>
            </div>

            {/* Test button + result */}
            <div className="pt-1 space-y-2">
              <button
                onClick={handleTestConnection}
                disabled={isTesting || !apiUrl}
                className="w-full inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl
                           border border-slate-700 bg-slate-800/40 hover:bg-slate-700/50
                           text-xs font-semibold text-slate-300
                           disabled:opacity-40 active:scale-[0.98] transition cursor-pointer"
              >
                {isTesting
                  ? <RefreshCw className="w-3.5 h-3.5 animate-spin text-emerald-400" />
                  : <Activity className="w-3.5 h-3.5" />
                }
                {isTesting ? 'Testing connection…' : 'Test Server Connectivity'}
              </button>

              <AnimatePresence>
                {connectionStatus !== 'idle' && (
                  <motion.div
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: 'auto' }}
                    exit={{ opacity: 0, height: 0 }}
                    className={`overflow-hidden rounded-xl border p-3 text-[11px] leading-relaxed font-medium ${connectionStatus === 'success'
                        ? 'bg-emerald-950/25 border-emerald-900/30 text-emerald-300'
                        : 'bg-rose-950/25 border-rose-900/30 text-rose-300'
                      }`}
                  >
                    {testResult}
                  </motion.div>
                )}
              </AnimatePresence>
            </div>
          </div>

          {/* Console Preferences */}
          <div className="rounded-2xl border border-slate-800/70 bg-slate-900/50 backdrop-blur-xl p-5 space-y-4">
            <SectionHeading icon={<Settings className="w-3.5 h-3.5" />}>Console Preferences</SectionHeading>

            <div className="space-y-3">
              <div>
                <FieldLabel>
                  <span className="flex items-center gap-1"><RefreshCw className="w-3 h-3" /> Auto-refresh Interval</span>
                </FieldLabel>
                <StyledSelect
                  value={refreshInterval}
                  onChange={(v) => setRefreshInterval(Number(v))}
                  options={[
                    { value: 15, label: 'Every 15 seconds' },
                    { value: 30, label: 'Every 30 seconds' },
                    { value: 60, label: 'Every 60 seconds' },
                    { value: 0, label: 'Disabled' },
                  ]}
                />
              </div>

              <div>
                <FieldLabel>
                  <span className="flex items-center gap-1"><Cpu className="w-3 h-3" /> Classifier Mode</span>
                </FieldLabel>
                <StyledSelect
                  value={classifierPref}
                  onChange={setClassifierPref}
                  options={[
                    { value: 'hybrid', label: 'Hybrid — Heuristics + Deep Learning' },
                    { value: 'ml_onnx', label: 'Deep Learning only (ONNX)' },
                    { value: 'rule_based', label: 'Rule-Based Heuristics only' },
                  ]}
                />
              </div>

              <div>
                <FieldLabel>
                  <span className="flex items-center gap-1"><AlignJustify className="w-3 h-3" /> Max Lines in Log Viewer</span>
                </FieldLabel>
                <StyledSelect
                  value={maxLogLines}
                  onChange={(v) => setMaxLogLines(Number(v))}
                  options={[
                    { value: 100, label: '100 lines' },
                    { value: 300, label: '300 lines' },
                    { value: 500, label: '500 lines' },
                    { value: 1000, label: '1000 lines' },
                  ]}
                />
              </div>
            </div>
          </div>
        </div>

        {/* ── Right column: Webhook integrations ── */}
        <div className="lg:col-span-2 rounded-2xl border border-slate-800/70 bg-slate-900/50 backdrop-blur-xl overflow-hidden flex flex-col">

          {/* Panel header */}
          <div className="px-6 py-4 border-b border-slate-800/70 bg-slate-950/30
                          flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <h2 className="flex items-center gap-2 text-[11px] font-bold uppercase tracking-widest text-slate-400">
              <Workflow className="w-3.5 h-3.5 text-emerald-400" />
              CI/CD Webhook Integrations
            </h2>

            {/* Tab switcher */}
            <div className="flex items-center gap-1 p-1 bg-slate-950/50 border border-slate-800/70 rounded-xl">
              {tabs.map(({ id, label, icon }) => {
                const active = activeIntTab === id;
                return (
                  <button
                    key={id}
                    onClick={() => setActiveIntTab(id)}
                    className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-[10px] font-bold
                                uppercase tracking-wide transition-all active:scale-95 cursor-pointer ${active
                        ? 'bg-slate-800 text-slate-100 ring-1 ring-slate-700/60'
                        : 'text-slate-500 hover:text-slate-300'
                      }`}
                  >
                    <span className={active ? 'text-emerald-400' : ''}>{icon}</span>
                    {label}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Tab content */}
          <div className="flex-1 p-6 overflow-y-auto scrollbar-thin scrollbar-thumb-slate-800 scrollbar-track-transparent">
            <AnimatePresence mode="wait">

              {/* ── GitHub ── */}
              {activeIntTab === 'github' && (
                <motion.div key="github"
                  initial={{ opacity: 0, x: 12 }} animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -12 }} transition={{ duration: 0.2 }}
                  className="space-y-5"
                >
                  {/* URL display */}
                  <div className="rounded-xl border border-slate-800/70 bg-slate-950/30 p-4 space-y-2.5">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-slate-200">GitHub Actions Webhook URL</span>
                      <CopyBtn text={githubWebhookUrl} id="github-url" copiedId={copiedText} onCopy={handleCopy} label="Copy URL" />
                    </div>
                    <p className="font-mono text-xs text-indigo-300 break-all bg-slate-950/60
                                  px-3 py-2 rounded-lg border border-slate-800/70 select-all">
                      {githubWebhookUrl}
                    </p>
                  </div>

                  {/* Steps */}
                  <div className="space-y-2">
                    <p className="text-[10px] font-bold uppercase tracking-widest text-slate-500">Setup Steps</p>
                    <ol className="space-y-2">
                      {[
                        <>Open repository settings on GitHub.</>,
                        <>Navigate to <strong className="text-slate-200">Webhooks › Add Webhook</strong>.</>,
                        <>Paste the Webhook URL above as the <strong className="text-slate-200">Payload URL</strong>.</>,
                        <>Set Content type to <code className="text-indigo-300 font-mono">application/json</code>.</>,
                        <>Enter the secret matching <code className="text-indigo-300 font-mono">rootcause.github.webhook-secret</code>.</>,
                        <>Select <strong className="text-slate-200">Individual events</strong> and toggle <span className="text-indigo-400 font-medium">Workflow runs</span>.</>,
                        <>Click <span className="text-emerald-400 font-semibold">Add Webhook</span> — done!</>,
                      ].map((step, i) => (
                        <li key={i} className="flex gap-3 text-xs text-slate-400 leading-relaxed">
                          <span className="shrink-0 mt-0.5 w-4.5 h-4.5 flex items-center justify-center
                                           rounded-full bg-slate-800 ring-1 ring-slate-700 text-[9px]
                                           font-bold text-slate-400">
                            {i + 1}
                          </span>
                          <span>{step}</span>
                        </li>
                      ))}
                    </ol>
                  </div>

                  {/* cURL */}
                  <div className="space-y-2">
                    <div className="flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-widest text-slate-500">
                      <Terminal className="w-3 h-3 text-indigo-400" />
                      Verification Command
                    </div>
                    <CodeBlock code={githubCurl} id="github-curl" lang="bash" copiedId={copiedText} onCopy={handleCopy} />
                  </div>
                </motion.div>
              )}

              {/* ── GitLab ── */}
              {activeIntTab === 'gitlab' && (
                <motion.div key="gitlab"
                  initial={{ opacity: 0, x: 12 }} animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -12 }} transition={{ duration: 0.2 }}
                  className="space-y-5"
                >
                  <div className="rounded-xl border border-slate-800/70 bg-slate-950/30 p-4 space-y-2.5">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-slate-200">GitLab CI/CD Webhook URL</span>
                      <CopyBtn text={gitlabWebhookUrl} id="gitlab-url" copiedId={copiedText} onCopy={handleCopy} label="Copy URL" />
                    </div>
                    <p className="font-mono text-xs text-indigo-300 break-all bg-slate-950/60
                                  px-3 py-2 rounded-lg border border-slate-800/70 select-all">
                      {gitlabWebhookUrl}
                    </p>
                  </div>

                  <div className="space-y-2">
                    <p className="text-[10px] font-bold uppercase tracking-widest text-slate-500">Setup Steps</p>
                    <ol className="space-y-2">
                      {[
                        <>Go to your GitLab project's <strong className="text-slate-200">Settings › Webhooks</strong>.</>,
                        <>Paste the Webhook URL in the <strong className="text-slate-200">URL</strong> field.</>,
                        <>Enter the secret matching <code className="text-indigo-300 font-mono">rootcause.gitlab.secret-token</code>.</>,
                        <>Select <span className="text-indigo-400 font-medium">Pipeline events</span> as the trigger.</>,
                        <>Enable <strong className="text-slate-200">SSL verification</strong> for production HTTPS.</>,
                        <>Click <span className="text-emerald-400 font-semibold">Add Webhook</span> — done!</>,
                      ].map((step, i) => (
                        <li key={i} className="flex gap-3 text-xs text-slate-400 leading-relaxed">
                          <span className="shrink-0 mt-0.5 w-4.5 h-4.5 flex items-center justify-center
                                           rounded-full bg-slate-800 ring-1 ring-slate-700 text-[9px]
                                           font-bold text-slate-400">
                            {i + 1}
                          </span>
                          <span>{step}</span>
                        </li>
                      ))}
                    </ol>
                  </div>

                  <div className="space-y-2">
                    <div className="flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-widest text-slate-500">
                      <Terminal className="w-3 h-3 text-indigo-400" />
                      Verification Command
                    </div>
                    <CodeBlock code={gitlabCurl} id="gitlab-curl" lang="bash" copiedId={copiedText} onCopy={handleCopy} />
                  </div>
                </motion.div>
              )}

              {/* ── Jenkins ── */}
              {activeIntTab === 'jenkins' && (
                <motion.div key="jenkins"
                  initial={{ opacity: 0, x: 12 }} animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -12 }} transition={{ duration: 0.2 }}
                  className="space-y-5"
                >
                  <div className="rounded-xl border border-slate-800/70 bg-slate-950/30 p-4 space-y-2.5">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-slate-200">Jenkins Build Webhook URL</span>
                      <CopyBtn text={jenkinsWebhookUrl} id="jenkins-url" copiedId={copiedText} onCopy={handleCopy} label="Copy URL" />
                    </div>
                    <p className="font-mono text-xs text-indigo-300 break-all bg-slate-950/60
                                  px-3 py-2 rounded-lg border border-slate-800/70 select-all">
                      {jenkinsWebhookUrl}
                    </p>
                  </div>

                  <div className="space-y-2">
                    <p className="text-[10px] font-bold uppercase tracking-widest text-slate-500">Jenkinsfile Setup</p>
                    <p className="text-xs text-slate-400 leading-relaxed">
                      Add a <code className="text-indigo-300 font-mono">post</code> failure handler to your Jenkinsfile:
                    </p>
                    <CodeBlock code={jenkinsPipeline} id="jenkins-pipeline" lang="groovy" copiedId={copiedText} onCopy={handleCopy} />
                  </div>

                  {/* info note */}
                  <div className="flex gap-3 p-3.5 rounded-xl border border-indigo-900/30 bg-indigo-950/15">
                    <Info className="w-3.5 h-3.5 text-indigo-400 shrink-0 mt-0.5" />
                    <p className="text-[11px] text-slate-400 leading-relaxed">
                      When contacted, RootCause fetches console logs from your build URL, classifies
                      the root-cause using the configured engine, and posts analysis back automatically.
                    </p>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </div>
      </div>
    </motion.div>
  );
}