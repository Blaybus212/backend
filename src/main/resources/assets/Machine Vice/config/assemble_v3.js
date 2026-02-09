import fs from 'fs';
import path from 'path';
import { NodeIO, Document } from '@gltf-transform/core';
import { ALL_EXTENSIONS } from '@gltf-transform/extensions';
import { prune, dedup, cloneDocument, mergeDocuments } from '@gltf-transform/functions';

// 설정: 파일 경로들
const MATRIX_JSON_PATH = 'matrix_information.json';
const ASSETS_DIR = '../'; // 부품 파일들이 있는 폴더 (현재 폴더의 상위라고 가정)
const OUTPUT_FILENAME = 'Machine_Vice_Restored_Full.gltf';

// 설정: 파트 이름(JSON)과 실제 파일명 매핑 테이블
// JSON에 있는 이름의 일부(키워드) : 실제 파일명
const FILE_MAPPING = {
    'grundplatte': 'Part8-grundplatte.gltf',
    'spindelsockel': 'Part4 spindelsockel.gltf',
    'Feste_Backe': 'Part2 Feste Backe.gltf',
    'lose_backe': 'Part3-lose backe.gltf',
    'Fuhrung': 'Part1 Fuhrung.gltf',
    'Spannbacke': 'Part5-Spannbacke.gltf',
    'fuhrungschiene': 'Part6-fuhrungschiene.gltf',
    'Druckhulse': 'Part9-Druckhulse.gltf',
    'TrapezSpindel': 'Part7-TrapezSpindel.gltf'
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
    console.log(`📦 자산 파일 로드 중...`);

    for (const [keyword, filename] of Object.entries(FILE_MAPPING)) {
        const filePath = path.join(ASSETS_DIR, filename);
        if (fs.existsSync(filePath)) {
            try {
                const doc = await io.read(filePath);
                assetCache.set(keyword, doc); // 키워드로 문서 캐싱
                console.log(`   ✅ 로드 성공: ${filename} (Key: ${keyword})`);
            } catch (e) {
                console.error(`   🚨 로드 실패: ${filename}`, e.message);
            }
        } else {
            console.warn(`   ⚠️ 파일 없음: ${filePath}`);
        }
    }

    // 3. 조립 시작
    const finalDoc = new Document();
    const masterScene = finalDoc.createScene('Scene');

    console.log(`🔧 조립 시작...`);

    let successCount = 0;
    for (const nodeInfo of matrixData.nodes) {
        const nodeName = nodeInfo.name; // 예: "Part8-grundplatte1"
        const matrix = nodeInfo.matrix;

        // 이름에서 매핑 키워드 찾기
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
                // 문서 복제 (cloneDocument 사용)
                const partDoc = await cloneDocument(sourceDoc);
                
                // 루트 씬 가져오기
                const partScene = partDoc.getRoot().getDefaultScene() || partDoc.getRoot().listScenes()[0];

                // 래퍼 노드 생성 및 행렬 적용
                // 여기서 중요한 점: 원본의 중심축이 틀어지지 않게 래퍼를 씌웁니다.
                const wrapperNode = partDoc.createNode(nodeName).setMatrix(matrix);
                
                // 기존 씬의 자식들을 래퍼 노드 밑으로 이동
                partScene.listChildren().forEach(child => wrapperNode.addChild(child));
                
                // 씬에 래퍼 노드만 남김 (또는 머지 준비)
                partScene.addChild(wrapperNode);

                // 최종 문서에 병합
                await mergeDocuments(finalDoc, partDoc);
                successCount++;
                console.log(`   🔗 결합 완료: ${nodeName}`);

            } catch (err) {
                console.error(`   🚨 병합 중 오류 (${nodeName}):`, err.message);
            }
        } else {
            console.warn(`   ⚠️ 매핑되는 자산 없음: ${nodeName}`);
        }
    }

    // 4. 씬 정리 (병합된 씬들을 하나의 메인 씬으로 통합)
    const root = finalDoc.getRoot();
    root.listScenes().forEach(scene => {
        if (scene !== masterScene) {
            scene.listChildren().forEach(child => masterScene.addChild(child));
            scene.dispose();
        }
    });

    // 5. 최적화 및 저장 (Embedded 모드)
    console.log(`🧹 최적화 및 저장 중...`);
    await finalDoc.transform(dedup(), prune());

    // 모든 버퍼와 이미지를 URI 삭제 -> Base64 임베딩 유도
    root.listBuffers().forEach(b => b.setURI(null));
    root.listImages().forEach(i => i.setURI(null));

    await io.write(OUTPUT_FILENAME, finalDoc);
    console.log(`✨ 전체 조립 완료! 결과 파일: ${OUTPUT_FILENAME} (성공: ${successCount}건)`);
}

main().catch(console.error);