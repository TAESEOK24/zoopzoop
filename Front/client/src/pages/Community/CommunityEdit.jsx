import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { fetchCommunityPostDetail, updateCommunityPost } from '../../api/community';

const CommunityEdit = () => {
    const { id } = useParams();
    const navigate = useNavigate();

    const [title, setTitle] = useState('');
    const [category, setCategory] = useState('');
    const [content, setContent] = useState('');
    const [loading, setLoading] = useState(false);

    // 1. 기존 데이터 불러와서 채워넣기
    useEffect(() => {
        const fetchPost = async () => {
            try {
                const response = await fetchCommunityPostDetail(id);
                const post = response.data.data;
                setTitle(post.title);
                setCategory(post.category);
                setContent(post.content);
            } catch (error) {
                alert("데이터를 불러오지 못했습니다.");
                navigate(-1);
            }
        };
        fetchPost();
    }, [id, navigate]);

    // 2. 수정 버튼 클릭 로직
    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!title.trim() || !content.trim()) return alert('모두 입력해주세요.');

        try {
            setLoading(true);
            const postData = { title, category, content };
            await updateCommunityPost(id, postData); // PUT 요청
            alert('수정되었습니다.');
            navigate(`/community/post/${id}`); // 수정한 상세 페이지로 다시 이동
        } catch (error) {
            alert('수정 중 오류가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-gray-50 py-8">
            <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
                <div className="mb-6 flex items-center justify-between">
                    <h2 className="text-2xl font-bold text-gray-900">게시글 수정</h2>
                    <button onClick={() => navigate(-1)} className="text-gray-500 hover:text-gray-700 font-medium">취소</button>
                </div>

                <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-8">
                    <form onSubmit={handleSubmit} className="space-y-6">
                        <div>
                            <label className="block text-sm font-bold text-gray-700 mb-2">카테고리</label>
                            <select value={category} onChange={(e) => setCategory(e.target.value)} className="w-full border border-gray-300 rounded-lg px-4 py-2.5 outline-none">
                                <option value="자유게시판">자유게시판</option>
                                <option value="질문&답변">질문&답변</option>
                            </select>
                        </div>
                        <div>
                            <label className="block text-sm font-bold text-gray-700 mb-2">제목</label>
                            <input type="text" value={title} onChange={(e) => setTitle(e.target.value)} className="w-full border border-gray-300 rounded-lg px-4 py-2.5 outline-none" />
                        </div>
                        <div>
                            <label className="block text-sm font-bold text-gray-700 mb-2">내용</label>
                            <textarea rows="15" value={content} onChange={(e) => setContent(e.target.value)} className="w-full border border-gray-300 rounded-lg px-4 py-2.5 outline-none resize-none"></textarea>
                        </div>
                        <div className="flex justify-end pt-4">
                            <button type="submit" disabled={loading} className="px-8 py-3 rounded-lg text-white font-bold bg-blue-600 hover:bg-blue-700">
                                {loading ? '수정 중...' : '수정 완료'}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default CommunityEdit;