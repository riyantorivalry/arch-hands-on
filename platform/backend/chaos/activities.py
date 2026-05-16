import subprocess
import time
import requests


def check_health_up(url, timeout=5):
    """Return True if health endpoint returns status UP within timeout seconds."""
    try:
        resp = requests.get(url, timeout=timeout)
        if resp.status_code == 200:
            body = resp.json()
            return body.get('status') == 'UP'
        return False
    except Exception:
        return False


def check_health_down(url, timeout=5):
    """Return True if health endpoint is not available or not UP."""
    try:
        resp = requests.get(url, timeout=timeout)
        if resp.status_code != 200:
            return True
        body = resp.json()
        return body.get('status') != 'UP'
    except Exception:
        return True


def check_http_ok(url, timeout=5):
    try:
        resp = requests.get(url, timeout=timeout)
        return resp.status_code == 200
    except Exception:
        return False


def wait_for_health(url, up=True, timeout=60, interval=2):
    """Wait until the health endpoint matches expected state (up/down) or timeout.
    Returns True if expected state achieved, False otherwise.
    """
    deadline = time.time() + timeout
    check = check_health_up if up else check_health_down
    while time.time() < deadline:
        if check(url):
            return True
        time.sleep(interval)
    return False


def wait_for_http_ok(url, timeout=60, interval=2):
    deadline = time.time() + int(timeout)
    while time.time() < deadline:
        if check_http_ok(url):
            return True
        time.sleep(interval)
    return False


def stop_container(container: str):
    """Stop a Docker container by name or id."""
    subprocess.run(["docker", "stop", container], check=True)
    return True


def start_container(container: str):
    """Start a Docker container by name or id."""
    subprocess.run(["docker", "start", container], check=True)
    return True


def restart_container(container: str):
    """Restart a Docker container by name or id."""
    subprocess.run(["docker", "restart", container], check=True)
    return True


# --- Toxiproxy helpers ---
TOXIPROXY_API = "http://localhost:8474"


def create_proxy(name: str, listen: str, upstream: str):
    """Create a proxy in Toxiproxy mapping listen -> upstream.
    listen example: 0.0.0.0:9201
    upstream example: opensearch:9200
    """
    url = f"{TOXIPROXY_API}/proxies"
    payload = {"name": name, "listen": listen, "upstream": upstream}
    resp = requests.post(url, json=payload)
    resp.raise_for_status()
    return resp.json()


def delete_proxy(name: str):
    url = f"{TOXIPROXY_API}/proxies/{name}"
    resp = requests.delete(url)
    resp.raise_for_status()
    return True


def create_toxic(proxy: str, toxic_type: str, toxic_name: str, attributes: dict, toxicity=1.0):
    """Create a toxic on a proxy.
    toxic_type examples: latency, limit_data, bandwidth, timeout
    attributes: dict of toxic-specific attributes, e.g., {"latency": 2000, "jitter": 100}
    """
    url = f"{TOXIPROXY_API}/proxies/{proxy}/toxics"
    payload = {"name": toxic_name, "type": toxic_type, "stream": "downstream", "toxicity": toxicity, "attributes": attributes}
    resp = requests.post(url, json=payload)
    resp.raise_for_status()
    return resp.json()


def remove_toxic(proxy: str, toxic_name: str):
    url = f"{TOXIPROXY_API}/proxies/{proxy}/toxics/{toxic_name}"
    resp = requests.delete(url)
    resp.raise_for_status()
    return True


def list_proxies():
    url = f"{TOXIPROXY_API}/proxies"
    resp = requests.get(url)
    resp.raise_for_status()
    return resp.json()


def sleep_for(seconds: int):
    time.sleep(seconds)
    return True


