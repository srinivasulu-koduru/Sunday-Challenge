/**
 * Sunday Challenge — Frontend Application Script
 */

document.addEventListener('DOMContentLoaded', () => {
  const checkBtn = document.getElementById('check-backend-btn');
  const statusPill = document.getElementById('backend-status-pill');
  const statusText = document.getElementById('backend-status-text');
  const responseConsole = document.getElementById('api-response-console');
  const lastCheckedSpan = document.getElementById('last-checked-time');

  // Default API Endpoint
  const HEALTH_API_URL = 'http://localhost:8080/api/health';

  /**
   * Performs an HTTP GET request to check Spring Boot backend health
   */
  async function checkBackendHealth() {
    if (!statusPill || !statusText) return;

    statusPill.className = 'status-pill checking';
    statusText.textContent = 'CONNECTING...';
    if (checkBtn) checkBtn.disabled = true;

    try {
      const startTime = performance.now();
      const response = await fetch(HEALTH_API_URL, {
        method: 'GET',
        headers: {
          'Accept': 'application/json'
        },
        credentials: 'include'
      });

      const endTime = performance.now();
      const durationMs = Math.round(endTime - startTime);

      if (response.ok) {
        const data = await response.json();
        
        statusPill.className = 'status-pill online';
        statusText.textContent = 'ONLINE (200 OK)';

        renderJsonResponse(data, durationMs, response.status);
      } else {
        throw new Error(`HTTP ${response.status} - ${response.statusText}`);
      }
    } catch (error) {
      console.warn('Backend Health Check Error:', error);

      statusPill.className = 'status-pill offline';
      statusText.textContent = 'OFFLINE / UNREACHABLE';

      renderErrorResponse(error);
    } finally {
      if (checkBtn) checkBtn.disabled = false;
      if (lastCheckedSpan) {
        const now = new Date();
        lastCheckedSpan.textContent = now.toLocaleTimeString();
      }
    }
  }

  function renderJsonResponse(data, durationMs, statusCode) {
    if (!responseConsole) return;

    const formattedJson = JSON.stringify(data, null, 2);
    
    const highlighted = formattedJson
      .replace(/"([^"]+)":/g, '<span class="json-key">"$1"</span>:')
      .replace(/: "([^"]+)"/g, ': <span class="json-string">"$1"</span>');

    responseConsole.innerHTML = `
<div style="color: #64748b; margin-bottom: 0.5rem;">// GET /api/health (${statusCode} OK - ${durationMs}ms)</div>
<pre><code>${highlighted}</code></pre>`;
  }

  function renderErrorResponse(error) {
    if (!responseConsole) return;

    responseConsole.innerHTML = `
<div style="color: #f43f5e; margin-bottom: 0.5rem;">// Connection Failed</div>
<pre><code>{
  <span class="json-key">"error"</span>: <span class="json-string">"Unable to connect to backend at http://localhost:8080/api/health"</span>,
  <span class="json-key">"details"</span>: <span class="json-string">"${error.message || 'NetworkError / Backend offline'}"</span>,
  <span class="json-key">"troubleshooting"</span>: <span class="json-string">"Ensure Spring Boot is running on port 8080"</span>
}</code></pre>`;
  }

  if (checkBtn) {
    checkBtn.addEventListener('click', checkBackendHealth);
  }

  checkBackendHealth();
});
