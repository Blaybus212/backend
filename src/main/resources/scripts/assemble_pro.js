import fs from 'fs';
import path from 'path';
import { NodeIO, Document } from '@gltf-transform/core';
import { ALL_EXTENSIONS } from '@gltf-transform/extensions';
import { prune, dedup, cloneDocument, mergeDocuments, unpartition } from '@gltf-transform/functions';

// Usage: node assemble_pro.js <input_json_path> <assets_dir> <output_path>

async function main() {
    const args = process.argv.slice(2);
    if (args.length < 3) {
        console.error("Usage: node assemble_pro.js <input_json_path> <assets_dir> <output_path>");
        process.exit(1);
    }

    const [inputJsonPath, assetsDir, outputPath] = args;

    if (!fs.existsSync(inputJsonPath)) {
        console.error(`❌ Input JSON not found: ${inputJsonPath}`);
        process.exit(1);
    }

    const assemblyData = JSON.parse(fs.readFileSync(inputJsonPath, 'utf8'));
    // assemblyData structure:
    // {
    //   "assets": { "key": "filename.gltf" },
    //   "instances": [
    //     { "name": "...", "assetId": "key", "matrix": [...], "extras": {...} }
    //   ]
    // }

    const io = new NodeIO().registerExtensions(ALL_EXTENSIONS);
    
    // 1. Asset Caching
    const assetCache = new Map();
    // Use assets map if available, otherwise fallback to finding unique assetIds
    const assetMap = assemblyData.assets || {};
    // Collect all asset keys needed
    const requiredAssetKeys = [...new Set(assemblyData.instances.map(n => n.assetId))];

    console.log(`📦 Loading assets for ${requiredAssetKeys.length} types from ${assetsDir}...`);

    for (const assetKey of requiredAssetKeys) {
        // Resolve filename: check map, then default to key + .gltf
        let filename = assetMap[assetKey];
        if (!filename) {
             filename = assetKey.endsWith('.gltf') || assetKey.endsWith('.glb') ? assetKey : `${assetKey}.gltf`;
        }

        const filePath = path.join(assetsDir, filename);

        if (fs.existsSync(filePath)) {
            try {
                const doc = await io.read(filePath);
                assetCache.set(assetKey, doc);
                console.log(`   ✅ Loaded: ${filename} (Key: ${assetKey})`);
            } catch (e) {
                console.error(`   ❌ Failed to load: ${filename}`, e);
            }
        } else {
            console.warn(`   ⚠️ Asset file not found: ${filename}`);
        }
    }

    // 2. Assembly
    const finalDoc = new Document();
    const masterScene = finalDoc.createScene('Scene');

    console.log(`🔧 Assembling ${assemblyData.instances.length} instances...`);

    let successCount = 0;
    for (const instanceInfo of assemblyData.instances) {
        const { name, assetId, matrix, extras } = instanceInfo;

        if (assetCache.has(assetId)) {
            const sourceDoc = assetCache.get(assetId);
            const partDoc = await cloneDocument(sourceDoc);
            const partScene = partDoc.getRoot().getDefaultScene() || partDoc.getRoot().listScenes()[0];

            // Create Wrapper Node
            const wrapperNode = partDoc.createNode(name).setMatrix(matrix);
            
            // Inject Metadata
            if (extras) {
                wrapperNode.setExtras(extras);
                console.log(`   📝 Injected extras for ${name}: ${JSON.stringify(extras)}`);
            }

            // Do NOT reset child transforms. 
            // The source assets (e.g. Arm gear.gltf) often have root nodes with specific scales (e.g. 0.01)
            // or rotations that must be preserved. The assembly matrix applies to the whole component instance.
            partScene.listChildren().forEach(child => {
                wrapperNode.addChild(child);
            });

            partScene.addChild(wrapperNode);
            await mergeDocuments(finalDoc, partDoc);
            successCount++;
        } else {
            console.warn(`   ⚠️ Skipping node ${name}: Asset ${assetId} not loaded.`);
        }
    }

    // 2.5 Inject Scene Extras (LookAt, Note)
    if (assemblyData.extras) {
        console.log(`📝 Injecting Scene Extras...`);
        masterScene.setExtras(assemblyData.extras);
    }

    // 3. Clean up and Save
    const root = finalDoc.getRoot();
    root.listScenes().forEach(s => {
        if (s !== masterScene) {
            s.listChildren().forEach(c => masterScene.addChild(c));
            s.dispose();
        }
    });

    console.log(`🧹 Optimizing...`);
    await finalDoc.transform(
        unpartition(),
        prune(),
        dedup()
    );

    // Force single buffer (usually unpartition handles this, but ensuring checks)
    const currentBuffers = root.listBuffers();
    if (currentBuffers.length > 1) {
        console.log(`⚠️ Unpartition left ${currentBuffers.length} buffers. Forcing merge...`);
        const mainBuffer = currentBuffers[0];
        root.listAccessors().forEach(a => a.setBuffer(mainBuffer));
        currentBuffers.slice(1).forEach(b => b.dispose());
    }

    // Prepare for Embedded GLTF Output
    // We use writeJSON to extract raw data, then manually embed efficiently.
    console.log(`💾 Encoding to Embedded GLTF (${outputPath})...`);
    
    const { json, resources } = await io.writeJSON(finalDoc);

    // Embed Buffers
    if (json.buffers) {
        json.buffers.forEach((bufferDef, index) => {
            const uri = bufferDef.uri; // typically "buffer.bin" or similar from writeJSON defaults
            if (resources[uri]) {
                const bufferData = resources[uri];
                const base64 = Buffer.from(bufferData).toString('base64');
                bufferDef.uri = `data:application/octet-stream;base64,${base64}`;
                delete resources[uri]; // Mark as handled
                console.log(`   🔗 Embedded Buffer ${index} (${bufferData.length} bytes)`);
            }
        });
    }

    // Embed Images
    if (json.images) {
        json.images.forEach((imageDef, index) => {
            const uri = imageDef.uri;
            if (resources[uri]) {
                const imageData = resources[uri];
                const mimeType = imageDef.mimeType || 'image/png'; // Default to png if missing
                const base64 = Buffer.from(imageData).toString('base64');
                imageDef.uri = `data:${mimeType};base64,${base64}`;
                delete resources[uri];
                console.log(`   🖼️ Embedded Image ${index} (${uri})`);
            }
        });
    }

    // Write final JSON to file
    fs.writeFileSync(outputPath, JSON.stringify(json, null, 2));
    console.log(`✨ Done!`);
}

main().catch(e => {
    console.error(e);
    process.exit(1);
});
