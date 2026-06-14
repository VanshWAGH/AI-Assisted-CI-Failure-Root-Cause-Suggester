import { useEffect, useState } from 'react';
import api from '../api/client';
import type {
  DashboardStats,
  TrendDay,
  RecentAnalysis,
  AnalyzeRawRequest,
  AnalysisResponse,
  AnalysisHistoryResponse,
  Page,
  FailurePattern,
  PatternStats,
} from '../types';

// ── useDashboardStats ─────────────────────────────────
export function useDashboardStats(pollIntervalMs = 30_000) {
  const [data, setData]       = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState<string | null>(null);
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  const refetch = async () => {
    setRefreshTrigger((prev) => prev + 1);
  };

  useEffect(() => {
    let mounted = true;

    const fetchStats = async () => {
      try {
        const res = await api.get<DashboardStats>('/dashboard/stats');
        if (mounted) { setData(res.data); setError(null); }
      } catch (err) {
        if (mounted) setError((err as Error).message);
      } finally {
        if (mounted) setLoading(false);
      }
    };

    fetchStats();
    const id = setInterval(fetchStats, pollIntervalMs);
    return () => { mounted = false; clearInterval(id); };
  }, [pollIntervalMs, refreshTrigger]);

  return { data, loading, error, refetch };
}

// ── useTrendData ──────────────────────────────────────
export function useTrendData(days = 7) {
  const [data, setData]       = useState<TrendDay[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState<string | null>(null);
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  const refetch = async () => {
    setRefreshTrigger((prev) => prev + 1);
  };

  useEffect(() => {
    let mounted = true;
    const fetchTrend = async () => {
      try {
        const res = await api.get<TrendDay[]>('/dashboard/trend', { params: { days } });
        if (mounted) { setData(res.data); setError(null); }
      } catch (err) {
        if (mounted) setError((err as Error).message);
      } finally {
        if (mounted) setLoading(false);
      }
    };
    fetchTrend();
    return () => { mounted = false; };
  }, [days, refreshTrigger]);

  return { data, loading, error, refetch };
}

// ── useRecentAnalyses ─────────────────────────────────
export function useRecentAnalyses(limit = 10, pollIntervalMs = 30_000) {
  const [data, setData]       = useState<RecentAnalysis[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState<string | null>(null);
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  const refetch = async () => {
    setRefreshTrigger((prev) => prev + 1);
  };

  useEffect(() => {
    let mounted = true;
    const fetchRecent = async () => {
      try {
        const res = await api.get<RecentAnalysis[]>('/dashboard/recent', { params: { limit } });
        if (mounted) { setData(res.data); setError(null); }
      } catch (err) {
        if (mounted) setError((err as Error).message);
      } finally {
        if (mounted) setLoading(false);
      }
    };
    fetchRecent();
    const id = setInterval(fetchRecent, pollIntervalMs);
    return () => { mounted = false; clearInterval(id); };
  }, [limit, pollIntervalMs, refreshTrigger]);

  return { data, loading, error, refetch };
}

// ── useAnalyze (raw log submission) ──────────────────
export function useAnalyze() {
  const [result, setResult]   = useState<AnalysisResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState<string | null>(null);

  const analyze = async (request: AnalyzeRawRequest) => {
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      const res = await api.post<AnalysisResponse>('/analyze/raw', request);
      setResult(res.data);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  return { analyze, result, loading, error };
}

// ── useHistory ────────────────────────────────────────
export function useHistory(page = 0, size = 20, type?: string) {
  const [data, setData]       = useState<Page<AnalysisHistoryResponse> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;
    const fetchHistory = async () => {
      setLoading(true);
      try {
        const params: Record<string, unknown> = { page, size };
        if (type) params.type = type;
        const res = await api.get<Page<AnalysisHistoryResponse>>('/history', { params });
        if (mounted) { setData(res.data); setError(null); }
      } catch (err) {
        if (mounted) setError((err as Error).message);
      } finally {
        if (mounted) setLoading(false);
      }
    };
    fetchHistory();
    return () => { mounted = false; };
  }, [page, size, type]);

  return { data, loading, error };
}

// ── useServiceStatus ──────────────────────────────────
export function useServiceStatus() {
  const [online, setOnline] = useState<boolean | null>(null);

  useEffect(() => {
    let mounted = true;
    const check = async () => {
      try {
        await api.get('/status');
        if (mounted) setOnline(true);
      } catch {
        if (mounted) setOnline(false);
      }
    };
    check();
    const id = setInterval(check, 15_000);
    return () => { mounted = false; clearInterval(id); };
  }, []);

  return online;
}

// ── usePatterns ───────────────────────────────────────
export function usePatterns(active?: boolean) {
  const [data, setData]       = useState<FailurePattern[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState<string | null>(null);
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  const refetch = async () => {
    setRefreshTrigger((prev) => prev + 1);
  };

  useEffect(() => {
    let mounted = true;
    const fetchPatterns = async () => {
      setLoading(true);
      try {
        const params: Record<string, unknown> = {};
        if (active !== undefined) params.active = active;
        const res = await api.get<FailurePattern[]>('/patterns', { params });
        if (mounted) { setData(res.data); setError(null); }
      } catch (err) {
        if (mounted) setError((err as Error).message);
      } finally {
        if (mounted) setLoading(false);
      }
    };
    fetchPatterns();
    return () => { mounted = false; };
  }, [active, refreshTrigger]);

  return { data, loading, error, refetch };
}

// ── usePatternStats ───────────────────────────────────
export function usePatternStats() {
  const [data, setData]       = useState<PatternStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState<string | null>(null);
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  const refetch = async () => {
    setRefreshTrigger((prev) => prev + 1);
  };

  useEffect(() => {
    let mounted = true;
    const fetchStats = async () => {
      setLoading(true);
      try {
        const res = await api.get<PatternStats>('/patterns/stats');
        if (mounted) { setData(res.data); setError(null); }
      } catch (err) {
        if (mounted) setError((err as Error).message);
      } finally {
        if (mounted) setLoading(false);
      }
    };
    fetchStats();
    return () => { mounted = false; };
  }, [refreshTrigger]);

  return { data, loading, error, refetch };
}
