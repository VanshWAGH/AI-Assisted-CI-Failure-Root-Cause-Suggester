// ═══════════════════════════════════════════════════════
// Shared TypeScript types matching the Spring Boot DTOs
// ═══════════════════════════════════════════════════════

export type FailureType = 'infra' | 'test' | 'build' | 'security' | 'unknown';
export type ClassifierMode = 'rule_based' | 'ml_onnx' | 'hybrid';
export type CiPlatform = 'GITLAB' | 'JENKINS' | 'GITHUB';

// ── Dashboard DTOs ────────────────────────────────────
export interface DashboardStats {
  totalFailuresToday: number;
  infraFailures: number;
  testFailures: number;
  buildFailures: number;
  securityFailures: number;
  unknownFailures: number;
  avgConfidence: number;
  lastUpdated: string;
}

export interface TrendDay {
  date: string;
  infra: number;
  test: number;
  build: number;
  security: number;
  unknown: number;
}

export interface RecentAnalysis {
  analysisId: string;
  jobId: string;
  externalJobId: string;
  projectName: string;
  branchName?: string;
  failureType: FailureType;
  confidence: number;
  classifierMode: ClassifierMode;
  matchedPatternName?: string;
  explanation?: string;
  suggestedAction?: string;
  analyzedAt: string;
}

// ── Analysis Request / Response ───────────────────────
export interface AnalyzeRawRequest {
  projectName: string;
  ciPlatform: CiPlatform;
  logContent?: string;
  logLines?: string[];
  pipelineRef?: string;
  branchName?: string;
  commitSha?: string;
}

export interface AnalysisResponse {
  analysisId: string;
  jobId: string;
  externalJobId?: string;
  projectName?: string;
  failureType: FailureType;
  confidence: number;
  explanation: string;
  suggestedAction: string;
  classifierMode: ClassifierMode;
  matchedPatternName?: string;
  analyzedAt: string;
}

// ── History (paginated) ───────────────────────────────
export interface AnalysisHistoryResponse {
  analysisId: string;
  jobId: string;
  externalJobId: string;
  projectName: string;
  branchName?: string;
  failureType: FailureType;
  confidence: number;
  classifierMode: ClassifierMode;
  matchedPatternName?: string;
  analyzedAt: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  last: boolean;
  first: boolean;
}

// ── Pattern catalogue ─────────────────────────────────
export interface FailurePattern {
  id: string;
  name: string;
  failureType: FailureType;
  regexPattern: string;
  explanationTemplate: string;
  suggestedActionTemplate: string;
  priority: number;
  active: boolean;
  createdAt: string;
}

export interface PatternStats {
  total: number;
  active: number;
  infra?: number;
  test?: number;
  build?: number;
  security?: number;
  unknown?: number;
}

// ── Colour mapping helpers ────────────────────────────
export const FAILURE_COLORS: Record<FailureType, string> = {
  infra:    '#f97316',
  test:     '#a855f7',
  build:    '#3b82f6',
  security: '#ef4444',
  unknown:  '#6b7280',
};

export const FAILURE_LABELS: Record<FailureType, string> = {
  infra:    'Infrastructure',
  test:     'Test Failure',
  build:    'Build Error',
  security: 'Security',
  unknown:  'Unknown',
};

export const CI_PLATFORM_COLORS: Record<string, string> = {
  GITHUB:  '#e2e8f0',
  GITLAB:  '#fc6d26',
  JENKINS: '#d33833',
  OTHER:   '#6b7280',
};
