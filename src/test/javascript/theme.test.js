const { loadApp } = require('./setup/loadApp');

function click(document, id) {
  document.getElementById(id).dispatchEvent(new window.Event('click', { bubbles: true }));
}

describe('theme default (AC-4)', () => {
  test('defaults to dark when nothing is stored', async () => {
    const { document } = await loadApp();
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
  });

  test('stays dark even when the OS prefers light', async () => {
    const original = window.matchMedia;
    window.matchMedia = jest.fn().mockReturnValue({ matches: true, media: '(prefers-color-scheme: light)' });
    try {
      const { document } = await loadApp();
      expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
      expect(window.matchMedia).not.toHaveBeenCalled();
    } finally {
      window.matchMedia = original;
    }
  });
});

describe('theme toggle (AC-1)', () => {
  test('the toggle button exists and switches the theme on click', async () => {
    const { document } = await loadApp();
    expect(document.getElementById('theme-toggle')).not.toBeNull();
    click(document, 'theme-toggle');
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    click(document, 'theme-toggle');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
  });

  test('the label names the theme you will get next', async () => {
    const { document } = await loadApp();
    const button = document.getElementById('theme-toggle');
    expect(button.textContent).toContain('Light');
    click(document, 'theme-toggle');
    expect(button.textContent).toContain('Dark');
  });
});

describe('theme persistence (AC-3)', () => {
  test('restores a previously stored theme on load', async () => {
    const { document } = await loadApp({ storage: { 'ops-dashboard-theme': 'light' } });
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
  });

  test('clicking the toggle writes the new choice to storage', async () => {
    const { document, storage } = await loadApp();
    click(document, 'theme-toggle');
    expect(storage.getItem('ops-dashboard-theme')).toBe('light');
    click(document, 'theme-toggle');
    expect(storage.getItem('ops-dashboard-theme')).toBe('dark');
  });
});

describe('theme colours stay out of JavaScript (AC-2)', () => {
  test('chart elements carry no inline fill style, only classes', async () => {
    const { document } = await loadApp();
    click(document, 'theme-toggle');
    const styled = document.querySelectorAll('#chart-on-time [style], #chart-tickets [style]');
    expect(styled).toHaveLength(0);
  });
});
