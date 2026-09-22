---
name: security-review
description: Security-focused review of the current diff for ops-dashboard. Always reports the full test suite results (pass/fail per test) and an OWASP Top 10 alignment table alongside any vulnerability findings.
---

# Security review (ops-dashboard)

Runs a security-focused review of the current diff against `main`, the same way
the general security-review process does (input validation, authN/authZ,
crypto/secrets, injection/code execution, data exposure — scoped to what changed,
not the whole codebase). In this repository, every run must also include two
extra sections so a reviewer never has to ask for them separately: the full test
listing, and an OWASP Top 10 alignment table.

## Steps

1. **Vulnerability scan.** Diff the current branch against `origin/main`. Identify
   candidate findings, then filter out anything below ~80% confidence
   (>=8/10) of real exploitability. Exclude: DOS/resource exhaustion, secrets
   already secured on disk, rate limiting, outdated-dependency findings (handled
   elsewhere), and anything not newly introduced by the diff.

2. **Full test listing — always run, never summarize away.**
   - `./mvnw test` — report every test class with its `Tests run/Failures/Errors`
     line, plus the final aggregate line.
   - `npx jest --verbose` — report every `describe`/test name with ✓/✗, grouped
     by file, plus the final `Tests: <n> passed, <t> total` line.
   - If either suite's count is below the baseline in
     `.claude/skills/release-check/SKILL.md`, call that out explicitly as a
     regression, not just a number.

3. **OWASP Top 10 (2021) alignment table — always include.** For each of the 10
   categories, state: **Covered** (explicitly checked, in scope for this diff),
   **Partial** (checked at a narrow angle only — say which), or **Excluded**
   (out of scope by this review's own rules — say why). Do not skip categories;
   "not applicable, no auth code touched" is a valid Covered/none-found entry,
   silence is not.
   - A03 Injection (incl. XSS)
   - A01 Broken Access Control
   - A07 Identification & Authentication Failures
   - A02 Cryptographic Failures
   - A09 Security Logging & Monitoring Failures
   - A05 Security Misconfiguration
   - A08 Software & Data Integrity Failures
   - A06 Vulnerable & Outdated Components (excluded by design — separate process)
   - A10 SSRF (narrowed — host/protocol control only, not path-only)
   - A04 Insecure Design (out of scope — this is a diff scan, not threat modeling)

4. Note plainly that this is a diff-level scan, not a NIST-aligned assessment —
   it satisfies one practice (SSDF PW.7, human review for vulnerabilities), not
   a full SDLC control set (no SBOM/dependency scan, no threat model, no
   control-family mapping).

## Output format

```
# Security review: <branch/PR>

## Vulnerability findings
<markdown findings per the standard format, or "None above the confidence bar.">

## Test results
Java:  <per-class lines> ... <n> run, <f> failures, <e> errors  (baseline <n>)  PASS|FAIL
Jest:  <per-file, per-test ✓/✗ listing> ... <n> passed, <t> total  (baseline <n>)  PASS|FAIL

## OWASP Top 10 alignment
| Category | Status | Note |
|---|---|---|
...

## Framework note
Diff-level scan only; not a NIST-aligned assessment (see above).
```
