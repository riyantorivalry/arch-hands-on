# Incident Report Template

## Executive Summary

**Incident ID:** [INC-YYYY-MM-DD-XXX]  
**Title:** [Brief incident description]  
**Date of occurrence:** [YYYY-MM-DD HH:MM:SS UTC]  
**Date declared resolved:** [YYYY-MM-DD HH:MM:SS UTC]  
**Total duration:** [HH:MM]  
**Severity:** [Critical / Major / Minor]  
**Impact:** [Number of users affected, services affected, tenant scope, revenue impact if known]

---

## Timeline

### Detection

**When discovered:** [YYYY-MM-DD HH:MM:SS UTC]  
**How discovered:** [Alert / Customer report / Monitoring dashboard / On-call review]  
**Time to detection:** [Duration from incident start]

**Detection details:**
- Which alert fired (if any)?
- What was the alert threshold?
- Who first noticed the anomaly?
- Initial symptoms?

### Investigation phase

| Time | Event | Owner | Evidence |
|------|-------|-------|----------|
| HH:MM | Event description | Name | Logs/metrics/trace link |
| HH:MM | - | - | - |

### Mitigation phase

| Time | Action | Owner | Effect | Next step |
|------|--------|-------|--------|-----------|
| HH:MM | Description | Name | Outcome | - |
| HH:MM | - | - | - | - |

---

## Detection Analysis

### Monitoring coverage

**Alerts that should have fired:**
- [What alert should have caught this?]
- Why didn't it fire?

**Gaps in observability:**
- [What data was missing?]
- [What metric would have helped?]

### Time-to-detection opportunity

- Minutes lost before awareness?
- Root cause could have been identified X minutes sooner if [condition].

---

## Impact Assessment

### User-facing impact

- Scope: [All users / Users in region / Users in tenant X / Feature X]
- Duration: [How long perception of issue]
- User experience: [What did they see?]
- Transactions affected: [Operations interrupted, data lost, consistency violated, etc.]

### Business impact

- Revenue: [Direct loss, SLA credits issued]
- Trust: [Reputational damage, customer communications needed]
- Follow-up required: [Notifications, credits, etc.]

### Infrastructure impact

- Services affected: [List with percentage of fleet if partial]
- Data consistency: [Any anomalies or corrections needed?]
- Following incidents risk: [Cascading failures that were narrowly avoided?]

---

## Root Cause Analysis

### Primary cause

**What broke:**
[Component, configuration, or behavior that caused the incident]

### Contributing factors

- Factor 1
- Factor 2
- Factor 3

### How the incident propagated

[Explain the chain: initial failure → observed impact]

### Why detection was delayed

[If applicable: monitoring gaps, thresholds, rate limiting, batching, etc.]

---

## Technical Details

### Immediate cause

**Layer/Component:** [Which part of the system?]  
**Error type:** [Exception, timeout, configuration, resource exhaustion, etc.]  
**Affected service(s):** [Service A / Service B]  
**Affected tenant(s):** [All / Tenant X / Feature flag X]

### Logs and metrics

**Key log entries:**
```
[Example error logs or anomalies]
```

**Metric anomalies:**
- Metric: [Value gradient]
- Metric: [Value gradient]

**Trace link:** [APM UI / Jaeger link if applicable]

### Configuration or code involved

```
[If a config change or code bug: the exact snippet]
```

**Version affected:** [Release tag or commit]  
**Version fixed:** [Release tag or commit if already fixed]

---

## What Went Wrong

### System assumptions that failed

- Assumption: [Description]
  - Why it failed: [Root cause]
  - Evidence: [Logs, metrics]

### Operational gaps

- Process gap: [Description]
  - Consequence: [Why it mattered]

### Monitoring/logging gaps

- Gap: [What wasn't visible?]
  - Impact: [Why it delayed resolution]

---

## Remediation Steps Taken

### Emergency fix (during incident)

**Action:** [What was done to stop the bleeding?]  
**Owner:** [Who executed]  
**Time:** [When]  
**Effectiveness:** [Did it work? Partially?]  
**Side effects:** [Any negative consequences?]

### Permanent fix (post-incident)

**Action:** [Code change / configuration / process]  
**PR/Ticket:** [Link to change]  
**Deployment:** [When will it go out?]  
**Validation:** [How do we know it fixes the root cause?]

---

## Follow-Up Actions

### Immediate (within 1 week)

- [ ] **Action:** [Description]  
  **Owner:** [Name]  
  **Deadline:** [Date]  
  **Risk if delayed:** [Why it matters]

- [ ] **Action:** [Description]  
  **Owner:** [Name]  
  **Deadline:** [Date]  
  **Risk if delayed:** [Why it matters]

### Short-term (within 1 month)

- [ ] **Action:** [Description]  
  **Owner:** [Name]  
  **Deadline:** [Date]  
  **Story/Ticket:** [Link]

### Long-term (roadmap item)

- **Project:** [Major change / architectural improvement]  
  **Problem solved:** [What does this prevent?]  
  **Estimated effort:** [T-shirt size]  
  **Linked incidents:** [Other incidents this would have helped]

---

## Prevention and Early Detection

### What should alert sooner?

**New alert:** [Alert name and thresholds]
- Why it would help: [Explanation]
- Owner: [Who implements]
- Deployment timeline: [When]

**Improved metric:**
- Current: [What we measure now]
- Proposed: [Better metric]
- Rationale: [Why better]

### Process changes

**Change:** [Updated runbook / escalation / checklist]  
**Why it helps:** [Prevention / faster detection / faster resolution]  
**Implementation:** [Who owns, timeline]

### System design changes

**Area:** [Architecture / deployment / capacity planning]  
**Change:** [What needs to improve]  
**Why it helps:** [In this incident and others like it]  
**Estimated effort:** [T-shirt size]

---

## Learning Opportunities

### Knowledge gaps identified

- Gap: [What knowledge was missing?]
  - Owner to close gap: [Training / documentation]
  - Timeline: [When]

### Similar incidents to review

- [Linked incident ID]: [How was it similar?]
- [Linked incident ID]: [What did we learn from that?]

---

## Metrics for this incident

| Metric | Value | Target | Status |
|--------|-------|--------|--------|
| Time to detect | XX min | < 5 min | ❌ |
| Time to diagnose root cause | XX min | < 15 min | ⚠️ |
| Time to mitigate | XX min | < 30 min | ✅ |
| Time to permanent fix commit | XX hours | < 24 hours | ✅ |
| Incidents of this type (last 90 days) | X | < 1 | - |

---

## Communication

### Customer notifications sent

- Notification type: [Status page / In-app alert / Email]
- Recipients: [Customer segment / all users / affected tenants]
- Timestamp: [When sent]
- Approval: [Who approved the message?]

### Follow-up communication required

- Type: [Postmortem summary / Technical breakdown / Apology + credit]
- Timeline: [When to send]
- Owner: [Who drafts]

---

## Appendix

### Incident war room recording
[Link to Zoom recording / transcript]

### Slack thread
[Link to relevant threads]

### Related tickets/PRs
- [Link to PR fixing root cause]
- [Link to preventive work ticket]
- [Link to monitoring improvement ticket]

### Assets
- [Dashboard snapshot]
- [Error log dump]
- [Query traces]

### Contact info during incident
- Incident commander: [Name, contact]
- On-call engineer: [Name, contact]
- Engineering lead: [Name, contact]

---

## Ownership and Review

**Incident report prepared by:** [Name] on [Date]  
**Reviewed by:** [Engineering lead name] on [Date]  
**Approved for publication:** [Manager name] on [Date]

**Review checklist:**
- [ ] Root cause clearly identified
- [ ] Timeline is accurate
- [ ] Impact assessment complete
- [ ] Follow-up actions assigned with owners and deadlines
- [ ] No sensitive internal information exposed
- [ ] Lessons are actionable
- [ ] Customer communications captured
