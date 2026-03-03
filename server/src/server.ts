import * as net from 'net';
import { ObjectRegistry } from './objectRegistry';
import { ProtocolHandler, Request, EventMessage } from './protocolHandler';
import { chromium, firefox, webkit } from 'playwright';

export interface ServerOptions {
  standalone?: boolean;
}

export function createServer(port: number, options?: ServerOptions): net.Server {
  const standalone = options?.standalone ?? false;
  const connections = new Set<net.Socket>();

  const server = net.createServer((socket) => {
    console.log(`[server] Client connected from ${socket.remoteAddress}:${socket.remotePort}`);
    connections.add(socket);

    // Enable TCP keepalive with a short interval so the OS detects dead
    // connections quickly (e.g. when the JVM is force-killed on Windows and
    // no FIN/RST is sent).  After 5 s idle the first probe is sent; the OS
    // default retry interval (1 s on Windows, 75 s on Linux) and retry count
    // (10 on Windows, 9 on Linux) then determine the total detection time.
    socket.setKeepAlive(true, 5_000);

    // Application-level liveness probe: periodically write an empty line (\n)
    // to the socket.  Both sides already skip empty NDJSON lines, so this is
    // invisible to the protocol.  If the peer (JVM) is dead, the write will
    // fail and we destroy the socket, which triggers the 'close' handler that
    // cleans up browsers and exits.
    const heartbeat = setInterval(() => {
      if (socket.destroyed || !socket.writable) {
        clearInterval(heartbeat);
        socket.destroy();
        return;
      }
      socket.write('\n', (err) => {
        if (err) {
          console.log('[server] Heartbeat write failed, destroying socket:', err.message);
          clearInterval(heartbeat);
          socket.destroy();
        }
      });
    }, 3_000);
    heartbeat.unref();

    const registry = new ObjectRegistry();
    let buffer = '';

    // Pre-register playwright instance and browser types
    const pw = { chromium, firefox, webkit };
    registry.register(pw, 'Playwright', 'playwright');
    registry.register(chromium, 'BrowserType', 'browser-type-chromium');
    registry.register(firefox, 'BrowserType', 'browser-type-firefox');
    registry.register(webkit, 'BrowserType', 'browser-type-webkit');

    const sendEvent = (event: EventMessage) => {
      try {
        const line = JSON.stringify(event) + '\n';
        socket.write(line);
      } catch (e) {
        console.error('[server] Failed to send event:', e);
      }
    };

    const handler = new ProtocolHandler(registry, sendEvent);

    socket.on('data', (data) => {
      buffer += data.toString();
      const lines = buffer.split('\n');
      buffer = lines.pop() || '';

      for (const line of lines) {
        if (line.trim() === '') continue;
        try {
          const request: Request = JSON.parse(line);
          handler.handleRequest(request).then((response) => {
            try {
              const responseLine = JSON.stringify(response) + '\n';
              socket.write(responseLine);
            } catch (e) {
              console.error('[server] Failed to send response:', e);
            }
          });
        } catch (e) {
          console.error('[server] Failed to parse request:', line, e);
        }
      }
    });

    socket.on('close', () => {
      console.log('[server] Client disconnected');
      clearInterval(heartbeat);
      connections.delete(socket);

      // Force-exit safety net: if closeAll() hangs (e.g. browser unresponsive),
      // don't keep the server alive forever.
      const forceExitTimer = setTimeout(() => {
        console.log('[server] Force-exiting after cleanup timeout');
        process.exit(1);
      }, 10_000);
      forceExitTimer.unref();

      registry.closeAll().catch((e) => {
        console.error('[server] Error closing browsers:', e);
      }).finally(() => {
        clearTimeout(forceExitTimer);
        if (!standalone && connections.size === 0) {
          console.log('[server] Last client disconnected, shutting down');
          server.close();
          process.exit(0);
        }
      });
    });

    socket.on('error', (err) => {
      console.error('[server] Socket error:', err.message);
    });
  });

  server.listen(port, '127.0.0.1', () => {
    const addr = server.address();
    const actualPort = (addr && typeof addr === 'object') ? addr.port : port;
    console.log(`LISTENING:${actualPort}`);
  });

  server.on('error', (err) => {
    console.error('[server] Server error:', err.message);
    process.exit(1);
  });

  return server;
}
