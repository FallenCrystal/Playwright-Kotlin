import { ObjectRegistry } from '../objectRegistry';
import { EventMessage } from '../protocolHandler';
import { resolveEvalArg, evaluateExpression } from '../serializer';

export async function ElementHandleHandler(
  registry: ObjectRegistry,
  guid: string,
  method: string,
  params: Record<string, any>,
  sendEvent: (event: EventMessage) => void
): Promise<any> {
  const element = registry.get(guid);
  if (!element) throw new Error(`ElementHandle not found: ${guid}`);

  switch (method) {
    case 'click': {
      const options: Record<string, any> = {};
      if (params.button) options.button = params.button;
      if (params.clickCount !== undefined) options.clickCount = params.clickCount;
      if (params.delay !== undefined) options.delay = params.delay;
      if (params.timeout !== undefined) options.timeout = params.timeout;
      if (params.force !== undefined) options.force = params.force;
      await element.click(options);
      return {};
    }
    case 'dblclick': {
      const options: Record<string, any> = {};
      if (params.button) options.button = params.button;
      if (params.delay !== undefined) options.delay = params.delay;
      if (params.timeout !== undefined) options.timeout = params.timeout;
      await element.dblclick(options);
      return {};
    }
    case 'fill': {
      const options: Record<string, any> = {};
      if (params.timeout !== undefined) options.timeout = params.timeout;
      await element.fill(params.value, options);
      return {};
    }
    case 'type': {
      const options: Record<string, any> = {};
      if (params.delay !== undefined) options.delay = params.delay;
      if (params.timeout !== undefined) options.timeout = params.timeout;
      await element.type(params.text, options);
      return {};
    }
    case 'press': {
      const options: Record<string, any> = {};
      if (params.delay !== undefined) options.delay = params.delay;
      if (params.timeout !== undefined) options.timeout = params.timeout;
      await element.press(params.key, options);
      return {};
    }
    case 'textContent': {
      return await element.textContent();
    }
    case 'innerText': {
      return await element.innerText();
    }
    case 'innerHTML': {
      return await element.innerHTML();
    }
    case 'getAttribute': {
      return await element.getAttribute(params.name);
    }
    case 'isVisible': {
      return await element.isVisible();
    }
    case 'isHidden': {
      return await element.isHidden();
    }
    case 'isEnabled': {
      return await element.isEnabled();
    }
    case 'isDisabled': {
      return await element.isDisabled();
    }
    case 'boundingBox': {
      return await element.boundingBox();
    }
    case 'screenshot': {
      const options: Record<string, any> = {};
      if (params.path) options.path = params.path;
      if (params.type) options.type = params.type;
      if (params.quality !== undefined) options.quality = params.quality;
      if (params.timeout !== undefined) options.timeout = params.timeout;
      return await element.screenshot(options);
    }
    case 'hover': {
      const options: Record<string, any> = {};
      if (params.timeout !== undefined) options.timeout = params.timeout;
      if (params.force !== undefined) options.force = params.force;
      await element.hover(options);
      return {};
    }
    case 'focus': {
      await element.focus();
      return {};
    }
    case 'evaluate': {
      const arg = params.arg !== undefined ? resolveEvalArg(params.arg, registry) : undefined;
      return await evaluateExpression(element, params.expression, arg);
    }
    case 'querySelector': {
      const child = await element.$(params.selector);
      if (!child) return null;
      const childGuid = registry.register(child, 'ElementHandle');
      return { guid: childGuid, type: 'ElementHandle' };
    }
    case 'querySelectorAll': {
      const children = await element.$$(params.selector);
      return children.map((c: any) => {
        const cGuid = registry.register(c, 'ElementHandle');
        return { guid: cGuid, type: 'ElementHandle' };
      });
    }
    case 'dispose': {
      await element.dispose();
      registry.remove(guid);
      return {};
    }
    case 'contentFrame': {
      const frame = await element.contentFrame();
      if (!frame) return null;
      let frameGuid = registry.findGuid(frame);
      if (!frameGuid) {
        frameGuid = registry.register(frame, 'Frame');
      }
      return { guid: frameGuid, type: 'Frame' };
    }
    default:
      throw new Error(`Unknown method ElementHandle.${method}`);
  }
}
