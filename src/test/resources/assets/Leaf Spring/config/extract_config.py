import json
import os
import sys

# 🔍 노드 이름 매핑 규칙 (원본 파일의 노드 이름 기준)
NAME_MAPPING = {
    "clamp_primary": "Solid1034",
    "clamp_center": "Solid2",
    "clamp_secondary": "Solid1032",
    "leaf_layer": "Solid2_1",
    "support_chassis": "Solid1010",
    "support_rubber": "Solid1011",
    "support_main": "Solid2001"
}

def run_extraction():
    # 1. 명령어 인자 확인
    if len(sys.argv) < 2:
        print("❌ 사용법: python extract_config.py <flatten_gltf_경로>")
        print("예시: python extract_config.py ../scene_instance/Leaf_Spring_flatten.gltf")
        return

    source_gltf = sys.argv[1]
    output_config = "assembly_config.json"

    # 2. 파일 존재 여부 확인
    if not os.path.exists(source_gltf):
        print(f"❌ 파일을 찾을 수 없습니다: {os.path.abspath(source_gltf)}")
        return

    print(f"🚀 분석 시작: {source_gltf}")

    with open(source_gltf, 'r', encoding='utf-8') as f:
        data = json.load(f)

    nodes = data.get('nodes', [])
    instances = []

    for i, node in enumerate(nodes):
        if 'mesh' not in node:
            continue

        node_name = node.get('name', f"node_{i}")
        
        # 이름 규칙에 따라 assetId 결정 (매칭 안되면 unknown_part)
        asset_id = "unknown_part"
        for aid, keyword in NAME_MAPPING.items():
            if keyword in node_name:
                asset_id = aid
                break

        # TRS 데이터 추출 (Scale은 1.0으로 고정 - 에셋 중복 축소 방지)
        instances.append({
            "name": node_name,
            "assetId": asset_id,
            "transform": {
                "translation": node.get('translation', [0, 0, 0]),
                "rotation": node.get('rotation', [0, 0, 0, 1]),
                "scale": [1, 1, 1] 
            }
        })
        print(f"   ✅ [추출] {node_name} -> {asset_id}")

    # 최종 조립 지시서 구조 생성
    config_result = {
        "outputName": "Leaf_Spring_Full_Assembly_Final.gltf",
        "assets": {
            "clamp_center": "Clamp-Center.gltf",
            "clamp_primary": "Clamp-Primary.gltf",
            "clamp_secondary": "Clamp-Secondary.gltf",
            "leaf_layer": "Leaf-Layer.gltf",
            "support_chassis": "Support-Chassis.gltf",
            "support_rubber": "Support-Rubber.gltf",
            "support_main": "Support.gltf"
        },
        "instances": instances
    }

    with open(output_config, 'w', encoding='utf-8') as f:
        json.dump(config_result, f, indent=2)
    
    print(f"\n✨ 생성 완료! 현재 폴더에 '{output_config}' 파일이 만들어졌습니다.")

if __name__ == "__main__":
    run_extraction()