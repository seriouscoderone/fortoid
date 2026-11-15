The wallet for a computer
=========================

This is the same wallet that is in the phone app, for a desktop browser.
It is a demonstration build. Do not use it for anything you depend on.

To start it
-----------

1. Open a terminal in this folder.
2. Run:    python3 start-wallet.py
   (on Windows:    py start-wallet.py)
3. The wallet opens in your browser at http://127.0.0.1:8791/

Leave the terminal open while you use the wallet. Press Ctrl+C in it to stop.
You need Python 3 and nothing else.

Good to know
------------

- Use Chrome or Edge. If the wallet opens in another browser, copy the address
  into Chrome or Edge. Other browsers are not tested, and Safari removes a
  site's storage after a week without use, which would remove your vault.
- Use one tab. A second tab is told that the wallet is already open.
- Use a browser profile with no extensions. An extension that may read pages
  can read a wallet.
- The wallet lives at one address: http://127.0.0.1:8791/. Your vault is kept
  by the browser under that address. If something else on this computer is
  using port 8791 the script says so and stops. It does not pick another port,
  because another address would be an empty wallet. Do not open the address
  while the script is not running: whatever holds the port would answer there.
- The browser decides whether it keeps the wallet's storage for good. Chrome
  and Edge usually say no to a site they have just met. The wallet asks again
  each time you open your vault. Settings shows the answer as
  "Kept by the Browser".
- Set the computer's clock to automatic time.
- Use the phone app and this download from the same release, so both are the
  same version.
- The start script sends nothing anywhere. It serves this folder, and only to
  this computer.
