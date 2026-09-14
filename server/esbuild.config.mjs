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
  target: 'node24',
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
      name: 'fix-playwright-core-bundle',
      setup(build) {
        build.onLoad({ filter: /coreBundle\.js$/ }, async (args) => {
          let contents = fs.readFileSync(args.path, 'utf-8');
          // Replace dynamic require of browsers.json and package.json with static relative requires
          // so that esbuild can inline them into the bundle.
          contents = contents.replace(
            /require\([a-zA-Z0-9_]+\.default\.join\(packageRoot, "browsers\.json"\)\)/g,
            'require("../browsers.json")'
          );
          contents = contents.replace(
            /require\([a-zA-Z0-9_]+\.default\.join\(packageRoot, "package\.json"\)\)/g,
            'require("../package.json")'
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
