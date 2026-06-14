import { useState, useMemo, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Search, X, Copy, Check, ChevronDown, ChevronUp } from 'lucide-react';

interface Props {
  content: string;
  maxLines?: number;
  title?: string;
}

type LineClass = 'error' | 'warning' | 'success' | 'info' | 'highlight' | '';

function classifyLine(line: string): LineClass {
  const l = line.toLowerCase();
  if (/\b(error|exception|fatal|failed|failure|critical|crash|aborted|killed)\b/.test(l)) return 'error';
  if (/\b(warn|warning|deprecated|todo|fixme)\b/.test(l)) return 'warning';
  if (/\b(success|passed|ok|done|complete|built|deployed)\b/.test(l)) return 'success';
  if (/\b(info|debug|trace|starting|running|downloading)\b/.test(l)) return 'info';
  return '';
}

export default function LogViewer({ content, maxLines = 200, title = 'Log Output' }: Props) {
  const [query, setQuery]       = useState('');
  const [copied, setCopied]     = useState(false);
  const [collapsed, setCollapsed] = useState(false);
  const bodyRef = useRef<HTMLDivElement>(null);

  const lines = useMemo(() => content.split('\n').slice(0, maxLines), [content, maxLines]);

  const filteredLines = useMemo(() => {
    if (!query) return lines.map((text, i) => ({ text, idx: i, cls: classifyLine(text) }));
    const q = query.toLowerCase();
    return lines
      .map((text, idx) => ({ text, idx, cls: classifyLine(text) }))
      .filter(({ text }) => text.toLowerCase().includes(q));
  }, [lines, query]);

  const copyToClipboard = () => {
    navigator.clipboard.writeText(content).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    });
  };

  // Auto-scroll to bottom
  useEffect(() => {
    if (bodyRef.current && !collapsed && !query) {
      bodyRef.current.scrollTop = bodyRef.current.scrollHeight;
    }
  }, [content, collapsed, query]);

  const errorCount   = lines.filter((l) => classifyLine(l) === 'error').length;
  const warningCount = lines.filter((l) => classifyLine(l) === 'warning').length;

  return (
    <div className="log-viewer">
      <div className="log-viewer-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--sp-3)' }}>
          {/* Terminal dots */}
          <div style={{ display: 'flex', gap: 6 }}>
            {['#ef4444', '#f59e0b', '#10b981'].map((c) => (
              <div key={c} style={{ width: 10, height: 10, borderRadius: '50%', background: c }} />
            ))}
          </div>
          <span style={{ fontFamily: 'var(--font-mono)', fontSize: '0.78rem', color: 'var(--text-muted)' }}>
            {title}
          </span>
          <div style={{ display: 'flex', gap: 6 }}>
            {errorCount > 0 && (
              <span className="badge badge-security">{errorCount} errors</span>
            )}
            {warningCount > 0 && (
              <span className="badge badge-warning">{warningCount} warnings</span>
            )}
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--sp-2)' }}>
          {/* Search */}
          <div style={{ position: 'relative' }}>
            <Search size={13} style={{ position: 'absolute', left: 8, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)', pointerEvents: 'none' }} />
            <input
              className="form-control log-search"
              placeholder="Filter logs…"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              style={{ paddingLeft: 28, height: 30, fontSize: '0.78rem', width: 180 }}
            />
            {query && (
              <button onClick={() => setQuery('')} style={{ position: 'absolute', right: 8, top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-muted)', display: 'flex' }}>
                <X size={13} />
              </button>
            )}
          </div>

          <button className="btn btn-ghost" style={{ padding: '4px 8px' }} onClick={copyToClipboard} title="Copy log">
            {copied ? <Check size={14} color="var(--green)" /> : <Copy size={14} />}
          </button>

          <button className="btn btn-ghost" style={{ padding: '4px 8px' }} onClick={() => setCollapsed(!collapsed)}>
            {collapsed ? <ChevronDown size={14} /> : <ChevronUp size={14} />}
          </button>
        </div>
      </div>

      <AnimatePresence initial={false}>
        {!collapsed && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.25, ease: 'easeInOut' }}
          >
            <div className="log-viewer-body" ref={bodyRef}>
              {filteredLines.length === 0 ? (
                <div style={{ color: 'var(--text-muted)', fontStyle: 'italic' }}>
                  No lines match "{query}"
                </div>
              ) : (
                filteredLines.map(({ text, idx, cls }) => {
                  const highlighted = query && text.toLowerCase().includes(query.toLowerCase());
                  return (
                    <div key={idx} className={`log-line ${cls} ${highlighted ? 'highlight' : ''}`}>
                      <span className="log-line-num">{idx + 1}</span>
                      <span className="log-line-content">
                        {query && highlighted ? highlightMatch(text, query) : text || ' '}
                      </span>
                    </div>
                  );
                })
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

function highlightMatch(text: string, query: string) {
  const idx = text.toLowerCase().indexOf(query.toLowerCase());
  if (idx < 0) return text;
  return (
    <>
      {text.slice(0, idx)}
      <mark style={{ background: 'rgba(99,102,241,0.3)', color: 'var(--accent)', borderRadius: 2 }}>
        {text.slice(idx, idx + query.length)}
      </mark>
      {text.slice(idx + query.length)}
    </>
  );
}
