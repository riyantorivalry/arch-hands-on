# Incidents

This directory stores simulated incident reports and postmortems.

## Quick start

See [INCIDENT_TEMPLATE.md](./INCIDENT_TEMPLATE.md) for the full report structure and guidelines.

## Initial scenario backlog

Phase 1 simulation scenarios:

- traffic spike during a major tenant event
- PostgreSQL replication lag
- duplicate Kafka consumer delivery
- partial regional outage

Phase 2+ scenarios (once infrastructure added):

- cache stampede
- realtime connection storms
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

1. Copy [INCIDENT_TEMPLATE.md](./INCIDENT_TEMPLATE.md) to a new file: `INCIDENT-[date]-[subject].md`
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
