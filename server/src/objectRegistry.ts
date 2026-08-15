import { generateGuid } from './guidGenerator';

export class ObjectRegistry {
  private objects = new Map<string, any>();
  private types = new Map<string, string>();

  register(object: any, type: string, guid?: string): string {
    if (!guid) {
      const existing = this.findGuid(object);
      if (existing) return existing;
    }
    const id = guid || generateGuid(type);
    this.objects.set(id, object);
    this.types.set(id, type);
    return id;
  }

  get(guid: string): any {
    return this.objects.get(guid);
  }

  getType(guid: string): string | undefined {
    return this.types.get(guid);
  }

  remove(guid: string): void {
    this.objects.delete(guid);
    this.types.delete(guid);
  }

  has(guid: string): boolean {
    return this.objects.has(guid);
  }

  findGuid(object: any): string | undefined {
    for (const [guid, obj] of this.objects.entries()) {
      if (obj === object) return guid;
    }
    return undefined;
  }

  clear(): void {
    this.objects.clear();
    this.types.clear();
  }

  async closeAll(): Promise<void> {
    // Closing a Browser also closes its contexts. De-duplicate by object
    // identity so event callbacks or duplicate registrations cannot trigger
    // the same Playwright close operation more than once.
    const browsers = new Set<any>();
    const contexts = new Set<any>();
    for (const [guid, obj] of this.objects.entries()) {
      const type = this.types.get(guid);
      if (type === 'Browser') browsers.add(obj);
      if (type === 'BrowserContext') contexts.add(obj);
    }

    for (const obj of [...browsers, ...contexts]) {
      try {
        await obj.close();
      } catch (e) {
        // Browser/context may already be closed.
      }
    }
    this.objects.clear();
    this.types.clear();
  }
}
