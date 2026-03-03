import { createServer } from './server';

const args = process.argv.slice(2);

if (args.includes('--install-browsers')) {
  // Install browsers using playwright-core's registry API
  const { installBrowsersForNpmInstall } = require('playwright-core/lib/server/registry/index') as {
    installBrowsersForNpmInstall: () => Promise<void>
  };
  installBrowsersForNpmInstall()
    .then(() => {
      console.log('Browsers installed successfully.');
      process.exit(0);
    })
    .catch((err: Error) => {
      console.error('Failed to install browsers:', err.message);
      process.exit(1);
    });
} else {
  const port = parseInt(process.env.PORT || '0', 10);
  const standalone = args.includes('--standalone');

  const server = createServer(port, { standalone });

  // When the parent process (JVM) is killed, the stdin pipe breaks.
  // Detect this and exit so we don't leave an orphan Node.js process.
  if (!standalone) {
    process.stdin.resume();
    process.stdin.on('end', () => {
      console.log('[server] Parent process exited (stdin closed), shutting down...');
      server.close();
      process.exit(0);
    });
    process.stdin.on('error', () => {
      console.log('[server] Parent process exited (stdin error), shutting down...');
      server.close();
      process.exit(0);
    });
  }

  process.on('SIGINT', () => {
    console.log('[server] Shutting down...');
    server.close();
    process.exit(0);
  });

  process.on('SIGTERM', () => {
    console.log('[server] Shutting down...');
    server.close();
    process.exit(0);
  });
}
