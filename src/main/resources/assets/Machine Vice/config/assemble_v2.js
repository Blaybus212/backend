import fs from 'fs';
import path from 'path';
import { NodeIO, Document } from '@gltf-transform/core';
import { ALL_EXTENSIONS } from '@gltf-transform/extensions'; // 모든 확장 등록
import { dedup, prune, mergeDocuments } from '@gltf-transform/functions';

const CONFIG_FILE = './assembly_config.json';

async function main() {
    // 1. NodeIO 설정 (확장 등록이 색상 보존의 핵심입니다)
    const io = new NodeIO().registerExtensions(ALL_EXTENSIONS);
    
    if (!fs.existsSync(CONFIG_FILE)) {
        console.error(`❌ 설정 파일을 찾을 수 없습니다.`);
        return;
    }
    const config = JSON.parse(fs.readFileSync(CONFIG_FILE, 'utf8'));
    // CONFIG_FILE이 위치한 디렉토리를 찾고, 그 디렉토리의 상위(부모) 디렉토리 경로를 설정합니다.
    const assetDir = path.join(path.dirname(CONFIG_FILE), '..');

    // 2. 자원 로드
    const assetCache = new Map();
    console.log('📦 자원 로드 중...');
    for (const [id, filename] of Object.entries(config.assets)) {
        const filePath = path.join(assetDir, filename);
        if (fs.existsSync(filePath)) {
            assetCache.set(id, await io.read(filePath));
            console.log(`   ✅ 로드 완료: ${id}`);
        } else {
            console.warn(`   ⚠️ 파일 없음: ${filePath}`);
        }
    }

    // 3. 통합 문서 생성
    const finalDoc = new Document();
    const masterScene = finalDoc.createScene('Scene');

    console.log(`🔧 조립 시작...`);
    for (const instance of config.instances) {
        const sourceDoc = assetCache.get(instance.assetId);
        if (!sourceDoc) continue;

        try {
            // 원본 문서를 복제하여 개별 인스턴스 생성
            const partDoc = sourceDoc.clone();
            const partScene = partDoc.getRoot().getDefaultScene() || partDoc.getRoot().listScenes()[0];
            
            // 행렬 적용을 위한 래퍼 노드 생성
            const wrapper = partDoc.createNode(instance.name).setMatrix(instance.matrix);
            partScene.listChildren().forEach(child => wrapper.addChild(child));
            partScene.addChild(wrapper);

            // 최종 문서에 통합
            await mergeDocuments(finalDoc, partDoc);
            console.log(`   ✅ 배치 완료: ${instance.name}`);
        } catch (err) {
            console.error(`   🚨 오류 발생 (${instance.name}):`, err.message);
        }
    }

    // 4. 모든 Scene을 하나로 합치기
    const root = finalDoc.getRoot();
    root.listScenes().forEach(s => {
        if (s !== masterScene) {
            s.listChildren().forEach(c => masterScene.addChild(c));
            s.dispose();
        }
    });

    // 5. 최적화 및 저장
    console.log('🧹 최적화 중...');
    // 색상 유실이 걱정된다면 dedup()에서 재질(material) 제외를 고려할 수 있습니다.
    await finalDoc.transform(prune()); 

    root.listBuffers().forEach(buffer => buffer.setURI(''));
    
    const outputPath = config.outputName || 'Final_Assembled.gltf';
    await io.write(outputPath, finalDoc);
    console.log(`✨ 조립 완료! 결과물: ${outputPath}`);
}

main().catch(console.error);