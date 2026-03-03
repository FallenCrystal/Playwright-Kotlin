import { build } from 'esbuild';
import { builtinModules } from 'module';
import path from 'path';
import { fileURLToPath } from 'url';
import fs from 'fs';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

await build({
  entryPoints: ['dist/index.js'],
  bundle: true,
  platform: 'node',
  target: 'node22',
  outfile: 'bundle/server-bundle.js',
  format: 'cjs',
  external: [
    ...builtinModules,
    ...builtinModules.map(m => `node:${m}`),
    'chromium-bidi',
    'chromium-bidi/*',
  ],
  plugins: [
    {
      name: 'fix-playwright-core-resolve',
      setup(build) {
        // Patch nodePlatform.js to replace the problematic require.resolve
        // with a direct __dirname reference (since everything is bundled)
        const nodePlatformPath = path.resolve(
          __dirname, 'node_modules/playwright-core/lib/server/utils/nodePlatform.js'
        );
        build.onLoad({ filter: /nodePlatform\.js$/ }, async (args) => {
          if (args.path !== nodePlatformPath) return null;
          let contents = fs.readFileSync(args.path, 'utf-8');
          // Replace: const coreDir = path.dirname(require.resolve("../../../package.json"));
          // With: const coreDir = __dirname;  (since the bundle is a single file)
          contents = contents.replace(
            /const coreDir = .*require\.resolve\("\.\.\/\.\.\/\.\.\/package\.json"\).*\);/,
            'const coreDir = __dirname;'
          );
          return { contents, loader: 'js' };
        });
      },
    },
  ],
  minify: false,
  sourcemap: false,
});

console.log('Bundle created: bundle/server-bundle.js');
