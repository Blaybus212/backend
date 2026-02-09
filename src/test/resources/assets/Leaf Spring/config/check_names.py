import json
import os
import sys

def check_node_names():
    # 1. 명령어 인자가 있는지 확인 (스크립트 이름 제외 첫 번째 인자)
    if len(sys.argv) < 2:
        print("❌ 사용법: python check_names.py <gltf_파일_경로>")
        print("예시: python check_names.py ../Leaf_Spring_flatten.gltf")
        return

    source_path = sys.argv[1]

    # 2. 파일 존재 여부 확인
    if not os.path.exists(source_path):
        print(f"❌ 파일을 찾을 수 없습니다: {os.path.abspath(source_path)}")
        return

    print(f"🚀 분석 중인 파일: {source_path}")
    print("-" * 50)

    try:
        with open(source_path, 'r', encoding='utf-8') as f:
            data = json.load(f)

        nodes = data.get('nodes', [])
        found_mesh = False

        # 3. 노드 순회 및 출력
        for i, node in enumerate(nodes):
            if 'mesh' in node:
                found_mesh = True
                name = node.get('name', 'N/A')
                mesh_idx = node['mesh']
                
                # 정점 개수도 함께 출력해주면 매핑할 때 훨씬 편합니다.
                v_count = -1
                if 'meshes' in data and mesh_idx < len(data['meshes']):
                    mesh = data['meshes'][mesh_idx]
                    v_count = 0
                    for prim in mesh['primitives']:
                        pos_idx = prim['attributes']['POSITION']
                        v_count += data['accessors'][pos_idx]['count']

                print(f"Index: {i:3} | Name: {name:20} | Mesh: {mesh_idx:2} | Verts: {v_count}")

        if not found_mesh:
            print("⚠️ 메쉬를 포함한 노드가 이 파일에 없습니다.")

    except Exception as e:
        print(f"☠️ 오류 발생: {e}")

if __name__ == "__main__":
    check_node_names()