import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createCommunityPost } from '../../api/community';

const CommunityWrite = () => {
    const navigate = useNavigate();

    // 입력받을 데이터 상태 관리
    const [title, setTitle] = useState('');
    const [category, setCategory] = useState('자유게시판');
    const [content, setContent] = useState('');
    const [loading, setLoading] = useState(false);

    // 등록 버튼 클릭 시 실행될 함수
    const handleSubmit = async (e) => {
        e.preventDefault();

        // 빈 칸 검사
        if (!title.trim() || !content.trim()) {
            alert('제목과 내용을 모두 입력해주세요.');
            return;
        }

        try {
            setLoading(true);

            // 백엔드로 보낼 데이터 객체 조립
            const postData = {
                type: '일반',          // 기본 타입
                category: category,    // 선택한 카테고리
                title: title,
                content: content,
                author: '테스트유저'   // 임시 작성자 (추후 로그인 연동 시 변경)
            };

            // API 함수 호출하여 DB에 저장
            await createCommunityPost(postData);

            alert('게시글이 성공적으로 등록되었습니다.');
            navigate('/community'); // 등록 완료 후 커뮤니티 메인 목록으로 이동

        } catch (error) {
            console.error('글 작성 실패:', error);
            alert('글 작성 중 오류가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-gray-50 py-8">
            <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">

                {/* 상단 헤더 부분 */}
                <div className="mb-6 flex items-center justify-between">
                    <h2 className="text-2xl font-bold text-gray-900">새 게시글 작성</h2>
                    <button
                        onClick={() => navigate(-1)}
                        className="text-gray-500 hover:text-gray-700 font-medium"
                    >
                        취소하고 돌아가기
                    </button>
                </div>

                {/* 입력 폼 영역 */}
                <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-8">
                    <form onSubmit={handleSubmit} className="space-y-6">

                        {/* 1. 카테고리 선택 */}
                        <div>
                            <label className="block text-sm font-bold text-gray-700 mb-2">카테고리</label>
                            <select
                                value={category}
                                onChange={(e) => setCategory(e.target.value)}
                                className="w-full border border-gray-300 rounded-lg px-4 py-2.5 focus:outline-none focus:ring-2 focus:ring-blue-500"
                            >
                                <option value="자유게시판">자유게시판</option>
                                <option value="질문&답변">질문&답변</option>
                            </select>
                        </div>

                        {/* 2. 제목 입력 */}
                        <div>
                            <label className="block text-sm font-bold text-gray-700 mb-2">제목</label>
                            <input
                                type="text"
                                placeholder="제목을 입력해주세요."
                                value={title}
                                onChange={(e) => setTitle(e.target.value)}
                                className="w-full border border-gray-300 rounded-lg px-4 py-2.5 focus:outline-none focus:ring-2 focus:ring-blue-500"
                            />
                        </div>

                        {/* 3. 본문 내용 입력 */}
                        <div>
                            <label className="block text-sm font-bold text-gray-700 mb-2">내용</label>
                            <textarea
                                rows="15"
                                placeholder="내용을 자유롭게 작성해주세요. (욕설이나 비방글은 삭제될 수 있습니다.)"
                                value={content}
                                onChange={(e) => setContent(e.target.value)}
                                className="w-full border border-gray-300 rounded-lg px-4 py-2.5 focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
                            ></textarea>
                        </div>

                        {/* 등록 버튼 */}
                        <div className="flex justify-end pt-4 border-t border-gray-100">
                            <button
                                type="submit"
                                disabled={loading}
                                className={`px-8 py-3 rounded-lg text-white font-bold text-lg transition ${
                                    loading ? 'bg-gray-400' : 'bg-blue-600 hover:bg-blue-700'
                                }`}
                            >
                                {loading ? '등록 중...' : '게시글 등록하기'}
                            </button>
                        </div>
                    </form>
                </div>

            </div>
        </div>
    );
};

export default CommunityWrite;