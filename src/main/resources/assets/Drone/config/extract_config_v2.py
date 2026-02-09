import json
import os
import sys

def round_matrix(matrix, decimals=6):
    """부동 소수점 오차 방지를 위한 반올림"""
    return tuple(round(x, decimals) for x in matrix)

def get_clean_name(name):
    """이름에서 숫자와 확장자를 제거하여 순수 키워드 추출 (예: 'Gearing1' -> 'gearing')"""
    name = name.replace('.gltf', '').lower()
    # 숫자 제거 (예: Gearing1, Gearing2 -> gearing)
    return ''.join([i for i in name if not i.isdigit()]).strip('-_ ')

def run_extraction():
    if len(sys.argv) < 2:
        print("❌ 사용법: python extract_config_v4.py <flatten_gltf_경로>")
        return

    source_gltf_path = sys.argv[1]
    parent_dir = ".."
    matrix_info_path = "matrix_information.json"
    output_config = "assembly_config.json"

    if not os.path.exists(source_gltf_path) or not os.path.exists(matrix_info_path):
        print("❌ 입력 파일(glTF 또는 matrix_information.json)이 없습니다.")
        return

    # 1. 부모 디렉토리 에셋 스캔 (자동 라이브러리 구축)
    asset_library = {}
    for f in os.listdir(parent_dir):
        if f.endswith('.gltf') and 'flatten' not in f.lower():
            clean_key = get_clean_name(f)
            asset_library[clean_key] = f
    
    print(f"📦 에셋 라이브러리 구축 완료: {list(asset_library.keys())}")

    # 2. 데이터 로드
    with open(source_gltf_path, 'r', encoding='utf-8') as f:
        gltf_data = json.load(f)
    with open(matrix_info_path, 'r', encoding='utf-8') as f:
        matrix_info = json.load(f)

    # 3. 매트릭스 룩업 테이블 생성 (Matrix -> Logical Name)
    matrix_lookup = {}
    for node in matrix_info.get('nodes', []):
        if 'matrix' in node:
            m_key = round_matrix(node['matrix'])
            matrix_lookup[m_key] = node['name']

    # 4. 분석 및 매핑
    print(f"🚀 자동 조립 분석 시작: {source_gltf_path}")
    gltf_nodes = gltf_data.get('nodes', [])
    instances = []

    for i, node in enumerate(gltf_nodes):
        if 'mesh' not in node: continue

        node_matrix = node.get('matrix')
        if not node_matrix: continue

        m_key = round_matrix(node_matrix)
        
        # 행렬 지문으로 'Gearing1' 같은 이름을 찾음
        logical_name = matrix_lookup.get(m_key)
        
        asset_id = "unknown_part"
        if logical_name:
            # 'Gearing1' -> 'gearing'
            clean_logic_name = get_clean_name(logical_name)
            
            # 라이브러리에서 'gearing' 키를 가진 파일(Gearing.gltf)을 찾음
            if clean_logic_name in asset_library:
                asset_id = clean_logic_name
                print(f"   ✅ 자동 매칭: {logical_name} -> {asset_library[asset_id]}")
            else:
                # 유사 이름 검색 (예: 'arm_gear' vs 'armgear')
                for key in asset_library.keys():
                    if key in clean_logic_name or clean_logic_name in key:
                        asset_id = key
                        break
        
        instances.append({
            "name": logical_name if logical_name else node.get('name', f"node_{i}"),
            "assetId": asset_id,
            "matrix": node_matrix
        })

    # 5. 결과 저장
    input_base = os.path.basename(source_gltf_path).replace('.gltf', '')
    config_result = {
        "outputName": f"{input_base}_Assembled.gltf",
        "assets": asset_library,
        "instances": instances
    }

    with open(output_config, 'w', encoding='utf-8') as f:
        json.dump(config_result, f, indent=2)
    
    print(f"\n✨ 조립 지시서 생성 완료: {output_config}")

if __name__ == "__main__":
    run_extraction()