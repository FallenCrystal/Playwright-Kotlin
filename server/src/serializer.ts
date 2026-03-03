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
