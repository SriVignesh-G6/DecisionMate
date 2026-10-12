
const BASE = (
  import.meta.env.DEV
    ? (import.meta.env.VITE_API_URL || "http://localhost:8080")
    : ""
).replace(/\/+$/, "") + "/api";

async function request(path, options = {}) {
  const method = (options.method || "GET").toUpperCase();

  const headers = {
    ...(options.body ? { "Content-Type": "application/json" } : {}),
    ...(options.headers || {}),
  };

  if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
    const csrfResponse = await fetch(`${BASE}/auth/csrf`, {
      credentials: "include",
    });

    if (!csrfResponse.ok) {
      throw new Error("Could not obtain CSRF token. Please refresh and try again.");
    }

    const csrfData = await csrfResponse.json();
    const token = csrfData.token;

    if (!token) {
      throw new Error("CSRF token missing from server response.");
    }

    headers[csrfData.headerName || "X-XSRF-TOKEN"] = token;
  }

  const response = await fetch(`${BASE}${path}`, {
    ...options,
    method,
    credentials: "include",
    headers,
  });

  if (response.status === 204) return null;

  const data = await response.json().catch(() => ({}));

  if (!response.ok) {
    throw new Error(
      data.message || data.error || `Request failed (${response.status})`
    );
  }

  return data;
}
