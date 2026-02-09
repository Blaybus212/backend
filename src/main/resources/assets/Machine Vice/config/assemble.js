import fs from 'fs';
import path from 'path';
import { NodeIO, Document } from '@gltf-transform/core';
import { dedup, prune, cloneDocument, mergeDocuments } from '@gltf-transform/functions';

const CONFIG_FILE = './assembly_config.json';

async function main() {
    const io = new NodeIO();
    
    // 1. 설정 파일 로드
    if (!fs.existsSync(CONFIG_FILE)) {
        console.error(`❌ 설정 파일을 찾을 수 없습니다.`);
        return;
    }
    const config = JSON.parse(fs.readFileSync(CONFIG_FILE, 'utf8'));
    const assetDir = path.join(path.dirname(CONFIG_FILE), '../');

    // 2. 자원 로드 및 캐싱
    const assetCache = new Map();
    console.log('📦 자원 로드 중...');
    for (const [id, filename] of Object.entries(config.assets)) {
        const filePath = path.join(assetDir, filename);
        if (fs.existsSync(filePath)) {
            assetCache.set(id, await io.read(filePath));
            console.log(`   ✅ 로드 완료: ${id}`);
        }
    }

    // 3. 통합 문서 생성
    const finalDoc = new Document();
    const masterScene = finalDoc.createScene('Scene');

    console.log(`🔧 Matrix 기반 조립 시작...`);

    for (const instance of config.instances) {
        const sourceDoc = assetCache.get(instance.assetId);
        if (!sourceDoc) continue;

        try {
            const partDoc = await cloneDocument(sourceDoc);
            const partScene = partDoc.getRoot().listScenes()[0];
            
            // Wrapper 노드 생성 및 Matrix 적용
            const wrapper = partDoc.createNode(instance.name);

            if (instance.matrix) {
                wrapper.setMatrix(instance.matrix);
            } else if (instance.transform) {
                wrapper.setTranslation(instance.transform.translation || [0, 0, 0])
                       .setRotation(instance.transform.rotation || [0, 0, 0, 1])
                       .setScale(instance.transform.scale || [1, 1, 1]);
            }

            // 부품 노드들을 Wrapper 하위로 이동
            partScene.listChildren().forEach(child => wrapper.addChild(child));
            partScene.addChild(wrapper);

            await mergeDocuments(finalDoc, partDoc);
            console.log(`   ✅ 배치 완료: ${instance.name}`);
        } catch (err) {
            console.error(`   🚨 오류 발생 (${instance.name}):`, err.message);
        }
    }

    // 4. Scene 통합 (여러 Scene을 하나로)
    const root = finalDoc.getRoot();
    root.listScenes().forEach(s => {
        if (s !== masterScene) {
            s.listChildren().forEach(c => masterScene.addChild(c));
            s.dispose();
        }
    });

    // 5. 최적화 및 강제 Embedded 변환 (핵심 수정 부분)
    console.log('🧹 최적화 및 Embedded 변환 중...');
    try {
        await finalDoc.transform(dedup(), prune());

        // 모든 Buffer(바이너리)의 URI를 비워 Base64 임베딩 유도
        root.listBuffers().forEach(buffer => buffer.setURI(''));
        
        // 모든 Image(이미지)의 URI를 비워 Base64 임베딩 유도 (v4 표준 방식)
        // listImages()가 없을 경우를 대비해 안전하게 처리합니다.
        const images = root.listImages ? root.listImages() : [];
        images.forEach(image => {
            if (image && typeof image.setURI === 'function') {
                image.setURI('');
            }
        });

        const outputPath = config.outputName || 'Assembled_Matrix_Embedded.gltf';
        await io.write(outputPath, finalDoc);

        console.log(`\n🎉 조립 및 임베딩 완료!`);
        console.log(`📂 저장 경로: ${path.resolve(outputPath)}`);
    } catch (e) {
        console.error("☠️ 저장 중 오류 발생:", e.message);
    }
}

main().catch(err => console.error("💥 치명적 오류:", err));