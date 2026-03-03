import { ObjectRegistry } from '../objectRegistry';
import { EventMessage } from '../protocolHandler';

export async function LocatorHandler(
  registry: ObjectRegistry,
  guid: string,
  method: string,
  params: Record<string, any>,
  sendEvent: (event: EventMessage) => void
): Promise<any> {
  const locator = registry.get(guid);
  if (!locator) throw new Error(`Locator not found: ${guid}`);

  switch (method) {
    case 'click': {
      const options: Record<string, any> = {};
      if (params.button) options.button = params.button;
      if (params.clickCount !== undefined) options.clickCount = params.clickCount;
      if (params.delay !== undefined) options.delay = params.delay;
      if (params.timeout !== undefined) options.timeout = params.timeout;
      if (params.force !== undefined) options.force = params.force;
      if (params.noWaitAfter !== undefined) options.noWaitAfter = params.noWaitAfter;
      await locator.click(options);
      return {};
    }
    case 'dblclick': {
      const options: Record<string, any> = {};
      if (params.button) options.button = params.button;
      if (params.delay !== undefined) options.delay = params.delay;
      if (params.timeout !== undefined) options.timeout = params.timeout;
      if (params.force !== undefined) options.force = params.force;
      await locator.dblclick(options);
      return {};
    }
    case 'fill': {
      const options: Record<string, any> = {};
      if (params.timeout !== undefined) options.timeout = params.timeout;
      if (params.force !== undefined) options.force = params.force;
      if (params.noWaitAfter !== undefined) options.noWaitAfter = params.noWaitAfter;
      await locator.fill(params.value, options);
      return {};
    }
    case 'type': {
      const options: Record<string, any> = {};
      if (params.delay !== undefined) options.delay = params.delay;
      if (params.timeout !== undefined) options.timeout = params.timeout;
      if (params.noWaitAfter !== undefined) options.noWaitAfter = params.noWaitAfter;
      await locator.type(params.text, options);
      return {};
    }
    case 'press': {
      const options: Record<string, any> = {};
      if (params.delay !== undefined) options.delay = params.delay;
      if (params.timeout !== undefined) options.timeout = params.timeout;
      if (params.noWaitAfter !== undefined) options.noWaitAfter = params.noWaitAfter;
      await locator.press(params.key, options);
      return {};
    }
    case 'textContent': {
      const options: Record<string, any> = {};
      if (params.timeout !== undefined) options.timeout = params.timeout;
      return await locator.textContent(options);
    }
    case 'innerText': {
      const options: Record<string, any> = {};
      if (params.timeout !== undefined) options.timeout = params.timeout;
      return await locator.innerText(options);
    }
    case 'innerHTML': {
      const options: Record<string, any> = {};
      if (params.timeout !== undefined) options.timeout = params.timeout;
      return await locator.innerHTML(options);
    }
    case 'inputValue': {
      const options: Record<string, any> = {};
      if (params.timeout !== undefined) options.timeout = params.timeout;
      return await locator.inputValue(options);
    }
    case 'getAttribute': {
      const options: Record<string, any> = {};
      if (params.timeout !== undefined) options.timeout = params.timeout;
      return await locator.getAttribute(params.name, options);
    }
    case 'isVisible': {
      const options: Record<string, any> = {};
      if (params.timeout !== undefined) options.timeout = params.timeout;
      return await locator.isVisible(options);
    }
    case 'isHidden': {
      const options: Record<string, any> = {};
      if (params.timeout !== undefined) options.timeout = params.timeout;
      return await locator.isHidden(options);
    }
    case 'isEnabled': {
      const options: Record<string, any> = {};
      if (params.timeout !== undefined) options.timeout = params.timeout;
      return await locator.isEnabled(options);
    }
    case 'isDisabled': {
      const options: Record<string, any> = {};
      if (params.timeout !== undefined) options.timeout = params.timeout;
      return await locator.isDisabled(options);
    }
    case 'isChecked': {
      const options: Record<string, any> = {};
      if (params.timeout !== undefined) options.timeout = params.timeout;
      return await locator.isChecked(options);
    }
    case 'check': {
      const options: Record<string, any> = {};
      if (params.timeout !== undefined) options.timeout = params.timeout;
      if (params.force !== undefined) options.force = params.force;
      await locator.check(options);
      return {};
    }
    case 'uncheck': {
      const options: Record<string, any> = {};
      if (params.timeout !== undefined) options.timeout = params.timeout;
      if (params.force !== undefined) options.force = params.force;
      await locator.uncheck(options);
      return {};
    }
    case 'hover': {
      const options: Record<string, any> = {};
      if (params.timeout !== undefined) options.timeout = params.timeout;
      if (params.force !== undefined) options.force = params.force;
      await locator.hover(options);
      return {};
    }
    case 'focus': {
      const options: Record<string, any> = {};
      if (params.timeout !== undefined) options.timeout = params.timeout;
      await locator.focus(options);
      return {};
    }
    case 'selectOption': {
      const options: Record<string, any> = {};
      if (params.timeout !== undefined) options.timeout = params.timeout;
      return await locator.selectOption(params.values, options);
    }
    case 'screenshot': {
      const options: Record<string, any> = {};
      if (params.path) options.path = params.path;
      if (params.type) options.type = params.type;
      if (params.quality !== undefined) options.quality = params.quality;
      if (params.timeout !== undefined) options.timeout = params.timeout;
      return await locator.screenshot(options);
    }
    case 'boundingBox': {
      const options: Record<string, any> = {};
      if (params.timeout !== undefined) options.timeout = params.timeout;
      return await locator.boundingBox(options);
    }
    case 'count': {
      return await locator.count();
    }
    case 'evaluate': {
      return await locator.evaluate(params.expression, params.arg);
    }
    case 'scrollIntoViewIfNeeded': {
      const options: Record<string, any> = {};
      if (params.timeout !== undefined) options.timeout = params.timeout;
      await locator.scrollIntoViewIfNeeded(options);
      return {};
    }
    case 'waitFor': {
      const options: Record<string, any> = {};
      if (params.state) options.state = params.state;
      if (params.timeout !== undefined) options.timeout = params.timeout;
      await locator.waitFor(options);
      return {};
    }
    // Sub-locator methods (client-side operations, but also supported server-side)
    case 'locator': {
      const options: Record<string, any> = {};
      if (params.hasText) options.hasText = params.hasText;
      if (params.hasNotText) options.hasNotText = params.hasNotText;
      const subLocator = locator.locator(params.selector, options);
      const subGuid = registry.register(subLocator, 'Locator');
      return { guid: subGuid, type: 'Locator', selector: params.selector };
    }
    case 'first': {
      const first = locator.first();
      const firstGuid = registry.register(first, 'Locator');
      return { guid: firstGuid, type: 'Locator' };
    }
    case 'last': {
      const last = locator.last();
      const lastGuid = registry.register(last, 'Locator');
      return { guid: lastGuid, type: 'Locator' };
    }
    case 'nth': {
      const nth = locator.nth(params.index);
      const nthGuid = registry.register(nth, 'Locator');
      return { guid: nthGuid, type: 'Locator' };
    }
    case 'all': {
      const all = await locator.all();
      return all.map((l: any) => {
        const lGuid = registry.register(l, 'Locator');
        return { guid: lGuid, type: 'Locator' };
      });
    }
    case 'elementHandle': {
      const handle = await locator.elementHandle();
      const handleGuid = registry.register(handle, 'ElementHandle');
      return { guid: handleGuid, type: 'ElementHandle' };
    }
    default:
      throw new Error(`Unknown method Locator.${method}`);
  }
}
