// Use content hashes for async chunk filenames.
// Without this, webpack assigns arbitrary numeric IDs (446.js, 464.js, ...) that
// change unpredictably between builds. When a user has an old composeApp.js
// cached that references chunk IDs from a previous build, those files no longer
// exist and loading fails. Content hashes produce stable, unique names per build.
config.output = config.output || {};
config.output.chunkFilename = '[contenthash].js';
