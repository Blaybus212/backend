#!/bin/bash
set -e

echo "🔐 .env 파일 암호화 스크립트"
echo ""

# GPG_PASSPHRASE 환경 변수 확인
if [ -z "$GPG_PASSPHRASE" ]; then
    echo "❌ GPG_PASSPHRASE 환경 변수가 설정되지 않았습니다."
    echo ""
    echo "사용법:"
    echo "  export GPG_PASSPHRASE='your-passphrase'"
    echo "  ./scripts/encrypt-configs.sh"
    echo ""
    echo "또는:"
    echo "  GPG_PASSPHRASE='your-passphrase' ./scripts/encrypt-configs.sh"
    exit 1
fi

# .env 파일 존재 확인
if [ ! -f ".env" ]; then
    echo "❌ .env 파일이 없습니다."
    echo ""
    echo ".env 파일을 먼저 생성해주세요."
    exit 1
fi

echo "📄 .env 파일 암호화 중..."
echo "$GPG_PASSPHRASE" | gpg --batch --yes --passphrase-fd 0 \
    --symmetric --cipher-algo AES256 .env

if [ $? -eq 0 ]; then
    echo "✅ .env.gpg 생성 완료"
    ls -lh .env.gpg
    echo ""
    echo "다음 단계:"
    echo "  1. git add .env.gpg"
    echo "  2. git commit -m 'chore: update encrypted .env file'"
    echo "  3. git push"
    echo ""
    echo "⚠️  주의: .env 파일은 절대 커밋하지 마세요!"
else
    echo "❌ 암호화 실패"
    exit 1
fi
