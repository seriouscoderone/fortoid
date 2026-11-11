// FortNative: promise-based client for Fortoid's native capability bridge.
//
//   const info = await FortNative.call('device.info');
//
// Requests go out over window.bridge (installed by the Android host for the
// trusted app origin only); responses come back on the same channel.
(function () {
  'use strict';

  var bridge = window.bridge;
  var pending = new Map();
  var sequence = 0;

  function onMessage(event) {
    var message;
    try {
      message = JSON.parse(event.data);
    } catch (e) {
      return;
    }
    if (!message || message.type !== 'native_response') return;

    var entry = pending.get(message.id);
    if (!entry) return;
    pending.delete(message.id);

    if (message.ok) {
      entry.resolve(message.result);
    } else {
      var error = new Error((message.error && message.error.message) || 'native call failed');
      error.code = message.error && message.error.code;
      entry.reject(error);
    }
  }

  if (bridge) {
    bridge.addEventListener('message', onMessage);
  }

  window.FortNative = {
    available: !!bridge,

    call: function (capability, params) {
      if (!bridge) {
        var error = new Error('Native bridge not available (not running inside Fortoid)');
        error.code = 'unavailable';
        return Promise.reject(error);
      }
      var id = 'n' + (++sequence) + '-' + Date.now().toString(36);
      return new Promise(function (resolve, reject) {
        pending.set(id, { resolve: resolve, reject: reject });
        bridge.postMessage(JSON.stringify({
          type: 'native_request',
          id: id,
          capability: capability,
          params: params || {}
        }));
      });
    }
  };
})();
