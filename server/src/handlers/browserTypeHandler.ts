import { ObjectRegistry } from '../objectRegistry';
import { EventMessage } from '../protocolHandler';

export async function BrowserTypeHandler(
  registry: ObjectRegistry,
  guid: string,
  method: string,
  params: Record<string, any>,
  sendEvent: (event: EventMessage) => void
): Promise<any> {
  const browserType = registry.get(guid);
  if (!browserType) throw new Error(`BrowserType not found: ${guid}`);

  switch (method) {
    case 'launch': {
      const options: Record<string, any> = {};
      if (params.headless !== undefined) options.headless = params.headless;
      if (params.channel) options.channel = params.channel;
      if (params.executablePath) options.executablePath = params.executablePath;
      if (params.args) options.args = params.args;
      if (params.ignoreDefaultArgs !== undefined) options.ignoreDefaultArgs = params.ignoreDefaultArgs;
      if (params.timeout !== undefined) options.timeout = params.timeout;
      if (params.slowMo !== undefined) options.slowMo = params.slowMo;

      const browser = await browserType.launch(options);
      const browserGuid = registry.register(browser, 'Browser');

      browser.on('disconnected', () => {
        sendEvent({
          guid: browserGuid,
          method: '__event__',
          params: { type: 'disconnected' },
        });
        registry.remove(browserGuid);
      });

      return { guid: browserGuid, type: 'Browser' };
    }
    case 'launchPersistentContext': {
      const options: Record<string, any> = {};
      if (params.headless !== undefined) options.headless = params.headless;
      if (params.channel) options.channel = params.channel;
      if (params.executablePath) options.executablePath = params.executablePath;
      if (params.args) options.args = params.args;
      if (params.ignoreDefaultArgs !== undefined) options.ignoreDefaultArgs = params.ignoreDefaultArgs;
      if (params.timeout !== undefined) options.timeout = params.timeout;
      if (params.slowMo !== undefined) options.slowMo = params.slowMo;
      if (params.viewport !== undefined) options.viewport = params.viewport;
      if (params.userAgent) options.userAgent = params.userAgent;
      if (params.locale) options.locale = params.locale;
      if (params.timezoneId) options.timezoneId = params.timezoneId;
      if (params.ignoreHTTPSErrors !== undefined) options.ignoreHTTPSErrors = params.ignoreHTTPSErrors;
      if (params.javaScriptEnabled !== undefined) options.javaScriptEnabled = params.javaScriptEnabled;
      if (params.bypassCSP !== undefined) options.bypassCSP = params.bypassCSP;
      if (params.deviceScaleFactor !== undefined) options.deviceScaleFactor = params.deviceScaleFactor;

      const context = await browserType.launchPersistentContext(params.userDataDir, options);
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
    case 'name': {
      return browserType.name();
    }
    default:
      throw new Error(`Unknown method BrowserType.${method}`);
  }
}
