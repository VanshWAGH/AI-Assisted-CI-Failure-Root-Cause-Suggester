#!/usr/bin/env pwsh
<#
.SYNOPSIS
    One-time deployment setup script for AI-Assisted CI Failure Root Cause Suggester.
    Sets GitHub Actions secrets, links Vercel project, and prints next steps.

.DESCRIPTION
    Run this ONCE after cloning the repo. It will:
      1. Prompt for your Neon DB connection string, Vercel credentials, Render hook URL
      2. Set all required GitHub Actions secrets via the `gh` CLI
      3. Link the frontend to Vercel (creates .vercel/project.json)
      4. Output a checklist of what's done and what's still manual

.REQUIREMENTS
    - GitHub CLI (gh):  winget install GitHub.cli  OR  https://cli.github.com
    - Vercel CLI:       npm i -g vercel
    - Node.js 20+

.EXAMPLE
    .\setup-deploy.ps1
#>

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# ─── Colors ───────────────────────────────────────────────────────────────────
function Write-Header  { param($msg) Write-Host "`n━━━ $msg ━━━" -ForegroundColor Cyan }
function Write-Success { param($msg) Write-Host "  ✅ $msg" -ForegroundColor Green }
function Write-Warn    { param($msg) Write-Host "  ⚠️  $msg" -ForegroundColor Yellow }
function Write-Step    { param($msg) Write-Host "  → $msg" -ForegroundColor White }

Write-Host ""
Write-Host "╔══════════════════════════════════════════════════════╗" -ForegroundColor Magenta
Write-Host "║   RootCause AI — Deployment Setup Wizard            ║" -ForegroundColor Magenta
Write-Host "╚══════════════════════════════════════════════════════╝" -ForegroundColor Magenta
Write-Host ""

# ─── Check prerequisites ──────────────────────────────────────────────────────
Write-Header "Checking Prerequisites"

$ghOk     = $null -ne (Get-Command gh     -ErrorAction SilentlyContinue)
$vercelOk = $null -ne (Get-Command vercel -ErrorAction SilentlyContinue)
$nodeOk   = $null -ne (Get-Command node   -ErrorAction SilentlyContinue)

if ($ghOk)     { Write-Success "GitHub CLI (gh) found" }
else           { Write-Warn "GitHub CLI not found — install: winget install GitHub.cli" }

if ($vercelOk) { Write-Success "Vercel CLI found" }
else {
    Write-Warn "Vercel CLI not found — installing globally..."
    npm install -g vercel --quiet
    $vercelOk = $true
}

if ($nodeOk)   { Write-Success "Node.js found" }
else           { Write-Warn "Node.js not found — install from https://nodejs.org" ; exit 1 }

# ─── Repo info ────────────────────────────────────────────────────────────────
Write-Header "Repository Info"
$repoRoot = Split-Path $PSScriptRoot -Parent
if (-not (Test-Path "$repoRoot\.git")) {
    # script is at repo root
    $repoRoot = $PSScriptRoot
}
$REPO_SLUG = "VanshWAGH/AI-Assisted-CI-Failure-Root-Cause-Suggester"
Write-Step "Repo: $REPO_SLUG"
Write-Step "Root: $repoRoot"

# ─── Gather secrets ───────────────────────────────────────────────────────────
Write-Header "Gathering Secrets (press Enter to skip any)"

Write-Host ""
Write-Host "  📌 NEON DATABASE URL" -ForegroundColor Yellow
Write-Host "     Get from: https://console.neon.tech → your project → Connection Details" -ForegroundColor Gray
Write-Host "     Format:   postgresql://user:pass@ep-xxx.neon.tech/neondb?sslmode=require" -ForegroundColor Gray
$NEON_URL = Read-Host "  Paste Neon connection string (postgresql://...)"

Write-Host ""
Write-Host "  📌 RENDER DEPLOY HOOK URL" -ForegroundColor Yellow
Write-Host "     Get from: Render Dashboard → your service → Settings → Deploy Hooks" -ForegroundColor Gray
$RENDER_HOOK = Read-Host "  Paste Render deploy hook URL (https://api.render.com/deploy/...)"

Write-Host ""
Write-Host "  📌 VERCEL TOKEN" -ForegroundColor Yellow
Write-Host "     Get from: https://vercel.com/account/tokens → New Token" -ForegroundColor Gray
$VERCEL_TOKEN = Read-Host "  Paste Vercel personal access token"

Write-Host ""
Write-Host "  📌 API KEY (pre-filled)" -ForegroundColor Yellow
$VITE_API_KEY  = "rca_92d6c92615c849759e0cc8523e8fc567"
$API_KEY_RENDER = $VITE_API_KEY
Write-Step "Using: $VITE_API_KEY"

# ─── Set GitHub secrets ───────────────────────────────────────────────────────
Write-Header "Setting GitHub Actions Secrets"

if (-not $ghOk) {
    Write-Warn "Skipping GitHub secrets — gh CLI not available"
    Write-Warn "Set these manually at: https://github.com/$REPO_SLUG/settings/secrets/actions"
} else {
    # Check auth
    $ghAuth = & gh auth status 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Step "Logging in to GitHub CLI..."
        & gh auth login
    } else {
        Write-Success "Already authenticated to GitHub"
    }

    function Set-GhSecret {
        param([string]$Name, [string]$Value, [string]$Description)
        if ([string]::IsNullOrWhiteSpace($Value)) {
            Write-Warn "Skipping $Name (empty value)"
            return
        }
        $Value | & gh secret set $Name --repo $REPO_SLUG 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) { Write-Success "Set $Name ($Description)" }
        else                     { Write-Warn "Failed to set $Name" }
    }

    Set-GhSecret "RENDER_DEPLOY_HOOK_URL" $RENDER_HOOK   "Render deploy trigger"
    Set-GhSecret "VERCEL_TOKEN"           $VERCEL_TOKEN  "Vercel personal access token"
    Set-GhSecret "VITE_API_BASE_URL"      "https://rootcause-api.onrender.com/api/v1" "Backend API URL for frontend build"
    Set-GhSecret "VITE_API_KEY"           $VITE_API_KEY  "API key baked into frontend"
}

# ─── Vercel project linking ───────────────────────────────────────────────────
Write-Header "Linking Frontend to Vercel"

$frontendDir = Join-Path $repoRoot "frontend"
Push-Location $frontendDir

if ($vercelOk -and -not [string]::IsNullOrWhiteSpace($VERCEL_TOKEN)) {
    Write-Step "Running: vercel link --yes --token=<token>"
    $env:VERCEL_TOKEN = $VERCEL_TOKEN
    & vercel link --yes --token=$VERCEL_TOKEN 2>&1 | Out-Null

    $projectJsonPath = Join-Path $frontendDir ".vercel\project.json"
    if (Test-Path $projectJsonPath) {
        $projectJson = Get-Content $projectJsonPath | ConvertFrom-Json
        $ORG_ID     = $projectJson.orgId
        $PROJECT_ID = $projectJson.projectId
        Write-Success "Vercel project linked!"
        Write-Step "VERCEL_ORG_ID:     $ORG_ID"
        Write-Step "VERCEL_PROJECT_ID: $PROJECT_ID"

        if ($ghOk) {
            $ORG_ID     | & gh secret set "VERCEL_ORG_ID"     --repo $REPO_SLUG 2>&1 | Out-Null
            $PROJECT_ID | & gh secret set "VERCEL_PROJECT_ID" --repo $REPO_SLUG 2>&1 | Out-Null
            Write-Success "Set VERCEL_ORG_ID + VERCEL_PROJECT_ID as GitHub secrets"
        } else {
            Write-Warn "Set these manually as GitHub secrets:"
            Write-Host "     VERCEL_ORG_ID     = $ORG_ID"
            Write-Host "     VERCEL_PROJECT_ID = $PROJECT_ID"
        }
    } else {
        Write-Warn "Could not find .vercel/project.json — run 'vercel link' manually in frontend/"
    }
} else {
    Write-Warn "Skipping Vercel link (no token provided)"
}

Pop-Location

# ─── Render Blueprint reminder ────────────────────────────────────────────────
Write-Header "Render Blueprint — One-Time Setup"
Write-Host ""
Write-Host "  If you haven't deployed via Render Blueprint yet:" -ForegroundColor White
Write-Host ""
Write-Host "  1. Go to: https://dashboard.render.com/blueprints" -ForegroundColor Cyan
Write-Host "  2. Click 'New Blueprint Instance'" -ForegroundColor Cyan
Write-Host "  3. Connect repo: $REPO_SLUG" -ForegroundColor Cyan
Write-Host "  4. Enter when prompted:" -ForegroundColor Cyan
if (-not [string]::IsNullOrWhiteSpace($NEON_URL)) {
    Write-Host "       DATABASE_URL      = $NEON_URL" -ForegroundColor Green
} else {
    Write-Host "       DATABASE_URL      = <paste your Neon JDBC or postgresql:// URL>" -ForegroundColor Yellow
}
Write-Host "       ROOTCAUSE_API_KEYS = $API_KEY_RENDER" -ForegroundColor Green
Write-Host "  5. Click 'Apply' — Render builds and deploys (~5 min)" -ForegroundColor Cyan
Write-Host ""

# ─── CORS update reminder ─────────────────────────────────────────────────────
Write-Header "After Frontend Deploys — Update CORS"
Write-Host ""
Write-Host "  After Vercel deploys, update Render env var:" -ForegroundColor White
Write-Host "    Render Dashboard → rootcause-api → Environment" -ForegroundColor Cyan
Write-Host "    CORS_ALLOWED_ORIGINS = https://<your-project>.vercel.app" -ForegroundColor Cyan
Write-Host ""

# ─── Trigger CI/CD ────────────────────────────────────────────────────────────
Write-Header "Triggering CI/CD Pipeline"

$pushAnswer = Read-Host "  Push current changes to main to trigger GitHub Actions? (Y/n)"
if ($pushAnswer -ne 'n' -and $pushAnswer -ne 'N') {
    Push-Location $repoRoot
    $branchStatus = & git status --porcelain
    if ($branchStatus) {
        & git add -A
        & git commit -m "chore: trigger deployment via setup-deploy.ps1" 2>&1
    }
    & git push origin main
    if ($LASTEXITCODE -eq 0) {
        Write-Success "Pushed to main — GitHub Actions pipeline started!"
        Write-Step "Monitor at: https://github.com/$REPO_SLUG/actions"
    }
    Pop-Location
} else {
    Write-Warn "Skipped push. Run 'git push origin main' when ready."
}

# ─── Final summary ────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "╔══════════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║   Setup Complete! Your deployment checklist:        ║" -ForegroundColor Green
Write-Host "╚══════════════════════════════════════════════════════╝" -ForegroundColor Green
Write-Host ""
Write-Host "  ✅ GitHub Actions workflow: .github/workflows/deploy.yml" -ForegroundColor Green
Write-Host "  ✅ DatabaseUrlPostProcessor registered (Neon URL auto-conversion)" -ForegroundColor Green
Write-Host "  ✅ frontend/.env.production baked with API URL" -ForegroundColor Green
Write-Host ""
Write-Host "  📋 Monitor deployments:" -ForegroundColor Cyan
Write-Host "     GitHub Actions : https://github.com/$REPO_SLUG/actions" -ForegroundColor White
Write-Host "     Render Backend : https://dashboard.render.com" -ForegroundColor White
Write-Host "     Vercel Frontend: https://vercel.com/dashboard" -ForegroundColor White
Write-Host ""
Write-Host "  🔍 Health Check (once Render is live):" -ForegroundColor Cyan
Write-Host "     curl https://rootcause-api.onrender.com/actuator/health/readiness" -ForegroundColor White
Write-Host ""
Write-Host "  🔑 API Test:" -ForegroundColor Cyan
Write-Host "     curl -H 'X-API-Key: $VITE_API_KEY' https://rootcause-api.onrender.com/api/v1/analyses" -ForegroundColor White
Write-Host ""
