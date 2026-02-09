import fs from 'fs';
import path from 'path';
import { NodeIO, Document } from '@gltf-transform/core';
import { ALL_EXTENSIONS } from '@gltf-transform/extensions';
import { prune, dedup, cloneDocument, mergeDocuments } from '@gltf-transform/functions';

// 설정: 파일 경로들
const MATRIX_JSON_PATH = 'matrix_information.json';
const ASSETS_DIR = '../'; // 부품 파일들이 있는 폴더 (상위 폴더에 있다고 가정)
const OUTPUT_FILENAME = 'Robot_Gripper_Restored.gltf';

// 설정: 파트 이름(JSON Node Name)의 일부와 실제 파일명 매핑
// 주의: 대소문자를 구분하여 매칭합니다. (예: "Link"는 "Gear_link"에 매칭되지 않음)
const FILE_MAPPING = {
    'Base_Plate': 'Base Plate.gltf',
    'Base_Mounting_bracket': 'Base Mounting bracket.gltf',
    'Gear_link_1': 'Gear link 1.gltf',
    'Gear_link_2': 'Gear link 2.gltf',
    'Base_Gear': 'Base Gear.gltf',
    'Gripper': 'Gripper.gltf',
    'Link': 'Link.gltf',
    'Pin': 'Pin.gltf'
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

    // 2. 자산(부품) 미리 로드 및 캐싱
    const assetCache = new Map();
    console.log(`📦 자산 파일 로드 중... (경로: ${path.resolve(ASSETS_DIR)})`);

    for (const [keyword, filename] of Object.entries(FILE_MAPPING)) {
        const filePath = path.join(ASSETS_DIR, filename);
        if (fs.existsSync(filePath)) {
            try {
                const doc = await io.read(filePath);
                assetCache.set(keyword, doc);
                console.log(`   ✅ 로드 성공: ${filename} (Key: ${keyword})`);
            } catch (e) {
                console.error(`   🚨 로드 실패: ${filename}`, e.message);
            }
        } else {
            console.warn(`   ⚠️ 파일 없음: ${filename}`);
        }
    }

    // 3. 조립 시작
    const finalDoc = new Document();
    const masterScene = finalDoc.createScene('Scene');

    console.log(`🔧 조립 시작...`);

    let successCount = 0;
    for (const nodeInfo of matrixData.nodes) {
        const nodeName = nodeInfo.name; // 예: "Base_Plate1"
        const matrix = nodeInfo.matrix;

        // 이름 매칭 로직
        let matchedKey = null;
        for (const key of Object.keys(FILE_MAPPING)) {
            if (nodeName.includes(key)) {
                matchedKey = key;
                break;
            }
        }

        if (matchedKey && assetCache.has(matchedKey)) {
            const sourceDoc = assetCache.get(matchedKey);
            
            try {
                const partDoc = await cloneDocument(sourceDoc);
                const partScene = partDoc.getRoot().getDefaultScene() || partDoc.getRoot().listScenes()[0];

                // 래퍼 노드 생성 및 행렬 적용
                const wrapperNode = partDoc.createNode(nodeName).setMatrix(matrix);
                partScene.listChildren().forEach(child => wrapperNode.addChild(child));
                partScene.addChild(wrapperNode);

                await mergeDocuments(finalDoc, partDoc);
                successCount++;
                console.log(`   🔗 결합 완료: ${nodeName} (-> ${FILE_MAPPING[matchedKey]})`);

            } catch (err) {
                console.error(`   🚨 병합 중 오류 (${nodeName}):`, err.message);
            }
        } else {
            // console.warn(`   ⚠️ 매핑되는 자산 없음: ${nodeName}`);
        }
    }

    // 4. 씬 정리
    const root = finalDoc.getRoot();
    root.listScenes().forEach(scene => {
        if (scene !== masterScene) {
            scene.listChildren().forEach(child => masterScene.addChild(child));
            scene.dispose();
        }
    });

    // 5. 최적화 및 저장
    console.log(`🧹 최적화 및 저장 중...`);
    await finalDoc.transform(dedup(), prune());

    // URI 초기화 (Base64 임베딩)
    if (typeof root.listBuffers === 'function') {
        root.listBuffers().forEach(b => b.setURI(null));
    }
    if (typeof root.listImages === 'function') {
        root.listImages().forEach(i => i.setURI(null));
    }

    await io.write(OUTPUT_FILENAME, finalDoc);
    console.log(`✨ 전체 조립 완료! 결과 파일: ${OUTPUT_FILENAME} (성공: ${successCount}건)`);
}

main().catch(console.error);