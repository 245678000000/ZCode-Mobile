(function (cfg) {
  try {
    if (window.__ZCodeObserverInstalled) {
      if (window.__ZCodeScan) window.__ZCodeScan('manual');
      return 'already';
    }
    window.__ZCodeObserverInstalled = true;
    var DEBOUNCE = (cfg && cfg.debounceMs) || 500;
    var timer = null;
    var lastJson = '';
    var bridge = window.ZCodeAndroidBridge;
    // Body text is read once per snapshot; innerText forces layout and is expensive.
    var bodyTextCache = '';

    function sanitize(s, max) {
      if (!s) return '';
      var t = String(s).slice(0, max || 400);
      return t.replace(/(cookie|authorization|token|session|secret|password)[=:][^\s&]+/ig, '$1:•••')
        .replace(/bearer\s+[a-z0-9._\-]+/ig, 'bearer:•••');
    }
    function redactedUrl() {
      try {
        var u = location.origin + location.pathname;
        return sanitize(u, 300);
      } catch (e) { return ''; }
    }
    function bodyText() {
      if (bodyTextCache) return bodyTextCache;
      bodyTextCache = sanitize((document.body && document.body.innerText) || '', 4000);
      return bodyTextCache;
    }
    function visible(el) {
      if (!el || !el.getBoundingClientRect) return false;
      var r = el.getBoundingClientRect();
      var st = window.getComputedStyle ? getComputedStyle(el) : null;
      if (st && (st.display === 'none' || st.visibility === 'hidden')) return false;
      return r.width > 0 && r.height > 0;
    }
    function qsAll(selectors) {
      var out = [];
      (selectors || []).forEach(function (s) {
        try { out = out.concat(Array.prototype.slice.call(document.querySelectorAll(s))); } catch (e) {}
      });
      return out.filter(visible);
    }
    function textOf(el) {
      return sanitize((el && (el.innerText || el.textContent) || '').replace(/\s+/g, ' ').trim(), 240);
    }
    function hash(s) {
      var h = 0, i;
      for (i = 0; i < s.length; i++) h = ((h << 5) - h + s.charCodeAt(i)) | 0;
      return String(h);
    }
    function commonAncestor(a, b) {
      if (!a || !b) return null;
      var node = a, depth = 0;
      while (node && depth < 6) {
        if (node.contains(b)) return node;
        node = node.parentElement;
        depth++;
      }
      return null;
    }
    var STATUS_RE = /(等待确认|需要确认|待确认|运行中|正在执行|执行中|已完成|失败|已取消|排队|waiting for confirm|waiting approval|needs confirmation|running|in progress|working|completed|complete|failed|error|cancelled|canceled|queued)/i;
    var ALLOW_RE = /^(allow|approve|confirm|always allow|允许|始终允许|确认)$/i;
    var REJECT_RE = /^(reject|deny|refuse|拒绝|不允许)$/i;
    var WAIT_RE = /(waiting for|waiting approval|needs confirmation|permission|authorization|等待确认|需要确认|需要授权|等待授权)/i;
    var FILE_RE = /\.(md|html|htm|png|jpe?g|webp|gif|pdf|json|kt|java|ts|tsx|js|py|go|rs|sh|txt)(\?|$)/i;
    var EXPIRED_RE = /(session expired|登录过期|会话过期|连接已断开|disconnected|unauthorized)/i;

    function statusFrom(t) {
      if (!t) return '';
      var m = t.match(STATUS_RE);
      return m ? m[0] : '';
    }

    function scanTasks() {
      var rows = qsAll(cfg.taskRow);
      var tasks = [];
      function push(id, title, status, step, agent) {
        if (!title) return;
        tasks.push({
          id: id || hash(title),
          title: sanitize(title, 120),
          status: status || '',
          step: step || '',
          agentName: agent || ''
        });
      }
      rows.forEach(function (row) {
        var titleEl = qsAll(cfg.taskTitle).filter(function (n) { return row.contains(n); })[0];
        var statusEl = qsAll(cfg.taskStatus).filter(function (n) { return row.contains(n); })[0];
        var raw = textOf(row);
        var title = titleEl ? textOf(titleEl) : raw.replace(STATUS_RE, '').trim().slice(0, 80);
        var status = statusEl ? statusFrom(textOf(statusEl)) : statusFrom(raw);
        // The id must NOT depend on status, otherwise a status change looks like a new task.
        if (title && (status || title.length > 1)) push(row.getAttribute('data-testid') || hash(title), title, status, '', '');
      });
      if (tasks.length === 0) {
        var headings = qsAll(cfg.taskTitle);
        headings.slice(0, 8).forEach(function (h) {
          var block = h.parentElement || h;
          var raw = textOf(block);
          var status = statusFrom(raw);
          var title = textOf(h);
          if (title && title.length < 80) push(hash(title), title, status, '', '');
        });
      }
      if (tasks.length === 0) {
        var st = statusFrom(bodyText());
        var t = (document.title || '').trim();
        if (t) push('current', t, st, '', '');
      }
      var seen = {};
      return tasks.filter(function (t) {
        if (seen[t.id]) return false;
        seen[t.id] = 1;
        return true;
      }).slice(0, 20);
    }

    function scanApproval() {
      var dialogs = qsAll(cfg.approvalDialog);
      var buttons = Array.prototype.slice.call(document.querySelectorAll('button,[role="button"]')).filter(visible);
      var allowBtn = null, rejectBtn = null;
      buttons.forEach(function (b) {
        var label = (b.getAttribute('aria-label') || textOf(b) || '').trim();
        if (!allowBtn && ALLOW_RE.test(label)) allowBtn = { el: b, label: label };
        if (!rejectBtn && REJECT_RE.test(label)) rejectBtn = { el: b, label: label };
      });
      qsAll(cfg.approvalAllowButton).forEach(function (b) {
        if (!allowBtn) allowBtn = { el: b, label: textOf(b) || 'Allow' };
      });
      qsAll(cfg.approvalRejectButton).forEach(function (b) {
        if (!rejectBtn) rejectBtn = { el: b, label: textOf(b) || 'Reject' };
      });
      var dialog = dialogs[0] || null;
      var hasDialog = !!dialog;
      var hasAllow = !!allowBtn;
      var hasReject = !!rejectBtn;
      // Gate: an approval needs a dialog, or an allow+reject pair. A lone "确认" button
      // plus some code block elsewhere on the page is not an approval.
      if (!hasDialog && !(hasAllow && hasReject)) return null;
      // Only look inside the approval container, never the whole page.
      var scope = dialog || commonAncestor(allowBtn.el, rejectBtn.el);
      if (!scope) return null;
      var scopeText = textOf(scope);
      var waiting = WAIT_RE.test(scopeText);
      var command = '';
      var pre = scope.querySelector('pre,code,[data-testid*="command"],[data-testid*="tool"]');
      if (pre && visible(pre)) command = textOf(pre);
      if (!command && scopeText) {
        var cm = scopeText.match(/\b(rm |git |npm |pnpm |sudo |chmod |curl |wget |python |pip )[\s\S]{0,180}/);
        if (cm) command = cm[0];
      }
      var score = 0;
      if (hasDialog) score++;
      if (hasAllow || hasReject) score++;
      if (waiting) score++;
      if (command) score++;
      if (score < 2) return null;
      var titleEl = scope.querySelector('h1,h2,h3,[data-testid*="title"]');
      var title = titleEl ? textOf(titleEl) : '需要确认';
      return {
        id: hash((command || scopeText || title).slice(0, 80)),
        title: title || '需要确认',
        description: sanitize(scopeText.slice(0, 180), 240),
        command: sanitize(command, 300),
        hasDialog: hasDialog,
        hasAllow: hasAllow,
        hasReject: hasReject,
        waitingContext: waiting,
        allowLabel: allowBtn ? allowBtn.label : '',
        rejectLabel: rejectBtn ? rejectBtn.label : ''
      };
    }

    function scanArtifacts() {
      var nodes = qsAll(cfg.artifact);
      var extra = Array.prototype.slice.call(document.querySelectorAll('a[href],img[src]')).filter(visible);
      extra.forEach(function (n) {
        var href = n.getAttribute('href') || n.getAttribute('src') || '';
        if (FILE_RE.test(href) || FILE_RE.test(n.getAttribute('download') || n.getAttribute('alt') || textOf(n))) nodes.push(n);
      });
      var out = [];
      var seen = {};
      nodes.forEach(function (n) {
        var href = n.getAttribute('href') || n.getAttribute('src') || '';
        var name = n.getAttribute('download') || n.getAttribute('aria-label') || textOf(n) || (href.split('/').pop() || '');
        name = sanitize(name, 80);
        if (!name || seen[name]) return;
        if (!FILE_RE.test(name) && !FILE_RE.test(href)) return;
        seen[name] = 1;
        out.push({ id: hash(name + href), name: name, href: sanitize(href.split('?')[0], 240), mime: '' });
      });
      return out.slice(0, 20);
    }

    function scanMessages() {
      var nodes = qsAll(cfg.agentMessage);
      return nodes.slice(-6).map(function (n) {
        var t = textOf(n);
        return { id: hash(t), text: t };
      }).filter(function (m) { return m.text.length > 0 && m.text.length < 400; });
    }

    function scanError() {
      var banner = qsAll(cfg.errorBanner)[0];
      if (!banner) return '';
      var text = textOf(banner);
      // Strip action labels ("重试", "Retry") so the error text is the message only.
      qsAll(cfg.errorAction).forEach(function (a) {
        if (banner.contains(a)) {
          var label = textOf(a);
          if (label) text = text.replace(label, '');
        }
      });
      return sanitize(text.trim(), 200);
    }

    function connectionHint(errorText) {
      var t = bodyText() + ' ' + (document.title || '');
      if (EXPIRED_RE.test(t)) return 'expired';
      if (errorText) return 'error';
      return 'ok';
    }

    function snapshot(reason) {
      bodyTextCache = '';
      var tasks = scanTasks();
      var errorText = scanError();
      var sessionTitle = tasks[0] ? tasks[0].title : sanitize(document.title, 80);
      var snap = {
        url: redactedUrl(),
        title: sanitize(document.title, 80),
        connectionHint: connectionHint(errorText),
        errorText: errorText,
        sessionId: hash(redactedUrl() + sessionTitle),
        sessionTitle: sessionTitle,
        tasks: tasks,
        approval: scanApproval(),
        artifacts: scanArtifacts(),
        messages: scanMessages(),
        observerActive: true
      };
      // Compare content only; timestamp/reason must not defeat the dedupe.
      var json = JSON.stringify(snap);
      if (json === lastJson) return;
      lastJson = json;
      snap.timestamp = Date.now();
      snap.reason = reason || 'mutation';
      if (bridge && bridge.onPageState) {
        try { bridge.onPageState(JSON.stringify(snap)); } catch (e) {}
      }
    }

    function schedule(reason) {
      if (timer) clearTimeout(timer);
      timer = setTimeout(function () { snapshot(reason); }, DEBOUNCE);
    }

    var obs = new MutationObserver(function () { schedule('mutation'); });
    if (document.documentElement) {
      obs.observe(document.documentElement, { subtree: true, childList: true, characterData: true });
    }
    window.__ZCodeScan = snapshot;
    window.addEventListener('popstate', function () { schedule('popstate'); });
    try {
      var ps = history.pushState;
      history.pushState = function () {
        ps.apply(this, arguments);
        schedule('pushState');
      };
    } catch (e) {}
    snapshot('install');
    return 'ok';
  } catch (err) {
    try {
      if (window.ZCodeAndroidBridge && window.ZCodeAndroidBridge.onEvent) {
        window.ZCodeAndroidBridge.onEvent(JSON.stringify({ type: 'Unknown', detail: 'observer-failed', timestamp: Date.now() }));
      }
    } catch (e) {}
    return 'error';
  }
})
