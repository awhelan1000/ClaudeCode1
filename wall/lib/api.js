// Build Wall client for the Supabase REST API (PostgREST). Loaded by every page after config.js.
(function () {
  const cfg = window.BUILD_WALL_CONFIG || {};
  const base = String(cfg.supabaseUrl || '').replace(/\/+$/, '');
  const key = String(cfg.supabaseAnonKey || '');
  const configured = /^https?:\/\//.test(base) && key.length > 20 && !/YOUR-/.test(base + key);
  function csv(rows) {
    const cell = (v) => { let s = String(v == null ? '' : v); if (/^[=+\-@\t\r]/.test(s)) s = "'" + s; return '"' + s.replace(/"/g, '""') + '"'; };
    const lines = ['name,answer,submitted_at,hidden'];
    for (const r of rows || []) lines.push([cell(r.name), cell(r.text), cell(r.created_at), r.hidden ? 'yes' : 'no'].join(','));
    return lines.join('\r\n') + '\r\n';
  }
  const uuid = () => (crypto.randomUUID ? crypto.randomUUID() : 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => { const r = Math.random() * 16 | 0; return (c === 'x' ? r : (r & 3 | 8)).toString(16); }));
  // Legacy anon keys are JWTs and also go in Authorization; new publishable keys (sb_publishable_…) go in apikey only.
  const baseHeaders = { apikey: key, 'content-type': 'application/json' };
  if (/^eyJ/.test(key)) baseHeaders.Authorization = 'Bearer ' + key;
  const headers = (extra) => Object.assign({}, baseHeaders, extra || {});

  async function call(path, opts) {
    opts = opts || {};
    const r = await fetch(base + path, { method: opts.method || 'GET', headers: headers(opts.headers), body: opts.body, cache: 'no-store' });
    if (r.status === 204) return null;
    const text = await r.text();
    let data = null; try { data = JSON.parse(text); } catch (e) {}
    if (!r.ok) {
      const msg = (data && (data.message || data.error_description || data.error || data.hint)) || ('Request failed (' + r.status + ')');
      const err = new Error(String(msg).replace(/^P0001:\s*/, '')); err.status = r.status; throw err;
    }
    return data;
  }
  const rpc = (fn, args) => call('/rest/v1/rpc/' + fn, { method: 'POST', body: JSON.stringify(args) });
  const q = (v) => encodeURIComponent(v);

  // ---- Google Apps Script backend (a Sheet in Drive) ------------------------
  const gsUrl = String(cfg.appsScriptUrl || '').replace(/\/+$/, '');
  if (/^https:\/\/script\.google\.com\/macros\/s\/.+\/exec$/.test(gsUrl)) {
    const unwrap = async (r) => {
      const text = await r.text(); let data = null; try { data = JSON.parse(text); } catch (e) {}
      if (!data || data.error) { const err = new Error((data && data.error) || ('Request failed (' + r.status + ')')); err.status = r.status || 400; throw err; }
      return data;
    };
    const get = (params) => fetch(gsUrl + '?' + new URLSearchParams(params).toString(), { cache: 'no-store' }).then(unwrap);
    // No content-type header on purpose: a "simple" POST needs no CORS preflight, which Apps Script cannot answer.
    const post = (body) => fetch(gsUrl, { method: 'POST', body: JSON.stringify(body) }).then(unwrap);
    window.BuildWall = {
      configured: true,
      list: (session) => get({ action: 'wall', session }).then((w) => w.answers),
      stats: (session) => get({ action: 'stats', session }),
      wall: (session) => get({ action: 'wall', session }),
      add: (row) => post(Object.assign({ action: 'add' }, row)),
      removeOwn: (id, client) => post({ action: 'remove', id, client }).then((r) => r.ok),
      presenterOk: (k) => post({ action: 'presenter_ok', key: k }).then((r) => r.ok === true),
      hide: (k, id) => post({ action: 'hide', key: k, id }).then((r) => r.ok),
      reset: (k, session) => post({ action: 'reset', key: k, session }).then((r) => r.cleared),
      exportRows: (k, session) => post({ action: 'export', key: k, session }).then((r) => r.rows),
      csv: csv, uuid: uuid,
    };
    return;
  }

  window.BuildWall = {
    configured,
    list: (session) => call('/rest/v1/wall?session=eq.' + q(session) + '&order=created_at.asc&select=id,name,text,created_at&limit=500'),
    stats: async (session) => { const rows = await call('/rest/v1/wall_stats?session=eq.' + q(session)); return (rows && rows[0]) || { count: 0, people: 0 }; },
    add: (row) => call('/rest/v1/answers', { method: 'POST', headers: { Prefer: 'return=minimal' }, body: JSON.stringify(row) }),
    removeOwn: (id, client) => rpc('remove_own', { p_id: id, p_client: client }),
    presenterOk: (k) => rpc('presenter_ok', { p_key: k }),
    hide: (k, id) => rpc('presenter_hide', { p_key: k, p_id: id }),
    reset: (k, session) => rpc('presenter_reset', { p_key: k, p_session: session }),
    exportRows: (k, session) => rpc('presenter_export', { p_key: k, p_session: session }),
    wall: async (session) => { const [answers, stats] = await Promise.all([window.BuildWall.list(session), window.BuildWall.stats(session)]); return { answers, count: stats.count, people: stats.people }; },
    csv: csv, uuid: uuid,
  };
})();
