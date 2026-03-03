import { ObjectRegistry } from './objectRegistry';

export function serializeResult(value: any): any {
  if (value === null || value === undefined) {
    return null;
  }
  if (Buffer.isBuffer(value)) {
    return { type: 'Buffer', data: value.toString('base64') };
  }
  if (Array.isArray(value)) {
    return value.map(serializeResult);
  }
  if (typeof value === 'object') {
    const result: Record<string, any> = {};
    for (const [k, v] of Object.entries(value)) {
      result[k] = serializeResult(v);
    }
    return result;
  }
  return value;
}

export function deserializeArg(value: any): any {
  if (value === null || value === undefined) {
    return value;
  }
  if (typeof value === 'object' && value.type === 'Buffer' && typeof value.data === 'string') {
    return Buffer.from(value.data, 'base64');
  }
  if (Array.isArray(value)) {
    return value.map(deserializeArg);
  }
  if (typeof value === 'object') {
    const result: Record<string, any> = {};
    for (const [k, v] of Object.entries(value)) {
      result[k] = deserializeArg(v);
    }
    return result;
  }
  return value;
}

export function resolveEvalArg(value: any, registry: ObjectRegistry): any {
  if (value === null || value === undefined) {
    return value;
  }
  if (Array.isArray(value)) {
    return value.map(v => resolveEvalArg(v, registry));
  }
  if (typeof value === 'object') {
    if (typeof value.__pwGuid__ === 'string') {
      const obj = registry.get(value.__pwGuid__);
      if (!obj) throw new Error(`Object not found for guid: ${value.__pwGuid__}`);
      return obj;
    }
    const result: Record<string, any> = {};
    for (const [k, v] of Object.entries(value)) {
      result[k] = resolveEvalArg(v, registry);
    }
    return result;
  }
  return value;
}

function isFunctionExpression(expression: string): boolean {
  const trimmed = expression.trim();
  return trimmed.startsWith('function') ||
    trimmed.startsWith('async function') ||
    trimmed.startsWith('async (') ||
    trimmed.startsWith('(') ||
    /^\w+\s*=>/.test(trimmed);
}

export async function evaluateExpression(target: any, expression: string, arg?: any): Promise<any> {
  if (isFunctionExpression(expression)) {
    const fn = new Function('return (' + expression + ')')();
    return await target.evaluate(fn, arg);
  }
  return await target.evaluate(expression, arg);
}
