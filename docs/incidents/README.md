# Incidents

This directory stores simulated incident reports and postmortems.

## Initial scenario backlog

- traffic spike during a major tenant event
- PostgreSQL replication lag
- duplicate Kafka consumer delivery
- cache stampede
- realtime connection storms
- authorization policy regression
- rate limit false positive spike
- analytics secondary sink outage
- partial regional outage
- asymmetric network partitions
- cascading service degradation

## Report format

Each incident report should include:

- executive summary (impact, duration, severity)
- timeline (detection → investigation → mitigation)
- impact assessment (user, business, infrastructure)
- root cause analysis (primary, contributing factors, propagation)
- remediation (emergency fix, permanent fix)
- follow-up actions with owners and deadlines
- learning opportunities and prevention strategies

## How to contribute

1. Create a new file named `INCIDENT-[date]-[subject].md`
2. Document the incident with precise timeline and metrics
3. Include logs, traces, or dashboard snapshots in appendix
4. Ensure follow-up actions are owned and tracked
5. Publish to share learnings across team
6. Schedule post-incident reviews to extract lessons

## Key questions to answer

- What should have alerted sooner?
- What monitoring gaps exist?
- What process changes would help?
- What architectural changes would prevent recurrence?
