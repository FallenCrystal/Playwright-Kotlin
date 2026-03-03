import { ObjectRegistry } from '../objectRegistry';
import { EventMessage } from '../protocolHandler';

export async function BrowserContextHandler(
  registry: ObjectRegistry,
  guid: string,
  method: string,
  params: Record<string, any>,
  sendEvent: (event: EventMessage) => void
): Promise<any> {
  const context = registry.get(guid);
  if (!context) throw new Error(`BrowserContext not found: ${guid}`);

  switch (method) {
    case 'newPage': {
      const page = await context.newPage();
      const pageGuid = registry.register(page, 'Page');
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

      return { guid: pageGuid, type: 'Page', frameGuid };
    }
    case 'pages': {
      const pages = context.pages();
      const result = pages.map((p: any) => {
        let pGuid = registry.findGuid(p);
        if (!pGuid) {
          pGuid = registry.register(p, 'Page');
        }
        return { guid: pGuid, type: 'Page' };
      });
      return result;
    }
    case 'cookies': {
      const urls = params.urls;
      const cookies = urls ? await context.cookies(urls) : await context.cookies();
      return cookies;
    }
    case 'addCookies': {
      await context.addCookies(params.cookies);
      return {};
    }
    case 'clearCookies': {
      await context.clearCookies();
      return {};
    }
    case 'close': {
      await context.close();
      registry.remove(guid);
      return {};
    }
    case 'setDefaultTimeout': {
      context.setDefaultTimeout(params.timeout);
      return {};
    }
    case 'setDefaultNavigationTimeout': {
      context.setDefaultNavigationTimeout(params.timeout);
      return {};
    }
    case 'addInitScript': {
      await context.addInitScript(params.script);
      return {};
    }
    default:
      throw new Error(`Unknown method BrowserContext.${method}`);
  }
}
