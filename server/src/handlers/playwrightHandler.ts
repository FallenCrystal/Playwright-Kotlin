import { ObjectRegistry } from '../objectRegistry';
import { EventMessage } from '../protocolHandler';

export async function PlaywrightHandler(
  registry: ObjectRegistry,
  guid: string,
  method: string,
  params: Record<string, any>,
  sendEvent: (event: EventMessage) => void
): Promise<any> {
  switch (method) {
    case 'initialize': {
      return {
        chromium: { guid: 'browser-type-chromium', type: 'BrowserType' },
        firefox: { guid: 'browser-type-firefox', type: 'BrowserType' },
        webkit: { guid: 'browser-type-webkit', type: 'BrowserType' },
      };
    }
    case 'close': {
      return {};
    }
    default:
      throw new Error(`Unknown method Playwright.${method}`);
  }
}
