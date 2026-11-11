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

  var KEY_ALIAS = 'demo';

  function bytesToBase64(bytes) {
    var binary = '';
    bytes.forEach(function (b) { binary += String.fromCharCode(b); });
    return btoa(binary);
  }

  function base64ToBytes(text) {
    return Uint8Array.from(atob(text), function (c) { return c.charCodeAt(0); });
  }

  function messageBytes() {
    return new TextEncoder().encode(document.getElementById('sign-message').value);
  }

  window.DemoParams['keystore-generate'] = function () { return { alias: KEY_ALIAS }; };
  window.DemoParams['keystore-delete'] = function () { return { alias: KEY_ALIAS }; };
  window.DemoParams['keystore-sign'] = function () {
    return { alias: KEY_ALIAS, data: bytesToBase64(messageBytes()), subtitle: 'Fort Demo wants to sign a message' };
  };

  // Independently verify the hardware signature in the WebView with WebCrypto.
  window.DemoResults['keystore-sign'] = function (result) {
    return FortNative.call('keystore.publicKey', { alias: KEY_ALIAS })
      .then(function (info) {
        return crypto.subtle.importKey(
          'spki', base64ToBytes(info.publicKey), { name: 'ECDSA', namedCurve: 'P-256' }, false, ['verify']);
      })
      .then(function (key) {
        return crypto.subtle.verify(
          { name: 'ECDSA', hash: 'SHA-256' }, key, base64ToBytes(result.signatureP1363), messageBytes());
      })
      .then(function (verified) {
        return Object.assign({ verifiedByWebCrypto: verified }, result);
      });
  };

  var SECRET_KEY = 'demo';
  window.DemoParams['secret-store'] = function () {
    return { key: SECRET_KEY, value: document.getElementById('secret-value').value };
  };
  window.DemoParams['secret-retrieve'] = function () { return { key: SECRET_KEY }; };
  window.DemoParams['secret-status'] = function () { return { key: SECRET_KEY }; };
  window.DemoParams['secret-delete'] = function () { return { key: SECRET_KEY }; };

  window.DemoParams['qr-scan'] = function () {
    return { prompt: 'Scan a QR code (try a KERI OOBI)' };
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
