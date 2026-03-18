// Ensure Compose Multiplatform processed resources are served by the dev server.
// The CMP runtime fetches resources at ./composeResources/{moduleId}/files/...
// and the processed files live in build/processedResources/{target}/main/.
//
// __dirname = build/wasm/packages/ArenaTactics-composeApp (for wasmJs)
//           = build/js/packages/ArenaTactics-composeApp  (for js)
// ../../../../ = project root in both cases.
const path = require('path');

if (config.devServer) {
    const existing = config.devServer.static;
    const staticEntries = Array.isArray(existing) ? existing : (existing ? [existing] : []);

    ['wasmJs', 'js'].forEach(function(target) {
        const resourceDir = path.resolve(
            __dirname,
            '../../../../composeApp/build/processedResources/' + target + '/main'
        );
        const alreadyAdded = staticEntries.some(function(e) {
            return (typeof e === 'string' ? e : (e && e.directory)) === resourceDir;
        });
        if (!alreadyAdded) {
            staticEntries.push({ directory: resourceDir, watch: false });
        }
    });

    config.devServer.static = staticEntries;
}
