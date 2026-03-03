let counter = 0;

export function generateGuid(type: string): string {
  counter++;
  return `${type.toLowerCase()}-${counter.toString(36)}-${Date.now().toString(36)}`;
}

export function resetCounter(): void {
  counter = 0;
}
