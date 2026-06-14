import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Sparkles, Terminal, Copy, Check, Info, ArrowLeft,
  RefreshCw, Cpu, Shield, HelpCircle, HardDrive, TestTube,
  Hammer, ChevronRight, Activity, Zap, AlertTriangle
} from 'lucide-react';
import { useAnalyze } from '../hooks/useDashboardData';
import LogViewer from '../components/LogViewer';
import type { CiPlatform } from '../types';

/* ─── Subtle animated gradient mesh background ─── */
const MeshBackground = () => (
  <div
    aria-hidden
    style={{
      position: 'fixed', inset: 0, zIndex: 0, pointerEvents: 'none', overflow: 'hidden',
    }}
  >
    <div style={{
      position: 'absolute', top: '-20%', left: '-10%', width: '55%', height: '55%',
      background: 'radial-gradient(ellipse, rgba(99,102,241,0.07) 0%, transparent 70%)',
      animation: 'drift1 18s ease-in-out infinite alternate',
    }} />
    <div style={{
      position: 'absolute', bottom: '-15%', right: '-5%', width: '45%', height: '45%',
      background: 'radial-gradient(ellipse, rgba(20,184,166,0.06) 0%, transparent 70%)',
      animation: 'drift2 22s ease-in-out infinite alternate',
    }} />
    <style>{`
      @keyframes drift1 { from { transform: translate(0,0) scale(1); } to { transform: translate(30px,20px) scale(1.06); } }
      @keyframes drift2 { from { transform: translate(0,0) scale(1); } to { transform: translate(-25px,-15px) scale(1.04); } }
    `}</style>
  </div>
);

/* ─── Platform badge ─── */
const PlatformBadge = ({ platform }: { platform: string }) => {
  const map: Record<string, { label: string; color: string; bg: string }> = {
    GITHUB: { label: 'GitHub Actions', color: '#a5b4fc', bg: 'rgba(99,102,241,0.12)' },
    GITLAB: { label: 'GitLab CI/CD', color: '#fb923c', bg: 'rgba(249,115,22,0.12)' },
    JENKINS: { label: 'Jenkins CI', color: '#fbbf24', bg: 'rgba(251,191,36,0.12)' },
  };
  const s = map[platform] ?? { label: platform, color: '#94a3b8', bg: 'rgba(148,163,184,0.1)' };
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: 5, fontSize: 11,
      fontWeight: 600, letterSpacing: '0.04em', textTransform: 'uppercase',
      padding: '3px 10px', borderRadius: 20,
      background: s.bg, color: s.color,
      border: `1px solid ${s.color}30`,
    }}>
      <span style={{
        width: 6, height: 6, borderRadius: '50%', background: s.color,
        boxShadow: `0 0 6px ${s.color}`,
      }} />
      {s.label}
    </span>
  );
};

/* ─── Section label ─── */
const SectionLabel = ({ children }: { children: React.ReactNode }) => (
  <span style={{
    display: 'block', fontSize: 10, fontWeight: 700, letterSpacing: '0.1em',
    textTransform: 'uppercase', color: 'rgba(148,163,184,0.6)', marginBottom: 8,
  }}>
    {children}
  </span>
);

/* ─── Styled input ─── */
const FieldInput = (props: React.InputHTMLAttributes<HTMLInputElement>) => (
  <input
    {...props}
    style={{
      width: '100%', boxSizing: 'border-box',
      padding: '10px 14px', borderRadius: 10,
      border: '1px solid rgba(148,163,184,0.12)',
      background: 'rgba(15,23,42,0.6)',
      color: '#e2e8f0', fontSize: 13,
      outline: 'none', transition: 'border-color 0.2s',
      fontFamily: props.className?.includes('mono') ? 'ui-monospace, monospace' : 'inherit',
      ...props.style,
    }}
    onFocus={e => (e.target.style.borderColor = 'rgba(99,102,241,0.5)')}
    onBlur={e => (e.target.style.borderColor = 'rgba(148,163,184,0.12)')}
  />
);

export default function AnalyzePage() {
  const { analyze, result, loading } = useAnalyze();
  const [projectName, setProjectName] = useState('main-app-pipeline');
  const [ciPlatform, setCiPlatform] = useState<CiPlatform>('GITHUB');
  const [logContent, setLogContent] = useState('');
  const [branchName, setBranchName] = useState('');
  const [commitSha, setCommitSha] = useState('');
  const [copiedAction, setCopiedAction] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!projectName.trim() || !logContent.trim()) return;
    analyze({
      projectName, ciPlatform, logContent,
      branchName: branchName || undefined,
      commitSha: commitSha || undefined
    });
  };

  const copySuggestedAction = () => {
    if (!result?.suggestedAction) return;
    navigator.clipboard.writeText(result.suggestedAction).then(() => {
      setCopiedAction(true);
      setTimeout(() => setCopiedAction(false), 2000);
    });
  };

  const getFailureIcon = (type: string) => {
    const sz = 'w-5 h-5';
    switch (type) {
      case 'infra': return <HardDrive className={`${sz} text-amber-400`} />;
      case 'test': return <TestTube className={`${sz} text-violet-400`} />;
      case 'build': return <Hammer className={`${sz} text-sky-400`} />;
      case 'security': return <Shield className={`${sz} text-rose-400`} />;
      default: return <HelpCircle className={`${sz} text-slate-400`} />;
    }
  };

  const getFailureAccent = (type: string) => {
    switch (type) {
      case 'infra': return { color: '#fbbf24', bg: 'rgba(251,191,36,0.1)', border: 'rgba(251,191,36,0.2)' };
      case 'test': return { color: '#a78bfa', bg: 'rgba(167,139,250,0.1)', border: 'rgba(167,139,250,0.2)' };
      case 'build': return { color: '#38bdf8', bg: 'rgba(56,189,248,0.1)', border: 'rgba(56,189,248,0.2)' };
      case 'security': return { color: '#fb7185', bg: 'rgba(251,113,133,0.1)', border: 'rgba(251,113,133,0.2)' };
      default: return { color: '#94a3b8', bg: 'rgba(148,163,184,0.08)', border: 'rgba(148,163,184,0.15)' };
    }
  };

  const getFailureLabel = (type: string) => {
    switch (type) {
      case 'infra': return 'Infrastructure Failure';
      case 'test': return 'Test Automation Failure';
      case 'build': return 'Compilation / Build Error';
      case 'security': return 'Security Vulnerability';
      default: return 'Unknown Category';
    }
  };

  const getClassifierLabel = (mode: string) => {
    switch (mode) {
      case 'rule_based': return 'Heuristic / Rule Matcher';
      case 'ml_onnx': return 'Deep ONNX Neural Network';
      case 'hybrid': return 'Hybrid Intelligence';
      default: return mode;
    }
  };

  /* ── shared card styles ── */
  const card = {
    background: 'rgba(15,23,42,0.55)',
    border: '1px solid rgba(148,163,184,0.1)',
    borderRadius: 20,
    backdropFilter: 'blur(16px)',
    WebkitBackdropFilter: 'blur(16px)',
  } as React.CSSProperties;

  return (
    <div style={{ position: 'relative', maxWidth: 1100, margin: '0 auto', paddingBottom: 64 }}>
      <MeshBackground />

      {/* ── PAGE HEADER ── */}
      <motion.div
        initial={{ opacity: 0, y: -12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        style={{ ...card, padding: '28px 32px', marginBottom: 24, position: 'relative', overflow: 'hidden' }}
      >
        {/* Decorative rule-line */}
        <div style={{
          position: 'absolute', top: 0, left: 32, right: 32, height: 2,
          background: 'linear-gradient(90deg, transparent, rgba(99,102,241,0.6), rgba(20,184,166,0.5), transparent)',
          borderRadius: 2,
        }} />

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 16 }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 6 }}>
              <div style={{
                width: 36, height: 36, borderRadius: 10,
                background: 'linear-gradient(135deg, rgba(99,102,241,0.25), rgba(20,184,166,0.2))',
                border: '1px solid rgba(99,102,241,0.3)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>
                <Activity size={16} color="#818cf8" />
              </div>
              <h1 style={{
                margin: 0, fontSize: 26, fontWeight: 800, letterSpacing: '-0.03em',
                background: 'linear-gradient(120deg, #c7d2fe 0%, #818cf8 40%, #34d399 100%)',
                WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
              }}>
                Raw Log Classification
              </h1>
            </div>
            <p style={{ margin: 0, fontSize: 13, color: 'rgba(148,163,184,0.75)', lineHeight: 1.5 }}>
              Run heuristic rules and neural classification models on arbitrary CI log streams.
            </p>
          </div>

          <div style={{ display: 'flex', gap: 10 }}>
            {['Rule Engine', 'ONNX Model', 'Hybrid AI'].map(label => (
              <span key={label} style={{
                fontSize: 11, fontWeight: 600, padding: '4px 12px', borderRadius: 20,
                background: 'rgba(99,102,241,0.08)',
                border: '1px solid rgba(99,102,241,0.15)',
                color: 'rgba(165,180,252,0.8)',
                letterSpacing: '0.03em',
              }}>
                {label}
              </span>
            ))}
          </div>
        </div>
      </motion.div>

      <AnimatePresence mode="wait">

        {/* ── INPUT FORM ── */}
        {!result && !loading && (
          <motion.form
            key="submit-form"
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -16 }}
            transition={{ duration: 0.35 }}
            onSubmit={handleSubmit}
            style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: 20 }}
          >
            {/* Left: Metadata panel */}
            <div style={{ ...card, padding: 24, display: 'flex', flexDirection: 'column', gap: 20 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <Info size={14} color="#818cf8" />
                <span style={{ fontSize: 13, fontWeight: 700, color: '#e2e8f0', letterSpacing: '-0.01em' }}>
                  Pipeline Metadata
                </span>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                <SectionLabel>Project Name</SectionLabel>
                <FieldInput
                  type="text" required
                  value={projectName}
                  onChange={e => setProjectName(e.target.value)}
                  placeholder="e.g. core-auth-service"
                />
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                <SectionLabel>CI/CD Platform</SectionLabel>
                <select
                  value={ciPlatform}
                  onChange={e => setCiPlatform(e.target.value as CiPlatform)}
                  style={{
                    width: '100%', padding: '10px 14px', borderRadius: 10,
                    border: '1px solid rgba(148,163,184,0.12)',
                    background: 'rgba(15,23,42,0.6)', color: '#e2e8f0',
                    fontSize: 13, outline: 'none', appearance: 'none',
                    cursor: 'pointer',
                  }}
                >
                  <option value="GITHUB">GitHub Actions</option>
                  <option value="GITLAB">GitLab CI/CD</option>
                  <option value="JENKINS">Jenkins CI</option>
                </select>
              </div>

              {/* Divider */}
              <div style={{ borderTop: '1px solid rgba(148,163,184,0.08)', margin: '0 -4px' }} />

              <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                <SectionLabel>Branch Name <span style={{ opacity: 0.5 }}>(Optional)</span></SectionLabel>
                <FieldInput
                  type="text" value={branchName}
                  onChange={e => setBranchName(e.target.value)}
                  placeholder="e.g. main"
                />
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                <SectionLabel>Commit SHA <span style={{ opacity: 0.5 }}>(Optional)</span></SectionLabel>
                <FieldInput
                  type="text" value={commitSha}
                  onChange={e => setCommitSha(e.target.value)}
                  placeholder="e.g. a1b2c3d4..."
                  className="mono"
                />
              </div>

              {/* Metadata summary pill */}
              {(branchName || commitSha) && (
                <motion.div
                  initial={{ opacity: 0, scale: 0.96 }}
                  animate={{ opacity: 1, scale: 1 }}
                  style={{
                    padding: '10px 14px', borderRadius: 12,
                    background: 'rgba(99,102,241,0.07)',
                    border: '1px solid rgba(99,102,241,0.15)',
                  }}
                >
                  <p style={{ margin: 0, fontSize: 11, color: 'rgba(165,180,252,0.7)', lineHeight: 1.8 }}>
                    {branchName && <><span style={{ opacity: 0.6 }}>branch:</span> <strong style={{ color: '#a5b4fc' }}>{branchName}</strong><br /></>}
                    {commitSha && <><span style={{ opacity: 0.6 }}>sha:</span> <code style={{ fontSize: 10, fontFamily: 'monospace', color: '#a5b4fc' }}>{commitSha.slice(0, 12)}…</code></>}
                  </p>
                </motion.div>
              )}
            </div>

            {/* Right: Log input panel */}
            <div style={{ ...card, padding: 24, display: 'flex', flexDirection: 'column', gap: 16 }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <Terminal size={14} color="#34d399" />
                  <span style={{ fontSize: 13, fontWeight: 700, color: '#e2e8f0' }}>
                    Raw Execution Log
                  </span>
                </div>
                <PlatformBadge platform={ciPlatform} />
              </div>

              {/* Log textarea */}
              <div style={{ flex: 1, position: 'relative' }}>
                {/* Faux terminal chrome */}
                <div style={{
                  display: 'flex', alignItems: 'center', gap: 6,
                  padding: '8px 14px',
                  background: 'rgba(0,0,0,0.4)',
                  borderRadius: '12px 12px 0 0',
                  border: '1px solid rgba(148,163,184,0.1)',
                  borderBottom: 'none',
                }}>
                  {['#ff5f57', '#febc2e', '#28c840'].map(c => (
                    <span key={c} style={{ width: 10, height: 10, borderRadius: '50%', background: c, opacity: 0.8 }} />
                  ))}
                  <span style={{ marginLeft: 8, fontSize: 11, color: 'rgba(148,163,184,0.4)', fontFamily: 'monospace' }}>
                    stdin / log stream
                  </span>
                </div>

                <textarea
                  required
                  value={logContent}
                  onChange={e => setLogContent(e.target.value)}
                  placeholder={'Paste build output, container logs, stack traces,\ncompiler errors, test failures...'}
                  style={{
                    width: '100%', minHeight: 340, boxSizing: 'border-box',
                    padding: '16px 16px 36px',
                    borderRadius: '0 0 12px 12px',
                    border: '1px solid rgba(148,163,184,0.1)',
                    background: 'rgba(0,0,0,0.5)',
                    color: '#94a3b8', fontSize: 12,
                    fontFamily: 'ui-monospace, "Cascadia Code", monospace',
                    lineHeight: 1.7, resize: 'none', outline: 'none',
                    display: 'block',
                  }}
                  onFocus={e => (e.target.style.borderColor = 'rgba(99,102,241,0.4)')}
                  onBlur={e => (e.target.style.borderColor = 'rgba(148,163,184,0.1)')}
                />
                <span style={{
                  position: 'absolute', bottom: 10, right: 12,
                  fontSize: 10, color: 'rgba(148,163,184,0.35)',
                  fontFamily: 'monospace',
                  background: 'rgba(15,23,42,0.7)',
                  padding: '2px 8px', borderRadius: 6,
                  border: '1px solid rgba(148,163,184,0.08)',
                }}>
                  {(logContent.length / 1024).toFixed(1)} KB / 1024 KB
                </span>
              </div>

              {/* Submit row */}
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <p style={{ margin: 0, fontSize: 12, color: 'rgba(148,163,184,0.4)' }}>
                  Supports up to 1 MB of raw log content
                </p>
                <motion.button
                  type="submit"
                  disabled={!logContent.trim() || !projectName.trim()}
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.97 }}
                  style={{
                    display: 'flex', alignItems: 'center', gap: 8,
                    padding: '11px 22px', borderRadius: 12, cursor: 'pointer',
                    background: 'linear-gradient(135deg, #6366f1 0%, #8b5cf6 50%, #6366f1 100%)',
                    backgroundSize: '200% 200%',
                    border: '1px solid rgba(99,102,241,0.5)',
                    color: '#fff', fontSize: 13, fontWeight: 700,
                    letterSpacing: '-0.01em',
                    boxShadow: '0 0 24px rgba(99,102,241,0.25), inset 0 1px 0 rgba(255,255,255,0.1)',
                    transition: 'all 0.2s',
                  }}
                >
                  <Sparkles size={15} />
                  Analyze Failure
                  <ChevronRight size={14} style={{ opacity: 0.7 }} />
                </motion.button>
              </div>
            </div>
          </motion.form>
        )}

        {/* ── LOADING ── */}
        {loading && (
          <motion.div
            key="loading-screen"
            initial={{ opacity: 0, scale: 0.97 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.97 }}
            transition={{ duration: 0.3 }}
            style={{
              ...card, padding: '80px 40px',
              display: 'flex', flexDirection: 'column', alignItems: 'center',
              justifyContent: 'center', gap: 24,
            }}
          >
            {/* Concentric pulsing rings */}
            <div style={{ position: 'relative', width: 90, height: 90, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              {[0, 1, 2].map(i => (
                <div
                  key={i}
                  style={{
                    position: 'absolute',
                    width: 90 - i * 20, height: 90 - i * 20,
                    borderRadius: '50%',
                    border: `${1.5 - i * 0.3}px solid rgba(99,102,241,${0.35 - i * 0.08})`,
                    animation: `ping${i} ${2 + i * 0.4}s ease-out infinite`,
                  }}
                />
              ))}
              <div style={{
                width: 48, height: 48, borderRadius: '50%',
                border: '2.5px solid rgba(99,102,241,0.15)',
                borderTopColor: '#818cf8',
                animation: 'spin 0.9s linear infinite',
              }} />
              <Cpu
                size={18}
                color="#818cf8"
                style={{ position: 'absolute', animation: 'pulse 1.8s ease-in-out infinite' }}
              />
            </div>

            <div style={{ textAlign: 'center', maxWidth: 360 }}>
              <h3 style={{ margin: '0 0 8px', fontSize: 17, fontWeight: 700, color: '#e2e8f0', letterSpacing: '-0.02em' }}>
                Analyzing Log Stream…
              </h3>
              <p style={{ margin: 0, fontSize: 13, color: 'rgba(148,163,184,0.6)', lineHeight: 1.6 }}>
                Parsing outputs, vectorizing text features, running deep classifier models and matching heuristic patterns.
              </p>
            </div>

            {/* Pipeline steps */}
            {['Tokenizing log entries', 'Running heuristic rules', 'Inference via ONNX model', 'Synthesizing results'].map((step, i) => (
              <motion.div
                key={step}
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: i * 0.55 }}
                style={{
                  display: 'flex', alignItems: 'center', gap: 10,
                  fontSize: 12, color: 'rgba(148,163,184,0.6)',
                }}
              >
                <Zap size={12} color="#818cf8" style={{ opacity: 0.8 }} />
                {step}
              </motion.div>
            ))}

            <style>{`
              @keyframes spin { to { transform: rotate(360deg); } }
              @keyframes pulse { 0%,100% { opacity:.6 } 50% { opacity:1 } }
              @keyframes ping0 { 0% { transform:scale(1); opacity:.5 } 100% { transform:scale(1.4); opacity:0 } }
              @keyframes ping1 { 0% { transform:scale(1); opacity:.4 } 100% { transform:scale(1.35); opacity:0 } }
              @keyframes ping2 { 0% { transform:scale(1); opacity:.3 } 100% { transform:scale(1.3); opacity:0 } }
            `}</style>
          </motion.div>
        )}

        {/* ── RESULTS ── */}
        {result && (
          <motion.div
            key="analysis-result"
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -16 }}
            transition={{ duration: 0.4 }}
            style={{ display: 'flex', flexDirection: 'column', gap: 20 }}
          >
            {/* ── Row 1: Classification summary + Confidence ── */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 260px', gap: 20 }}>

              {/* Classification summary */}
              {(() => {
                const accent = getFailureAccent(result.failureType);
                return (
                  <div style={{
                    ...card, padding: 28,
                    borderColor: accent.border,
                    display: 'flex', flexDirection: 'column', justifyContent: 'space-between',
                  }}>
                    {/* Top strip */}
                    <div style={{
                      height: 3, borderRadius: 2,
                      background: `linear-gradient(90deg, ${accent.color}80, ${accent.color}20)`,
                      marginBottom: 20, marginTop: -4, marginLeft: -4, marginRight: -4,
                    }} />

                    <div style={{ display: 'flex', alignItems: 'flex-start', gap: 16 }}>
                      {/* Icon */}
                      <div style={{
                        width: 52, height: 52, borderRadius: 14, flexShrink: 0,
                        background: accent.bg,
                        border: `1px solid ${accent.border}`,
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                      }}>
                        {getFailureIcon(result.failureType)}
                      </div>

                      <div style={{ flex: 1 }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
                          <span style={{
                            fontSize: 10, fontWeight: 700, letterSpacing: '0.08em',
                            textTransform: 'uppercase', color: accent.color, opacity: 0.8,
                          }}>
                            Detected Failure
                          </span>
                        </div>
                        <h2 style={{
                          margin: '0 0 8px', fontSize: 20, fontWeight: 800,
                          letterSpacing: '-0.025em', color: '#f1f5f9',
                        }}>
                          {getFailureLabel(result.failureType)}
                        </h2>
                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                          <span style={{
                            fontSize: 11, fontWeight: 600,
                            padding: '3px 10px', borderRadius: 20,
                            background: 'rgba(99,102,241,0.1)',
                            border: '1px solid rgba(99,102,241,0.2)',
                            color: '#a5b4fc',
                          }}>
                            {getClassifierLabel(result.classifierMode)}
                          </span>
                          {result.matchedPatternName && (
                            <span style={{
                              fontSize: 11, fontWeight: 600, fontFamily: 'monospace',
                              padding: '3px 10px', borderRadius: 20,
                              background: 'rgba(236,72,153,0.1)',
                              border: '1px solid rgba(236,72,153,0.2)',
                              color: '#f9a8d4',
                            }}>
                              {result.matchedPatternName}
                            </span>
                          )}
                        </div>
                      </div>
                    </div>

                    {/* Footer row */}
                    <div style={{
                      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                      borderTop: '1px solid rgba(148,163,184,0.08)', paddingTop: 16, marginTop: 20,
                    }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                        <motion.button
                          type="button"
                          onClick={() => handleSubmit({ preventDefault: () => { } } as React.FormEvent)}
                          whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}
                          style={{
                            display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer',
                            padding: '6px 12px', borderRadius: 8,
                            background: 'rgba(148,163,184,0.06)',
                            border: '1px solid rgba(148,163,184,0.12)',
                            color: 'rgba(148,163,184,0.7)', fontSize: 12, fontWeight: 600,
                          }}
                        >
                          <RefreshCw size={12} />
                          Re-analyze
                        </motion.button>
                        <span style={{ fontSize: 12, color: 'rgba(148,163,184,0.35)', fontFamily: 'monospace' }}>
                          {new Date(result.analyzedAt).toLocaleString()}
                        </span>
                      </div>

                      <motion.button
                        type="button"
                        whileHover={{ x: -2 }}
                        onClick={() => window.location.reload()}
                        style={{
                          display: 'flex', alignItems: 'center', gap: 6,
                          background: 'none', border: 'none', cursor: 'pointer',
                          fontSize: 12, fontWeight: 700,
                          color: 'rgba(99,102,241,0.8)',
                        }}
                      >
                        <ArrowLeft size={13} />
                        Analyze Another Log
                      </motion.button>
                    </div>
                  </div>
                );
              })()}

              {/* Confidence score card */}
              <div style={{
                ...card, padding: 28,
                display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
                gap: 4, textAlign: 'center',
              }}>
                <SectionLabel>Confidence Score</SectionLabel>

                {/* SVG radial gauge */}
                <div style={{ position: 'relative', width: 120, height: 120, margin: '8px 0' }}>
                  <svg viewBox="0 0 120 120" style={{ width: '100%', height: '100%', transform: 'rotate(-90deg)' }}>
                    {/* Track */}
                    <circle cx="60" cy="60" r="48" fill="none"
                      stroke="rgba(148,163,184,0.1)" strokeWidth="7" />
                    {/* Glow ring */}
                    <circle cx="60" cy="60" r="48" fill="none"
                      stroke="rgba(99,102,241,0.08)" strokeWidth="14" />
                    {/* Progress arc */}
                    <circle cx="60" cy="60" r="48" fill="none"
                      stroke="url(#confGrad)"
                      strokeWidth="7"
                      strokeLinecap="round"
                      strokeDasharray={2 * Math.PI * 48}
                      strokeDashoffset={2 * Math.PI * 48 * (1 - result.confidence)}
                      style={{ transition: 'stroke-dashoffset 1.2s cubic-bezier(.4,0,.2,1)' }}
                    />
                    <defs>
                      <linearGradient id="confGrad" x1="0%" y1="0%" x2="100%" y2="0%">
                        <stop offset="0%" stopColor="#818cf8" />
                        <stop offset="100%" stopColor="#34d399" />
                      </linearGradient>
                    </defs>
                  </svg>
                  <div style={{
                    position: 'absolute', inset: 0,
                    display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
                  }}>
                    <span style={{ fontSize: 26, fontWeight: 900, color: '#f1f5f9', letterSpacing: '-0.04em', lineHeight: 1 }}>
                      {Math.round(result.confidence * 100)}
                    </span>
                    <span style={{ fontSize: 12, color: 'rgba(148,163,184,0.5)', fontWeight: 600 }}>%</span>
                  </div>
                </div>

                <p style={{ margin: 0, fontSize: 11, color: 'rgba(148,163,184,0.5)', lineHeight: 1.6, maxWidth: 180 }}>
                  Probability index calculated by classifier engines based on log heuristics.
                </p>

                {/* Mini progress bar */}
                <div style={{
                  width: '100%', height: 4, borderRadius: 4,
                  background: 'rgba(148,163,184,0.1)', marginTop: 8, overflow: 'hidden',
                }}>
                  <motion.div
                    initial={{ width: 0 }}
                    animate={{ width: `${result.confidence * 100}%` }}
                    transition={{ duration: 1.2, ease: [0.4, 0, 0.2, 1] }}
                    style={{
                      height: '100%', borderRadius: 4,
                      background: 'linear-gradient(90deg, #6366f1, #34d399)',
                    }}
                  />
                </div>
              </div>
            </div>

            {/* ── Row 2: Explanation + Action ── */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>

              {/* Root-cause explanation */}
              <div style={{ ...card, padding: 28 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 16 }}>
                  <div style={{
                    width: 24, height: 24, borderRadius: 6,
                    background: 'rgba(167,139,250,0.15)',
                    border: '1px solid rgba(167,139,250,0.2)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                  }}>
                    <AlertTriangle size={12} color="#a78bfa" />
                  </div>
                  <span style={{ fontSize: 13, fontWeight: 700, color: '#e2e8f0' }}>
                    Root-Cause Explanation
                  </span>
                </div>

                <div style={{
                  padding: '14px 16px', borderRadius: 12,
                  background: 'rgba(0,0,0,0.25)',
                  border: '1px solid rgba(148,163,184,0.07)',
                  borderLeft: '3px solid rgba(167,139,250,0.5)',
                }}>
                  <p style={{
                    margin: 0, fontSize: 13, color: '#94a3b8',
                    lineHeight: 1.75, whiteSpace: 'pre-line',
                  }}>
                    {result.explanation || 'No structured explanation returned. Review build log for details.'}
                  </p>
                </div>
              </div>

              {/* Suggested resolution */}
              <div style={{ ...card, padding: 28, display: 'flex', flexDirection: 'column' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 16 }}>
                  <div style={{
                    width: 24, height: 24, borderRadius: 6,
                    background: 'rgba(52,211,153,0.12)',
                    border: '1px solid rgba(52,211,153,0.2)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                  }}>
                    <Zap size={12} color="#34d399" />
                  </div>
                  <span style={{ fontSize: 13, fontWeight: 700, color: '#e2e8f0' }}>
                    Suggested Resolution
                  </span>
                </div>

                {/* Code block */}
                <div style={{
                  flex: 1, position: 'relative',
                  background: 'rgba(0,0,0,0.45)',
                  border: '1px solid rgba(52,211,153,0.12)',
                  borderRadius: 12, overflow: 'hidden',
                }}>
                  {/* Dot chrome */}
                  <div style={{
                    display: 'flex', alignItems: 'center', gap: 5,
                    padding: '8px 12px',
                    background: 'rgba(0,0,0,0.3)',
                    borderBottom: '1px solid rgba(148,163,184,0.07)',
                  }}>
                    {['#ff5f57', '#febc2e', '#28c840'].map(c => (
                      <span key={c} style={{ width: 8, height: 8, borderRadius: '50%', background: c, opacity: 0.6 }} />
                    ))}
                    <span style={{ marginLeft: 8, fontSize: 10, fontFamily: 'monospace', color: 'rgba(148,163,184,0.3)' }}>
                      resolution.sh
                    </span>
                  </div>
                  <pre style={{
                    margin: 0, padding: '14px 16px',
                    fontFamily: 'ui-monospace, "Cascadia Code", monospace',
                    fontSize: 12, color: '#86efac', lineHeight: 1.7,
                    whiteSpace: 'pre-wrap', wordBreak: 'break-all',
                    userSelect: 'all',
                  }}>
                    {result.suggestedAction || '# No suggested action.\n# Verify dependencies and configurations.'}
                  </pre>
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 12 }}>
                  <motion.button
                    type="button"
                    onClick={copySuggestedAction}
                    whileHover={{ scale: 1.04 }} whileTap={{ scale: 0.96 }}
                    style={{
                      display: 'flex', alignItems: 'center', gap: 6,
                      padding: '8px 16px', borderRadius: 9, cursor: 'pointer',
                      background: copiedAction ? 'rgba(52,211,153,0.1)' : 'rgba(148,163,184,0.07)',
                      border: `1px solid ${copiedAction ? 'rgba(52,211,153,0.3)' : 'rgba(148,163,184,0.12)'}`,
                      color: copiedAction ? '#34d399' : '#94a3b8',
                      fontSize: 12, fontWeight: 600,
                      transition: 'all 0.2s',
                    }}
                  >
                    {copiedAction
                      ? <><Check size={13} /> Copied!</>
                      : <><Copy size={13} /> Copy Resolution</>}
                  </motion.button>
                </div>
              </div>
            </div>

            {/* ── Row 3: Log viewer ── */}
            <div style={{ ...card, padding: 28 }}>
              <LogViewer
                content={logContent}
                maxLines={300}
                title={`Submitted Log — ${projectName}`}
              />
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}