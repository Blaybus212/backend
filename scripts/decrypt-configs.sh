#!/bin/bash
set -e

echo "🔓 .env 파일 복호화 스크립트"
echo ""

# GPG_PASSPHRASE 환경 변수 확인
if [ -z "$GPG_PASSPHRASE" ]; then
    echo "❌ GPG_PASSPHRASE 환경 변수가 설정되지 않았습니다."
    echo ""
    echo "사용법:"
    echo "  export GPG_PASSPHRASE='your-passphrase'"
    echo "  ./scripts/decrypt-configs.sh"
    echo ""
    echo "또는:"
    echo "  GPG_PASSPHRASE='your-passphrase' ./scripts/decrypt-configs.sh"
    exit 1
fi

# .env.gpg 파일 존재 확인
if [ ! -f ".env.gpg" ]; then
    echo "❌ .env.gpg 파일이 없습니다."
    echo ""
    echo "암호화된 .env.gpg 파일이 저장소에 있는지 확인해주세요."
    exit 1
fi

echo "� .env.gpg 복호화 중..."
echo "$GPG_PASSPHRASE" | gpg --batch --yes --passphrase-fd 0 \
    --decrypt --output .env .env.gpg

if [ $? -eq 0 ]; then
    echo "✅ .env 파일 생성 완료"
    
    # 보안: 파일 권한 설정 (소유자만 읽기/쓰기)
    chmod 600 .env
    echo "✅ 파일 권한 600으로 설정"
    
    ls -lh .env
    echo ""
    echo "⚠️  주의: .env 파일은 절대 커밋하지 마세요!"
else
    echo "❌ 복호화 실패"
    exit 1
fi
