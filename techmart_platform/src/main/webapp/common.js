const APP_BASE = window.location.pathname.replace(
  /\/(?:admin|user)\/.*$|\/[^^/]+\.html$/,
  "/",
);
const API_ROOT = `${window.location.origin}${APP_BASE}`;
function appPath(path) {
  const normalized = path.replace(/^(?:\.\.\/)+/, "").replace(/^\/+/, "");
  return `${APP_BASE}${normalized}`;
}

function api(path) {
  return `${API_ROOT}${path}`;
}

function parseJson(response) {
  return response.text().then((text) => {
    if (!text) return {};
    try {
      return JSON.parse(text);
    } catch (e) {
      return {};
    }
  });
}

function showMessage(id, text, isError = false, showToast = isError) {
  const el = document.getElementById(id);
  if (el) {
    el.textContent = text;
    el.style.color = isError ? "var(--danger)" : "var(--text-main)";
  }
  
  if (showToast && window.showToastNotification) {
    window.showToastNotification(isError ? "Notification" : "Success", text, isError);
  }
}

function getQueryParam(name) {
  return new URLSearchParams(window.location.search).get(name);
}

async function fetchCurrentUser() {
  const response = await fetch(api("/api/auth/me"));
  if (!response.ok) {
    return null;
  }
  const body = await parseJson(response);
  return body.user || null;
}

function redirectToRole(role) {
  if (role === "ADMIN") {
    window.location.href = appPath("admin/dashboard.html");
  } else {
    window.location.href = appPath("user/home.html");
  }
}

function requireLogin(defaultRedirect = "login.html") {
  return fetchCurrentUser().then((user) => {
    if (!user) {
      window.location.href = appPath(defaultRedirect);
      return null;
    }
    return user;
  });
}

function formatCurrency(amount) {
  return new Intl.NumberFormat("en-LK", {
    style: "currency",
    currency: "LKR",
  }).format(amount);
}

// Dynamic toast notification system (runs automatically on all pages)
(function () {
  // 1. Inject Toast CSS styles dynamically
  const style = document.createElement("style");
  style.textContent = `
        #toast-container {
            position: fixed;
            top: 20px;
            right: 20px;
            z-index: 99999;
            display: flex;
            flex-direction: column;
            gap: 12px;
            max-width: 360px;
            width: calc(100% - 40px);
            pointer-events: none;
        }
        .toast-item {
            background: #ffffff;
            border-left: 4px solid var(--primary, #4f46e5);
            border-radius: var(--radius, 8px);
            box-shadow: var(--shadow-lg, 0 10px 15px -3px rgba(0,0,0,0.1));
            padding: 16px;
            display: flex;
            flex-direction: column;
            gap: 4px;
            color: var(--text-main, #0f172a);
            pointer-events: auto;
            animation: toast-fade-in 0.3s cubic-bezier(0.16, 1, 0.3, 1) forwards;
            transition: all 0.3s ease;
            border: 1px solid var(--border, #e2e8f0);
            border-left-width: 4px;
        }
        .toast-title {
            font-weight: 700;
            font-size: 0.95rem;
            color: var(--text-main, #0f172a);
        }
        .toast-message {
            font-size: 0.85rem;
            color: var(--text-muted, #64748b);
        }
        @keyframes toast-fade-in {
            from { opacity: 0; transform: translateY(12px) scale(0.95); }
            to { opacity: 1; transform: translateY(0) scale(1); }
        }
        @keyframes toast-fade-out {
            from { opacity: 1; transform: translateY(0) scale(1); }
            to { opacity: 0; transform: translateY(-12px) scale(0.95); }
        }
    `;
  document.head.appendChild(style);

  // 2. Create toast container element
  let container = document.getElementById("toast-container");
  if (!container) {
    container = document.createElement("div");
    container.id = "toast-container";
    document.body.appendChild(container);
  }

  // Set of seen notification IDs (persisted per page view session to prevent repetition)
  const seenNotifications = new Set(
    JSON.parse(sessionStorage.getItem("seen_notifications") || "[]"),
  );

  function showToast(title, message, isError = false) {
    const item = document.createElement("div");
    item.className = "toast-item";
    if (isError) {
      item.style.borderLeftColor = "var(--danger, #ef4444)";
    } else {
      item.style.borderLeftColor = "var(--primary, #4f46e5)";
    }
    item.innerHTML = `
            <div class="toast-title">${title}</div>
            <div class="toast-message">${message}</div>
        `;
    container.appendChild(item);

    // Auto-remove after 4 seconds
    setTimeout(() => {
      item.style.animation = "toast-fade-out 0.3s ease forwards";
      setTimeout(() => item.remove(), 300);
    }, 4000);
  }

  // Expose toast function globally so it can be called by showMessage()
  window.showToastNotification = showToast;

  function pinNotification(notif) {
    let pinned = JSON.parse(localStorage.getItem("pinned_notifications") || "[]");
    if (!pinned.some(p => p.id === notif.id)) {
      pinned.push(notif);
      localStorage.setItem("pinned_notifications", JSON.stringify(pinned));
      renderPinnedNotifications();
    }
  }

  function renderPinnedNotifications() {
    let container = document.getElementById("pinned-notifications-bar");
    const pinned = JSON.parse(localStorage.getItem("pinned_notifications") || "[]");

    if (pinned.length === 0) {
      if (container) container.remove();
      return;
    }

    if (!container) {
      container = document.createElement("div");
      container.id = "pinned-notifications-bar";
      container.style.cssText = "background: #fef2f2; border: 1px solid #fee2e2; border-left: 5px solid #ef4444; padding: 12px 24px; margin-bottom: 20px; border-radius: 8px; display: flex; flex-direction: column; gap: 8px;";

      const mainContainer = document.querySelector(".container");
      if (mainContainer) {
        const header = mainContainer.querySelector("header");
        if (header && header.nextSibling) {
          mainContainer.insertBefore(container, header.nextSibling);
        } else {
          mainContainer.appendChild(container);
        }
      } else {
        document.body.insertBefore(container, document.body.firstChild);
      }
    }

    container.innerHTML = pinned.map(notif => `
        <div style="display:flex; justify-content:space-between; align-items:center; color:#b91c1c; font-size:0.95rem; font-weight:600;">
            <div>
                <span style="text-transform:uppercase; font-size:0.8rem; background:#fee2e2; padding:2px 8px; border-radius:4px; margin-right:8px;">PINNED ALERT</span>
                <strong>${notif.title}:</strong> ${notif.message}
            </div>
            <button onclick="unpinNotification('${notif.id}')" style="background:none; border:none; color:#ef4444; font-size:1.4rem; cursor:pointer; font-weight:bold; padding:0 8px; line-height:1;">&times;</button>
        </div>
    `).join("");
  }

  window.unpinNotification = function(id) {
    let pinned = JSON.parse(localStorage.getItem("pinned_notifications") || "[]");
    pinned = pinned.filter(p => p.id !== id);
    localStorage.setItem("pinned_notifications", JSON.stringify(pinned));
    renderPinnedNotifications();
  };

  // 3. Poll Notifications
  async function pollNotifications() {
    if (document.hidden) return;

    try {
      const response = await fetch(api("/api/notifications"));
      if (!response.ok) return;

      const notifications = await response.json();
      let updated = false;

      notifications.forEach((notif) => {
        if (!seenNotifications.has(notif.id)) {
          if (notif.title.toLowerCase().includes("cancelled") || notif.title.toLowerCase().includes("cancel")) {
            pinNotification(notif);
          }
          showToast(notif.title, notif.message, notif.title.toLowerCase().includes("cancelled"));
          seenNotifications.add(notif.id);
          updated = true;
        }
      });

      if (updated) {
        sessionStorage.setItem(
          "seen_notifications",
          JSON.stringify(Array.from(seenNotifications)),
        );
      }
    } catch (e) {
      // Ignore network errors during polling
    }
  }

  // Render any saved pinned alerts on load
  renderPinnedNotifications();

  // Start polling 1.5 seconds after page loads, then query every 3 seconds
  setTimeout(() => {
    pollNotifications();
    setInterval(pollNotifications, 3000);
  }, 1500);
})();
