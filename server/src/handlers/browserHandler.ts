import { ObjectRegistry } from '../objectRegistry';
import { EventMessage } from '../protocolHandler';

export async function BrowserHandler(
  registry: ObjectRegistry,
  guid: string,
  method: string,
  params: Record<string, any>,
  sendEvent: (event: EventMessage) => void
): Promise<any> {
  const browser = registry.get(guid);
  if (!browser) throw new Error(`Browser not found: ${guid}`);

  switch (method) {
    case 'newContext': {
      const options: Record<string, any> = {};
      if (params.viewport) options.viewport = params.viewport;
      if (params.userAgent) options.userAgent = params.userAgent;
      if (params.locale) options.locale = params.locale;
      if (params.timezoneId) options.timezoneId = params.timezoneId;
      if (params.ignoreHTTPSErrors !== undefined) options.ignoreHTTPSErrors = params.ignoreHTTPSErrors;
      if (params.javaScriptEnabled !== undefined) options.javaScriptEnabled = params.javaScriptEnabled;
      if (params.bypassCSP !== undefined) options.bypassCSP = params.bypassCSP;
      if (params.deviceScaleFactor !== undefined) options.deviceScaleFactor = params.deviceScaleFactor;

      const context = await browser.newContext(options);
      const contextGuid = registry.register(context, 'BrowserContext');

      context.on('close', () => {
        sendEvent({
          guid: contextGuid,
          method: '__event__',
          params: { type: 'close' },
        });
        registry.remove(contextGuid);
      });

      return { guid: contextGuid, type: 'BrowserContext' };
    }
    case 'newPage': {
      const options: Record<string, any> = {};
      if (params.viewport) options.viewport = params.viewport;
      if (params.userAgent) options.userAgent = params.userAgent;
      if (params.locale) options.locale = params.locale;
      if (params.ignoreHTTPSErrors !== undefined) options.ignoreHTTPSErrors = params.ignoreHTTPSErrors;

      // newPage creates context + page
      const context = await browser.newContext(options);
      const page = await context.newPage();
      const contextGuid = registry.register(context, 'BrowserContext');
      const pageGuid = registry.register(page, 'Page');

      // Register the main frame
      const mainFrame = page.mainFrame();
      const frameGuid = registry.register(mainFrame, 'Frame');

      page.on('close', () => {
        sendEvent({
          guid: pageGuid,
          method: '__event__',
          params: { type: 'close' },
        });
        registry.remove(pageGuid);
      });

      return { guid: pageGuid, type: 'Page', contextGuid, frameGuid };
    }
    case 'close': {
      await browser.close();
      registry.remove(guid);
      return {};
    }
    case 'contexts': {
      const contexts = browser.contexts();
      const result = contexts.map((ctx: any) => {
        let ctxGuid = registry.findGuid(ctx);
        if (!ctxGuid) {
          ctxGuid = registry.register(ctx, 'BrowserContext');
        }
        return { guid: ctxGuid, type: 'BrowserContext' };
      });
      return result;
    }
    case 'isConnected': {
      return browser.isConnected();
    }
    case 'version': {
      return browser.version();
    }
    default:
      throw new Error(`Unknown method Browser.${method}`);
  }
}
