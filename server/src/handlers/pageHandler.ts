import { ObjectRegistry } from '../objectRegistry';
import { EventMessage } from '../protocolHandler';

export async function PageHandler(
  registry: ObjectRegistry,
  guid: string,
  method: string,
  params: Record<string, any>,
  sendEvent: (event: EventMessage) => void
): Promise<any> {
  const page = registry.get(guid);
  if (!page) throw new Error(`Page not found: ${guid}`);

  switch (method) {
    case 'goto': {
      const options: Record<string, any> = {};
      if (params.timeout !== undefined) options.timeout = params.timeout;
      if (params.waitUntil) options.waitUntil = params.waitUntil;
      const response = await page.goto(params.url, options);
      if (!response) return null;
      const responseGuid = registry.register(response, 'Response');
      return { guid: responseGuid, type: 'Response' };
    }
    case 'reload': {
      const options: Record<string, any> = {};
      if (params.timeout !== undefined) options.timeout = params.timeout;
      if (params.waitUntil) options.waitUntil = params.waitUntil;
      const response = await page.reload(options);
      if (!response) return null;
      const responseGuid = registry.register(response, 'Response');
      return { guid: responseGuid, type: 'Response' };
    }
    case 'goBack': {
      const options: Record<string, any> = {};
      if (params.timeout !== undefined) options.timeout = params.timeout;
      if (params.waitUntil) options.waitUntil = params.waitUntil;
      const response = await page.goBack(options);
      if (!response) return null;
      const responseGuid = registry.register(response, 'Response');
      return { guid: responseGuid, type: 'Response' };
    }
    case 'goForward': {
      const options: Record<string, any> = {};
      if (params.timeout !== undefined) options.timeout = params.timeout;
      if (params.waitUntil) options.waitUntil = params.waitUntil;
      const response = await page.goForward(options);
      if (!response) return null;
      const responseGuid = registry.register(response, 'Response');
      return { guid: responseGuid, type: 'Response' };
    }
    case 'waitForLoadState': {
      const state = params.state || 'load';
      const options: Record<string, any> = {};
      if (params.timeout !== undefined) options.timeout = params.timeout;
      await page.waitForLoadState(state, options);
      return {};
    }
    case 'locator': {
      // Locator is a client-side concept on server too, but we create a real Playwright Locator
      const options: Record<string, any> = {};
      if (params.hasText) options.hasText = params.hasText;
      if (params.hasNotText) options.hasNotText = params.hasNotText;
      const locator = page.locator(params.selector, options);
      const locatorGuid = registry.register(locator, 'Locator');
      return { guid: locatorGuid, type: 'Locator', selector: params.selector };
    }
    case 'querySelector': {
      const element = await page.$(params.selector);
      if (!element) return null;
      const ehGuid = registry.register(element, 'ElementHandle');
      return { guid: ehGuid, type: 'ElementHandle' };
    }
    case 'querySelectorAll': {
      const elements = await page.$$(params.selector);
      return elements.map((el: any) => {
        const ehGuid = registry.register(el, 'ElementHandle');
        return { guid: ehGuid, type: 'ElementHandle' };
      });
    }
    case 'waitForSelector': {
      const options: Record<string, any> = {};
      if (params.state) options.state = params.state;
      if (params.timeout !== undefined) options.timeout = params.timeout;
      const element = await page.waitForSelector(params.selector, options);
      if (!element) return null;
      const ehGuid = registry.register(element, 'ElementHandle');
      return { guid: ehGuid, type: 'ElementHandle' };
    }
    case 'click': {
      const options: Record<string, any> = {};
      if (params.button) options.button = params.button;
      if (params.clickCount !== undefined) options.clickCount = params.clickCount;
      if (params.delay !== undefined) options.delay = params.delay;
      if (params.timeout !== undefined) options.timeout = params.timeout;
      await page.click(params.selector, options);
      return {};
    }
    case 'dblclick': {
      const options: Record<string, any> = {};
      if (params.button) options.button = params.button;
      if (params.delay !== undefined) options.delay = params.delay;
      if (params.timeout !== undefined) options.timeout = params.timeout;
      await page.dblclick(params.selector, options);
      return {};
    }
    case 'fill': {
      const options: Record<string, any> = {};
      if (params.timeout !== undefined) options.timeout = params.timeout;
      await page.fill(params.selector, params.value, options);
      return {};
    }
    case 'type': {
      const options: Record<string, any> = {};
      if (params.delay !== undefined) options.delay = params.delay;
      if (params.timeout !== undefined) options.timeout = params.timeout;
      await page.type(params.selector, params.text, options);
      return {};
    }
    case 'press': {
      const options: Record<string, any> = {};
      if (params.delay !== undefined) options.delay = params.delay;
      if (params.timeout !== undefined) options.timeout = params.timeout;
      await page.press(params.selector, params.key, options);
      return {};
    }
    case 'evaluate': {
      const result = await page.evaluate(params.expression, params.arg);
      return result === undefined ? null : result;
    }
    case 'evaluateHandle': {
      const handle = await page.evaluateHandle(params.expression, params.arg);
      const result = await handle.jsonValue();
      return result;
    }
    case 'screenshot': {
      const options: Record<string, any> = {};
      if (params.path) options.path = params.path;
      if (params.type) options.type = params.type;
      if (params.quality !== undefined) options.quality = params.quality;
      if (params.fullPage !== undefined) options.fullPage = params.fullPage;
      if (params.timeout !== undefined) options.timeout = params.timeout;
      const buffer = await page.screenshot(options);
      return buffer;
    }
    case 'pdf': {
      const options: Record<string, any> = {};
      if (params.path) options.path = params.path;
      if (params.format) options.format = params.format;
      if (params.printBackground !== undefined) options.printBackground = params.printBackground;
      const buffer = await page.pdf(options);
      return buffer;
    }
    case 'content': {
      return await page.content();
    }
    case 'setContent': {
      const options: Record<string, any> = {};
      if (params.timeout !== undefined) options.timeout = params.timeout;
      if (params.waitUntil) options.waitUntil = params.waitUntil;
      await page.setContent(params.html, options);
      return {};
    }
    case 'title': {
      return await page.title();
    }
    case 'url': {
      return page.url();
    }
    case 'close': {
      const options: Record<string, any> = {};
      if (params.runBeforeUnload !== undefined) options.runBeforeUnload = params.runBeforeUnload;
      await page.close(options);
      registry.remove(guid);
      return {};
    }
    case 'isClosed': {
      return page.isClosed();
    }
    case 'mainFrame': {
      const frame = page.mainFrame();
      let frameGuid = registry.findGuid(frame);
      if (!frameGuid) {
        frameGuid = registry.register(frame, 'Frame');
      }
      return { guid: frameGuid, type: 'Frame' };
    }
    case 'frames': {
      const frames = page.frames();
      return frames.map((f: any) => {
        let fGuid = registry.findGuid(f);
        if (!fGuid) {
          fGuid = registry.register(f, 'Frame');
        }
        return { guid: fGuid, type: 'Frame' };
      });
    }
    case 'setViewportSize': {
      await page.setViewportSize(params.viewportSize);
      return {};
    }
    case 'viewportSize': {
      return page.viewportSize();
    }
    case 'bringToFront': {
      await page.bringToFront();
      return {};
    }
    case 'setDefaultTimeout': {
      page.setDefaultTimeout(params.timeout);
      return {};
    }
    case 'setDefaultNavigationTimeout': {
      page.setDefaultNavigationTimeout(params.timeout);
      return {};
    }
    case 'addInitScript': {
      await page.addInitScript(params.script);
      return {};
    }
    default:
      throw new Error(`Unknown method Page.${method}`);
  }
}
