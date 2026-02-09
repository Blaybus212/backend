import fs from 'fs';
import path from 'path';
import { NodeIO, Document } from '@gltf-transform/core';
import { ALL_EXTENSIONS } from '@gltf-transform/extensions';
import { prune, dedup, cloneDocument, mergeDocuments } from '@gltf-transform/functions';

// 설정: 파일 경로들
const MATRIX_JSON_PATH = 'matrix_information.json';
const ASSETS_DIR = '../'; // 부품 파일 폴더
const OUTPUT_FILENAME = 'Suspension_Restored_Corrected.gltf';

// 설정: Suspension 파트 매핑
const FILE_MAPPING = {
    'BASE': 'BASE.gltf',
    'NIT': 'NIT.gltf',
    'NUT': 'NUT.gltf',
    'ROD': 'ROD.gltf',
    'SPRING': 'SPRING.gltf'
};

async function main() {
    const io = new NodeIO().registerExtensions(ALL_EXTENSIONS);

    // 1. 매트릭스 정보 로드
    if (!fs.existsSync(MATRIX_JSON_PATH)) {
        console.error(`❌ Matrix JSON 파일을 찾을 수 없습니다: ${MATRIX_JSON_PATH}`);
        return;
    }
    const matrixData = JSON.parse(fs.readFileSync(MATRIX_JSON_PATH, 'utf8'));
    console.log(`📄 Matrix 정보 로드 완료: ${matrixData.nodes.length}개의 노드 정의됨`);

    // 2. 자산 캐싱
    const assetCache = new Map();
    console.log(`📦 자산 파일 로드 중...`);

    for (const [keyword, filename] of Object.entries(FILE_MAPPING)) {
        const filePath = path.join(ASSETS_DIR, filename);
        if (fs.existsSync(filePath)) {
            assetCache.set(keyword, await io.read(filePath));
            console.log(`   ✅ 로드 성공: ${filename}`);
        }
    }

    // 3. 조립 시작
    const finalDoc = new Document();
    const masterScene = finalDoc.createScene('Scene');

    console.log(`🔧 조립 및 스케일 보정 시작...`);

    let successCount = 0;
    for (const nodeInfo of matrixData.nodes) {
        const nodeName = nodeInfo.name;
        const matrix = nodeInfo.matrix;

        // 이름 매칭
        let matchedKey = null;
        for (const key of Object.keys(FILE_MAPPING)) {
            if (nodeName.includes(key)) {
                matchedKey = key;
                break;
            }
        }

        if (matchedKey && assetCache.has(matchedKey)) {
            const sourceDoc = assetCache.get(matchedKey);
            const partDoc = await cloneDocument(sourceDoc);
            const partScene = partDoc.getRoot().getDefaultScene() || partDoc.getRoot().listScenes()[0];

            // 래퍼 노드 생성 (전역 매트릭스 적용)
            const wrapperNode = partDoc.createNode(nodeName).setMatrix(matrix);

            // [핵심 수정] 자식 노드들의 내부 트랜스폼을 초기화(Reset)하여 이중 스케일링 방지
            partScene.listChildren().forEach(child => {
                child.setMatrix([1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1]); // 단위 행렬로 초기화
                child.setTranslation([0,0,0]);
                child.setRotation([0,0,0,1]);
                child.setScale([1,1,1]); // 스케일을 1로 강제 설정 (기존 0.01 제거)
                
                wrapperNode.addChild(child);
            });

            partScene.addChild(wrapperNode);
            await mergeDocuments(finalDoc, partDoc);
            successCount++;
            console.log(`   🔗 결합 및 보정 완료: ${nodeName}`);
        }
    }

    // 4. 정리 및 저장
    const root = finalDoc.getRoot();
    root.listScenes().forEach(s => {
        if (s !== masterScene) {
            s.listChildren().forEach(c => masterScene.addChild(c));
            s.dispose();
        }
    });

    console.log(`🧹 최적화 및 저장 중...`);
    await finalDoc.transform(dedup(), prune());

    // URI 초기화 (Base64 임베딩)
    if (typeof root.listBuffers === 'function') root.listBuffers().forEach(b => b.setURI(null));
    if (typeof root.listImages === 'function') root.listImages().forEach(i => i.setURI(null));

    await io.write(OUTPUT_FILENAME, finalDoc);
    console.log(`✨ 전체 조립 완료! 결과 파일: ${OUTPUT_FILENAME}`);
}

main().catch(console.error);