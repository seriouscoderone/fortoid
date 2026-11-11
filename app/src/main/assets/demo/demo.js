// Wiring for the demo page: each [data-call] button runs one native capability
// and shows the JSON result (or error) in the card's output area.
(function () {
  'use strict';

  function show(out, ok, value) {
    out.className = 'out ' + (ok ? 'ok' : 'err');
    out.textContent = typeof value === 'string' ? value : JSON.stringify(value, null, 2);
  }

  // Cards can register a params builder (e.g. to reuse a key alias); default is {}.
  window.DemoParams = window.DemoParams || {};
  // Cards can register a result hook to post-process a successful result.
  window.DemoResults = window.DemoResults || {};

  window.DemoParams['biometric-auth'] = function () {
    return { title: 'Unlock Fort Demo', subtitle: 'Biometric check from the WebView' };
  };

  document.querySelectorAll('button[data-call]').forEach(function (button) {
    button.addEventListener('click', function () {
      var capability = button.getAttribute('data-call');
      var out = button.closest('.card').querySelector('pre.out');
      var build = window.DemoParams[button.id];
      var hook = window.DemoResults[button.id];

      button.disabled = true;
      show(out, true, 'Calling ' + capability + '…');

      Promise.resolve()
        .then(function () { return build ? build() : {}; })
        .then(function (params) { return FortNative.call(capability, params); })
        .then(function (result) { return hook ? hook(result) : result; })
        .then(function (result) { show(out, true, result); })
        .catch(function (error) {
          show(out, false, (error.code ? '[' + error.code + '] ' : '') + error.message);
        })
        .finally(function () { button.disabled = false; });
    });
  });

  document.getElementById('bridge-status').textContent = FortNative.available
    ? 'Native bridge connected.'
    : 'Native bridge not found. Open this page from the Fort Demo app icon.';
})();
