Chaos experiments for platform-backend

This folder contains Chaos Toolkit experiments and helper activities to test the backend resiliency.

Prerequisites
- Python 3.9+
- Docker (if you plan to run container-based experiments)

Install dependencies (create virtualenv recommended):

```powershell
cd D:\Project\arch-hands-on\platform\backend\chaos
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

Run an experiment

```powershell
# Basic health check
chaos run experiments\basic-health-check.yaml

# Kill and recover backend (requires backend running as a Docker container named 'platform-backend')
chaos run experiments\kill-recover-backend.yaml

# Toxiproxy latency example (requires observability stack with toxiproxy and opensearch running)
chaos run experiments\toxiproxy-latency-opensearch.yaml
```

For an end-to-end backend test, start the backend with the proxy URI so document search traffic flows through Toxiproxy:

```powershell
cd D:\Project\arch-hands-on\platform\backend
$env:SPRING_DATA_ELASTICSEARCH_URIS="http://localhost:9201"
mvn spring-boot:run
```

Notes
- The Python provider functions are implemented in `chaos/activities.py`. They use `requests` for HTTP probes and `docker` CLI via subprocess for container control.
- The kill/recover experiment assumes your backend is running in Docker and the container name is `platform-backend`. You can change the `container` parameter in the experiment file to match your setup.
- These experiments are intentionally simple and safe; they demonstrate how to verify health and perform controlled fault injection. Extend with latency injection, network partitions or database failures as needed.

Extending experiments
- Add probes that hit important endpoints (e.g., `/api/v1/tasks`, `/graphql`) and validate business outcomes
- Add actions that simulate network partition (using `tc` inside a container or `docker network disconnect`)
- Integrate with CI: run experiments in a staging environment and fail builds if critical SLOs are violated

Security
- Be cautious running destructive experiments in production. Use staging environments and approvals.



