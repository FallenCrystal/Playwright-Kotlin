import { ObjectRegistry } from './objectRegistry';
import { serializeResult } from './serializer';
import { PlaywrightHandler } from './handlers/playwrightHandler';
import { BrowserTypeHandler } from './handlers/browserTypeHandler';
import { BrowserHandler } from './handlers/browserHandler';
import { BrowserContextHandler } from './handlers/browserContextHandler';
import { PageHandler } from './handlers/pageHandler';
import { FrameHandler } from './handlers/frameHandler';
import { LocatorHandler } from './handlers/locatorHandler';
import { ElementHandleHandler } from './handlers/elementHandleHandler';
import { ResponseHandler } from './handlers/responseHandler';

export interface Request {
  id: number;
  guid: string;
  method: string;
  params: Record<string, any>;
}

export interface ResponseMessage {
  id: number;
  result?: any;
  error?: { message: string; name: string };
}

export interface EventMessage {
  guid: string;
  method: '__event__';
  params: { type: string; data?: any };
}

type Handler = (
  registry: ObjectRegistry,
  guid: string,
  method: string,
  params: Record<string, any>,
  sendEvent: (event: EventMessage) => void
) => Promise<any>;

const handlers: Record<string, Handler> = {
  Playwright: PlaywrightHandler,
  BrowserType: BrowserTypeHandler,
  Browser: BrowserHandler,
  BrowserContext: BrowserContextHandler,
  Page: PageHandler,
  Frame: FrameHandler,
  Locator: LocatorHandler,
  ElementHandle: ElementHandleHandler,
  Response: ResponseHandler,
};

export class ProtocolHandler {
  constructor(
    private registry: ObjectRegistry,
    private sendEvent: (event: EventMessage) => void
  ) {}

  async handleRequest(request: Request): Promise<ResponseMessage> {
    const { id, guid, method, params } = request;

    try {
      const type = this.registry.getType(guid);
      if (!type) {
        return {
          id,
          error: { message: `Object not found: ${guid}`, name: 'Error' },
        };
      }

      const handler = handlers[type];
      if (!handler) {
        return {
          id,
          error: { message: `No handler for type: ${type}`, name: 'Error' },
        };
      }

      const result = await handler(this.registry, guid, method, params || {}, this.sendEvent);
      return { id, result: serializeResult(result) };
    } catch (error: any) {
      return {
        id,
        error: {
          message: error.message || String(error),
          name: error.name || 'Error',
        },
      };
    }
  }
}
