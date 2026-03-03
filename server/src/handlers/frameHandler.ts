import { ObjectRegistry } from '../objectRegistry';
import { EventMessage } from '../protocolHandler';
import { resolveEvalArg, evaluateExpression } from '../serializer';

export async function FrameHandler(
  registry: ObjectRegistry,
  guid: string,
  method: string,
  params: Record<string, any>,
  sendEvent: (event: EventMessage) => void
): Promise<any> {
  const frame = registry.get(guid);
  if (!frame) throw new Error(`Frame not found: ${guid}`);

  switch (method) {
    case 'goto': {
      const options: Record<string, any> = {};
      if (params.timeout !== undefined) options.timeout = params.timeout;
      if (params.waitUntil) options.waitUntil = params.waitUntil;
      const response = await frame.goto(params.url, options);
      if (!response) return null;
      const responseGuid = registry.register(response, 'Response');
      return { guid: responseGuid, type: 'Response' };
    }
    case 'waitForLoadState': {
      const state = params.state || 'load';
      const options: Record<string, any> = {};
      if (params.timeout !== undefined) options.timeout = params.timeout;
      await frame.waitForLoadState(state, options);
      return {};
    }
    case 'locator': {
      const options: Record<string, any> = {};
      if (params.hasText) options.hasText = params.hasText;
      if (params.hasNotText) options.hasNotText = params.hasNotText;
      const locator = frame.locator(params.selector, options);
      const locatorGuid = registry.register(locator, 'Locator');
      return { guid: locatorGuid, type: 'Locator', selector: params.selector };
    }
    case 'querySelector': {
      const element = await frame.$(params.selector);
      if (!element) return null;
      const ehGuid = registry.register(element, 'ElementHandle');
      return { guid: ehGuid, type: 'ElementHandle' };
    }
    case 'querySelectorAll': {
      const elements = await frame.$$(params.selector);
      return elements.map((el: any) => {
        const ehGuid = registry.register(el, 'ElementHandle');
        return { guid: ehGuid, type: 'ElementHandle' };
      });
    }
    case 'waitForSelector': {
      const options: Record<string, any> = {};
      if (params.state) options.state = params.state;
      if (params.timeout !== undefined) options.timeout = params.timeout;
      const element = await frame.waitForSelector(params.selector, options);
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
      await frame.click(params.selector, options);
      return {};
    }
    case 'fill': {
      const options: Record<string, any> = {};
      if (params.timeout !== undefined) options.timeout = params.timeout;
      await frame.fill(params.selector, params.value, options);
      return {};
    }
    case 'type': {
      const options: Record<string, any> = {};
      if (params.delay !== undefined) options.delay = params.delay;
      if (params.timeout !== undefined) options.timeout = params.timeout;
      await frame.type(params.selector, params.text, options);
      return {};
    }
    case 'press': {
      const options: Record<string, any> = {};
      if (params.delay !== undefined) options.delay = params.delay;
      if (params.timeout !== undefined) options.timeout = params.timeout;
      await frame.press(params.selector, params.key, options);
      return {};
    }
    case 'evaluate': {
      const arg = params.arg !== undefined ? resolveEvalArg(params.arg, registry) : undefined;
      const result = await evaluateExpression(frame, params.expression, arg);
      return result;
    }
    case 'content': {
      return await frame.content();
    }
    case 'setContent': {
      const options: Record<string, any> = {};
      if (params.timeout !== undefined) options.timeout = params.timeout;
      if (params.waitUntil) options.waitUntil = params.waitUntil;
      await frame.setContent(params.html, options);
      return {};
    }
    case 'title': {
      return await frame.title();
    }
    case 'url': {
      return frame.url();
    }
    case 'name': {
      return frame.name();
    }
    case 'isDetached': {
      return frame.isDetached();
    }
    case 'parentFrame': {
      const parent = frame.parentFrame();
      if (!parent) return null;
      let parentGuid = registry.findGuid(parent);
      if (!parentGuid) {
        parentGuid = registry.register(parent, 'Frame');
      }
      return { guid: parentGuid, type: 'Frame' };
    }
    case 'childFrames': {
      const children = frame.childFrames();
      return children.map((f: any) => {
        let fGuid = registry.findGuid(f);
        if (!fGuid) {
          fGuid = registry.register(f, 'Frame');
        }
        return { guid: fGuid, type: 'Frame' };
      });
    }
    default:
      throw new Error(`Unknown method Frame.${method}`);
  }
}
