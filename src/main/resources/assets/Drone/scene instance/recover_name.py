import json

def restore_gltf_names(gltf_path, matrix_info_path, output_path):
    # 1. 파일 로드
    with open(gltf_path, 'r', encoding='utf-8') as f:
        gltf = json.load(f)
    with open(matrix_info_path, 'r', encoding='utf-8') as f:
        matrix_data = json.load(f)

    # 2. 매핑 테이블 생성 (Matrix -> Name)
    # 행렬 리스트를 튜플로 변환하여 딕셔너리 키로 사용 (부동소수점 오차 고려 가능)
    name_map = {}
    for node in matrix_data['nodes']:
        if 'matrix' in node:
            # 비교를 위해 소수점 6자리까지 반올림하여 튜플 생성
            matrix_key = tuple(round(v, 6) for v in node['matrix'])
            name_map[matrix_key] = node['name']

    # 3. glTF 노드 이름 업데이트
    update_count = 0
    for node in gltf.get('nodes', []):
        if 'matrix' in node:
            current_matrix = tuple(round(v, 6) for v in node['matrix'])
            if current_matrix in name_map:
                node['name'] = name_map[current_matrix]
                update_count += 1

    # 4. 결과 저장
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(gltf, f, indent=2)
    
    print(f"성공: 총 {update_count}개의 노드 이름이 복구되었습니다. -> {output_path}")

# 실행
restore_gltf_names('./Drone_embedded_flatten.gltf', '../config/matrix_information.json', './Drone_restored.gltf')