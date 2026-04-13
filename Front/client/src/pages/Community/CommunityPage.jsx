import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { fetchCommunityPosts } from '../../api/community';

const CommunityPage = () => {
    const navigate = useNavigate();

    // 상태 관리
    const [posts, setPosts] = useState([]);
    const [searchTerm, setSearchTerm] = useState('');
    const [loading, setLoading] = useState(true);

    // 페이지네이션 상태
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    // 게시글 불러오는 핵심 함수
    const loadPosts = async (page = 0, search = searchTerm) => {
        try {
            setLoading(true);
            const response = await fetchCommunityPosts(search, page, 15);

            // 데이터가 잘 왔는지 콘솔에 찍어보세요 (디버깅 필수!)
            console.log("서버 응답 데이터:", response.data.data);

            const result = response.data.data;

            // result가 객체이므로 posts 배열을 명확히 지정!
            if (result && result.posts) {
                setPosts(result.posts);
                setTotalPages(result.totalPages);
                setCurrentPage(result.currentPage);
            }
        } catch (error) {
            console.error("게시글 로딩 실패:", error);
            setPosts([]); // 에러 시 빈 배열로 초기화
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadPosts(0);
    }, []);

    // 검색 시 0페이지부터 다시 조회
    const handleSearch = (e) => {
        e.preventDefault();
        loadPosts(0, searchTerm);
    };

    // 번호 클릭 시 해당 페이지 조회
    const handlePageChange = (pageNumber) => {
        loadPosts(pageNumber, searchTerm);
        window.scrollTo(0, 0); // 상단으로 스크롤 이동
    };

    return (
        <div className="min-h-screen bg-gray-50 py-10">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col md:flex-row gap-8">

                {/* 왼쪽 사이드바 */}
                <div className="w-full md:w-64 space-y-4">
                    <div className="bg-white rounded-2xl p-6 shadow-sm border border-gray-100">
                        <button
                            onClick={() => navigate('/community/write')}
                            className="w-full py-3 bg-blue-600 text-white rounded-xl font-bold hover:bg-blue-700 transition"
                        >
                            글쓰기
                        </button>
                    </div>
                </div>

                {/* 오른쪽 메인 콘텐츠 */}
                <div className="flex-1">
                    {/* 검색창 */}
                    <div className="bg-white rounded-2xl p-4 mb-6 shadow-sm border border-gray-100">
                        <form onSubmit={handleSearch} className="flex gap-2">
                            <input
                                type="text"
                                className="flex-1 bg-gray-50 border-none rounded-xl px-5 py-3 focus:ring-2 focus:ring-blue-500 outline-none"
                                placeholder="제목 검색..."
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                            />
                            <button type="submit" className="px-6 py-3 bg-blue-600 text-white rounded-xl font-bold">검색</button>
                        </form>
                    </div>

                    {/* 테이블 영역 */}
                    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
                        <table className="w-full text-left">
                            <thead className="bg-gray-50 text-gray-400 text-sm">
                            <tr>
                                <th className="px-6 py-4">분류</th>
                                <th className="px-6 py-4">제목</th>
                                <th className="px-6 py-4 text-center">작성자</th>
                                <th className="px-6 py-4 text-center">조회수</th>
                            </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-50">
                            {loading ? (
                                <tr><td colSpan="4" className="text-center py-10">로딩 중...</td></tr>
                            ) : posts.map(post => (
                                <tr key={post.id} className="hover:bg-gray-50 transition">
                                    <td className="px-6 py-4 text-xs font-bold text-gray-400">{post.category}</td>
                                    <td className="px-6 py-4">
                                        <Link to={`/community/post/${post.id}`} className="font-bold text-gray-900 hover:text-blue-600">
                                            {post.title}
                                        </Link>
                                    </td>
                                    <td className="px-6 py-4 text-center text-sm">{post.author}</td>
                                    <td className="px-6 py-4 text-center text-sm">{post.views}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>

                        {/* 🚀 페이지네이션 버튼 디자인 */}
                        <div className="p-6 bg-gray-50/30 flex justify-center border-t border-gray-50">
                            <div className="flex space-x-2">
                                {[...Array(totalPages)].map((_, i) => (
                                    <button
                                        key={i}
                                        onClick={() => handlePageChange(i)}
                                        className={`w-10 h-10 flex items-center justify-center rounded-lg font-bold text-sm transition ${
                                            currentPage === i
                                                ? 'bg-blue-600 text-white shadow-md'
                                                : 'bg-white text-gray-400 border border-gray-200 hover:bg-gray-100'
                                        }`}
                                    >
                                        {i + 1}
                                    </button>
                                ))}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default CommunityPage;