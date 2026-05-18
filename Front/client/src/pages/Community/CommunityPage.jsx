import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchCommunityPosts } from '../../api/community';
import { fetchPolicies } from '../../api/policies';
import { getAccessToken } from '../../api/authSession';

const CommunityPage = () => {
    const navigate = useNavigate();

    const [posts, setPosts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [activeCategory, setActiveCategory] = useState('전체글보기');
    const [searchInput, setSearchInput] = useState('');
    const [searchTerm, setSearchTerm] = useState('');

    const [currentPage, setCurrentPage] = useState(1);
    const [totalPages, setTotalPages] = useState(1);
    const [itemsPerPage, setItemsPerPage] = useState(15);

    useEffect(() => {
        const fetchData = async () => {
            try {
                setLoading(true);

                const [communityRes, policyRes] = await Promise.all([
                    fetchCommunityPosts(searchTerm, currentPage - 1, itemsPerPage),
                    fetchPolicies()
                ]);

                let combinedData = [];

                if (communityRes.data && communityRes.data.data) {
                    const result = communityRes.data.data;
                    setTotalPages(result.totalPages || 1);
                    combinedData = [...(result.posts || [])];
                }

                const policyList = policyRes?.data?.items || policyRes?.data?.content || policyRes?.data || [];
                if (Array.isArray(policyList)) {
                    const formattedPolicies = policyList.map(p => ({
                        id: `policy-${p.serviceId || p.id}`,
                        policyServiceId: p.serviceId,
                        type: '정책',
                        category: '공지사항',
                        title: p.serviceName || p.polyBizSjnm || p.title || '제목 없음',
                        author: p.orgName || '정부/지자체',
                        date: p.applicationDeadline || p.startDate || '-',
                        views: p.viewCount || p.views || 0
                    }));

                    if (activeCategory === '전체글보기' || activeCategory === '공지사항') {
                        combinedData = [...combinedData, ...formattedPolicies];
                    }
                }

                if (activeCategory !== '전체글보기' && activeCategory !== '베스트 게시물 (HOT)' && activeCategory !== '공지사항') {
                    combinedData = combinedData.filter(post => post.category === activeCategory);
                } else if (activeCategory === '베스트 게시물 (HOT)') {
                    combinedData = combinedData.filter(post => (post.views || 0) >= 10);
                }

                setPosts(combinedData);

            } catch (error) {
                console.error("데이터 로드 실패:", error);
                setPosts([]);
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, [currentPage, searchTerm, itemsPerPage, activeCategory]);

    const handleCategoryClick = (category) => {
        setActiveCategory(category);
        setCurrentPage(1);
    };

    const handleSearch = () => {
        setSearchTerm(searchInput);
        setCurrentPage(1);
    };

    // 🚀 [핵심 추가] 글쓰기 버튼 클릭 시 로그인 여부 검사
    const handleWriteClick = () => {
        const token = getAccessToken();
        if (!token) {
            alert("로그인 후 이용할 수 있습니다.");
            navigate('/login');
            return;
        }
        navigate('/community/write');
    };

    const renderMenuItem = (name, icon = null) => {
        const isActive = activeCategory === name;
        return (
            <li
                key={name}
                onClick={() => handleCategoryClick(name)}
                className={`flex items-center cursor-pointer py-1 px-2 rounded-lg transition-all ${
                    isActive
                        ? 'font-bold text-blue-600 bg-blue-50'
                        : 'text-gray-600 hover:bg-gray-50 hover:text-blue-500'
                }`}
            >
                {icon && <span className="mr-2">{icon}</span>}
                {name}
            </li>
        );
    };

    return (
        <div className="min-h-screen bg-gray-50 py-8">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col md:flex-row gap-8">
                <aside className="w-full md:w-64 flex-shrink-0 space-y-6">
                    <div className="bg-white p-5 rounded-2xl shadow-sm border border-gray-200">
                        <div className="flex items-center space-x-3 mb-4">
                            <div className="w-12 h-12 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 text-xl font-bold">Z</div>
                            <h2 className="font-bold text-gray-900">ZoopZoop 공식</h2>
                        </div>
                        {/* 🚀 onClick 변경됨 */}
                        <button
                            onClick={handleWriteClick}
                            className="w-full py-2.5 bg-blue-600 text-white font-bold rounded-lg hover:bg-blue-700 transition shadow-sm"
                        >
                            글쓰기
                        </button>
                    </div>

                    <div className="bg-white p-5 rounded-2xl shadow-sm border border-gray-200">
                        <div className="mb-6 flex">
                            <input
                                type="text"
                                placeholder="게시글 검색"
                                value={searchInput}
                                onChange={(e) => setSearchInput(e.target.value)}
                                onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
                                className="w-full text-sm border border-gray-300 rounded-l-md px-3 py-1.5 focus:outline-none focus:ring-1 focus:ring-blue-500"
                            />
                            <button onClick={handleSearch} className="bg-blue-600 text-white px-3 py-1.5 rounded-r-md text-sm font-bold">검색</button>
                        </div>

                        <div className="space-y-5 text-sm">
                            <div>
                                <h3 className="font-bold mb-3 text-gray-900 flex items-center">
                                    <span className="text-yellow-500 mr-2">★</span> 즐겨찾는 게시판
                                </h3>
                                <ul className="space-y-1 ml-1">
                                    {renderMenuItem('전체글보기')}
                                    {renderMenuItem('베스트 게시물 (HOT)')}
                                </ul>
                            </div>
                            <div className="border-t pt-4">
                                <h3 className="font-bold mb-3 text-gray-900">ZoopZoop 소식</h3>
                                <ul className="space-y-1 ml-1">
                                    {renderMenuItem('공지사항', '📄')}
                                </ul>
                            </div>
                            <div className="border-t pt-4">
                                <h3 className="font-bold mb-3 text-gray-900">커뮤니티</h3>
                                <ul className="space-y-1 ml-1">
                                    {renderMenuItem('자유게시판', '💬')}
                                    {renderMenuItem('질문&답변', '❓')}
                                </ul>
                            </div>
                        </div>
                    </div>
                </aside>

                <main className="flex-1 bg-white rounded-2xl shadow-sm border border-gray-200 p-6 flex flex-col min-h-[600px]">
                    <div className="flex justify-between items-center border-b pb-4 mb-4">
                        <h2 className="text-xl font-bold text-gray-900">{activeCategory}</h2>
                        <select
                            className="border border-gray-300 text-sm rounded-md px-2 py-1 outline-none cursor-pointer bg-white"
                            value={itemsPerPage}
                            onChange={(e) => {
                                setItemsPerPage(Number(e.target.value));
                                setCurrentPage(1);
                            }}
                        >
                            <option value={5}>5개씩</option>
                            <option value={10}>10개씩</option>
                            <option value={15}>15개씩</option>
                            <option value={30}>30개씩</option>
                            <option value={45}>45개씩</option>
                        </select>
                    </div>

                    <div className="overflow-x-auto flex-1">
                        <table className="w-full text-left text-sm whitespace-nowrap">
                            <thead>
                            <tr className="border-b text-gray-400 font-medium">
                                <th className="py-3 px-4 w-16 text-center">분류</th>
                                <th className="py-3 px-4">제목</th>
                                <th className="py-3 px-4 w-32 text-center">작성자</th>
                                <th className="py-3 px-4 w-24 text-center">작성일</th>
                                <th className="py-3 px-4 w-16 text-center">조회</th>
                            </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-50">
                            {loading ? (
                                <tr><td colSpan="5" className="py-12 text-center text-gray-400">데이터 로딩 중...</td></tr>
                            ) : posts.length > 0 ? (
                                posts.map((post) => (
                                    <tr key={post.id} className="hover:bg-gray-50 cursor-pointer transition-colors" onClick={() => navigate(post.policyServiceId ? `/policies/${post.policyServiceId}` : `/community/post/${post.id}`)}>
                                        <td className="py-4 px-4 text-center">
                                                <span className={`px-2 py-0.5 rounded text-xs font-bold ${
                                                    post.type === '정책' ? 'bg-blue-50 text-blue-600' : 'bg-gray-100 text-gray-600'
                                                }`}>
                                                    {post.category || '일반'}
                                                </span>
                                        </td>
                                        <td className="py-4 px-4 text-gray-800 font-medium">{post.title}</td>
                                        <td className="py-4 px-4 text-center text-gray-600">{post.author}</td>
                                        <td className="py-4 px-4 text-center text-gray-400 text-xs">{post.date}</td>
                                        <td className="py-4 px-4 text-center text-gray-400 text-xs">{post.views || 0}</td>
                                    </tr>
                                ))
                            ) : (
                                <tr><td colSpan="5" className="py-12 text-center text-gray-400">표시할 데이터가 없습니다.</td></tr>
                            )}
                            </tbody>
                        </table>
                    </div>

                    <div className="mt-8 flex justify-center space-x-1">
                        {[...Array(totalPages)].map((_, i) => (
                            <button
                                key={i + 1}
                                onClick={() => {
                                    setCurrentPage(i + 1);
                                    window.scrollTo(0, 0);
                                }}
                                className={`w-8 h-8 flex items-center justify-center rounded-md text-sm font-bold transition-all ${
                                    currentPage === i + 1
                                        ? 'bg-blue-600 text-white shadow-md'
                                        : 'bg-white text-gray-400 border border-gray-200 hover:border-blue-300 hover:text-blue-500'
                                }`}
                            >
                                {i + 1}
                            </button>
                        ))}
                    </div>
                </main>
            </div>
        </div>
    );
};

export default CommunityPage;