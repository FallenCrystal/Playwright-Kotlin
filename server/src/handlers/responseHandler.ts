import { ObjectRegistry } from '../objectRegistry';
import { EventMessage } from '../protocolHandler';

export async function ResponseHandler(
  registry: ObjectRegistry,
  guid: string,
  method: string,
  params: Record<string, any>,
  sendEvent: (event: EventMessage) => void
): Promise<any> {
  const response = registry.get(guid);
  if (!response) throw new Error(`Response not found: ${guid}`);

  switch (method) {
    case 'status': {
      return response.status();
    }
    case 'statusText': {
      return response.statusText();
    }
    case 'url': {
      return response.url();
    }
    case 'ok': {
      return response.ok();
    }
    case 'headers': {
      return response.headers();
    }
    case 'body': {
      return await response.body();
    }
    case 'text': {
      return await response.text();
    }
    case 'json': {
      return await response.json();
    }
    default:
      throw new Error(`Unknown method Response.${method}`);
  }
}
