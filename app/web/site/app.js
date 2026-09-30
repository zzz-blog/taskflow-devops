/* TaskFlow 前端：仅用原生 JS，通过同源 /api 反向代理访问后端 */
const $ = (sel) => document.querySelector(sel);
const API = "/api";
let currentFilter = "all";

async function fetchJSON(url, options) {
  const resp = await fetch(url, { headers: { "Content-Type": "application/json" }, ...options });
  if (resp.status === 204) return null;
  const body = await resp.json().catch(() => ({}));
  if (!resp.ok) throw new Error(body.detail ? JSON.stringify(body.detail) : `HTTP ${resp.status}`);
  return body;
}

function renderStatus(ok) {
  $("#api-status").className = `dot ${ok ? "ok" : "fail"}`;
  $("#api-status-text").textContent = ok ? "API 正常" : "API 不可用";
}

async function checkHealth() {
  try {
    const body = await fetchJSON(`${API}/healthz`);
    renderStatus(body.status === "ok");
    $("#build-info").textContent = `TaskFlow API v${body.version} · env=${body.env}`;
  } catch {
    renderStatus(false);
  }
}

function taskItem(t) {
  const li = document.createElement("li");
  li.innerHTML = `
    <input type="checkbox" ${t.done ? "checked" : ""} data-id="${t.id}" />
    <div class="task-title ${t.done ? "done-text" : ""}">
      <div>${escapeHTML(t.title)}</div>
      ${t.description ? `<div class="task-desc">${escapeHTML(t.description)}</div>` : ""}
    </div>
    <span class="badge p${t.priority}">${{ 1: "高", 2: "中", 3: "低" }[t.priority]}优先级</span>
    <button class="del" data-id="${t.id}">删除</button>`;
  return li;
}

function escapeHTML(s) {
  return s.replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}

async function loadTasks() {
  const params = new URLSearchParams();
  if (currentFilter !== "all") params.set("done", String(currentFilter === "done"));
  try {
    const tasks = await fetchJSON(`${API}/tasks?${params}`);
    const list = $("#task-list");
    list.replaceChildren(...tasks.map(taskItem));
    $("#empty-hint").classList.toggle("hidden", tasks.length > 0);
    await loadStats();
    renderStatus(true);
  } catch {
    renderStatus(false);
    $("#task-list").replaceChildren();
    $("#empty-hint").classList.remove("hidden");
  }
}

async function loadStats() {
  const s = await fetchJSON(`${API}/tasks/stats`);
  $("#stat-total").textContent = s.total;
  $("#stat-todo").textContent = s.todo;
  $("#stat-done").textContent = s.done;
}

$("#task-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const title = $("#f-title").value.trim();
  if (!title) return;
  try {
    await fetchJSON(`${API}/tasks`, {
      method: "POST",
      body: JSON.stringify({
        title,
        description: $("#f-desc").value.trim() || null,
        priority: Number($("#f-priority").value),
      }),
    });
    $("#f-title").value = "";
    $("#f-desc").value = "";
    loadTasks();
  } catch (err) {
    alert(`创建失败：${err.message}`);
  }
});

$("#task-list").addEventListener("click", async (e) => {
  const id = e.target.dataset.id;
  if (!id) return;
  if (e.target.type === "checkbox") {
    await fetchJSON(`${API}/tasks/${id}`, { method: "PATCH", body: JSON.stringify({ done: e.target.checked }) });
  } else if (e.target.classList.contains("del")) {
    await fetchJSON(`${API}/tasks/${id}`, { method: "DELETE" });
  }
  loadTasks();
});

$("#filters").addEventListener("click", (e) => {
  if (e.target.dataset.f) {
    currentFilter = e.target.dataset.f;
    document.querySelectorAll("#filters button").forEach((b) => b.classList.toggle("active", b === e.target));
    loadTasks();
  }
});

checkHealth();
loadTasks();
setInterval(checkHealth, 10000);
