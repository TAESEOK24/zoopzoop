import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

const CommunityPage = () => {
    const navigate = useNavigate();

    // 1. 임시 게시글 데이터 (테스트를 위해 카테고리 속성 추가 및 데이터 확장)
    const allPosts = [
        { id: 1, type: '필독', category: '공지사항', title: '📢 ZoopZoop 커뮤니티 이용 가이드 및 통합 공지', author: '운영자', date: '2026.04.06', views: 12000 },
        { id: 2, type: '공지', category: '공지사항', title: '[이벤트] 숨은 혜택 찾기 인증하고 상품 받아가세요!', author: '운영자', date: '2026.04.05', views: 3412 },
        { id: 3, type: '일반', category: '질문&답변', title: '청년도약계좌 신청하신 분들 질문 있습니다 ㅠㅠ', author: '고민많은20대', date: '12:30', views: 45 },
        { id: 4, type: '일반', category: '자유게시판', title: '이번에 K-패스 교통카드 혜택 진짜 좋네요 (후기)', author: '지하철출퇴근러', date: '10:15', views: 128 },
        { id: 5, type: '일반', category: '질문&답변', title: '중소기업 취업자 소득세 감면 서류 뭐뭐 필요한가요?', author: '신입사원', date: '09:22', views: 67 },
        { id: 6, type: '일반', category: '자유게시판', title: '월세 지원금 오늘 들어왔습니다!!', author: '자취생', date: '어제', views: 201 },
        { id: 7, type: '일반', category: '자유게시판', title: '다들 점심 뭐 드시나요?', author: '배고픈직장인', date: '어제', views: 34 },
        { id: 8, type: '일반', category: '질문&답변', title: '내일배움카드 학원 추천 좀 해주세요', author: '취준생', date: '2026.04.04', views: 89 },
        { id: 9, type: '일반', category: '자유게시판', title: '오늘 날씨 진짜 좋네요 놀러가고 싶다', author: '방구석', date: '2026.04.04', views: 56 },
    ];

    // 2. 상태(State) 관리
    const [activeCategory, setActiveCategory] = useState('전체글보기'); // 현재 선택된 카테고리
    const [searchInput, setSearchInput] = useState(''); // 검색창 입력값
    const [searchTerm, setSearchTerm] = useState(''); // 실제 검색 실행된 키워드
    const [itemsPerPage, setItemsPerPage] = useState(15); // 한 페이지당 게시글 수
    const [currentPage, setCurrentPage] = useState(1); // 현재 페이지 번호

    // 3. 데이터 필터링 로직 (카테고리 + 검색어)
    let filteredPosts = allPosts;

    // 카테고리 필터
    if (activeCategory !== '전체글보기' && activeCategory !== '베스트 게시물 (HOT)') {
        filteredPosts = filteredPosts.filter(post => post.category === activeCategory);
    } else if (activeCategory === '베스트 게시물 (HOT)') {
        filteredPosts = filteredPosts.filter(post => post.views >= 100); // 조회수 100 이상을 베스트로 취급
    }

    // 검색어 필터
    if (searchTerm) {
        filteredPosts = filteredPosts.filter(post =>
            post.title.includes(searchTerm) || post.author.includes(searchTerm)
        );
    }

    // 4. 페이징 로직 계산
    const totalPages = Math.ceil(filteredPosts.length / itemsPerPage) || 1;
    const startIndex = (currentPage - 1) * itemsPerPage;
    const currentPosts = filteredPosts.slice(startIndex, startIndex + itemsPerPage);

    // --- 핸들러 함수들 ---
    const handleCategoryClick = (category) => {
        setActiveCategory(category);
        setSearchTerm(''); // 카테고리 이동 시 검색 초기화
        setSearchInput('');
        setCurrentPage(1); // 페이지 1로 초기화
    };

    const handleSearch = () => {
        setSearchTerm(searchInput);
        setCurrentPage(1);
    };

    const handleKeyPress = (e) => {
        if (e.key === 'Enter') handleSearch();
    };

    // 사이드바 메뉴 렌더링 함수 (중복 코드 최소화)
    const renderMenuItem = (name, icon = null) => {
        const isActive = activeCategory === name;
        return (
            <li
                onClick={() => handleCategoryClick(name)}
                className={`flex items-center cursor-pointer transition-colors ${isActive ? 'font-bold text-blue-600 underline' : 'hover:text-blue-600 hover:underline'}`}
            >
                {icon && <span className="w-5">{icon}</span>}
                {name}
            </li>
        );
    };

    return (
        <div className="min-h-screen bg-gray-50 py-8">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col md:flex-row gap-8">

                {/* 좌측 사이드바 */}
                <aside className="w-full md:w-64 flex-shrink-0 space-y-6">
                    <div className="bg-white p-5 rounded-2xl shadow-sm border border-gray-200">
                        <div className="flex items-center space-x-3 mb-4">
                            <div className="w-12 h-12 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 text-xl font-bold">Z</div>
                            <div>
                                <h2 className="font-bold text-gray-900">ZoopZoop 공식</h2>
                            </div>
                        </div>
                        <button
                            onClick={() => alert('글쓰기 페이지로 이동합니다! (추후 연결)')}
                            className="w-full py-2.5 bg-blue-600 text-white font-bold rounded-lg hover:bg-blue-700 transition shadow-sm"
                        >
                            글쓰기
                        </button>
                    </div>

                    <div className="bg-white p-5 rounded-2xl shadow-sm border border-gray-200">
                        {/* 검색창 */}
                        <div className="mb-6 flex">
                            <input
                                type="text"
                                placeholder="게시글 검색"
                                value={searchInput}
                                onChange={(e) => setSearchInput(e.target.value)}
                                onKeyDown={handleKeyPress}
                                className="w-full text-sm border border-gray-300 rounded-l-md px-3 py-1.5 focus:outline-none focus:border-blue-500"
                            />
                            <button onClick={handleSearch} className="bg-blue-600 text-white px-3 py-1.5 rounded-r-md text-sm hover:bg-blue-700">검색</button>
                        </div>

                        {/* 카테고리 메뉴 */}
                        <div className="space-y-5 text-sm text-gray-700">
                            <div>
                                <h3 className="font-bold mb-2 flex items-center text-gray-900"><span className="text-blue-500 mr-1">★</span> 즐겨찾는 게시판</h3>
                                <ul className="space-y-2 pl-5">
                                    {renderMenuItem('전체글보기')}
                                    {renderMenuItem('베스트 게시물 (HOT)')}
                                </ul>
                            </div>
                            <div className="border-t border-gray-100 pt-4">
                                <h3 className="font-bold mb-2 text-gray-900">ZoopZoop 소식</h3>
                                <ul className="space-y-2 pl-2">
                                    {renderMenuItem('공지사항', '📄')}
                                </ul>
                            </div>
                            <div className="border-t border-gray-100 pt-4">
                                <h3 className="font-bold mb-2 text-gray-900">커뮤니티</h3>
                                <ul className="space-y-2 pl-2">
                                    {renderMenuItem('자유게시판', '💬')}
                                    {renderMenuItem('질문&답변', '❓')}
                                </ul>
                            </div>
                        </div>
                    </div>
                </aside>

                {/* 우측 메인 콘텐츠 (게시글 목록) */}
                <main className="flex-1 bg-white rounded-2xl shadow-sm border border-gray-200 p-6 overflow-hidden flex flex-col min-h-[600px]">

                    {/* 상단 타이틀 및 옵션 */}
                    <div className="flex justify-between items-center border-b border-gray-800 pb-3 mb-4">
                        <h2 className="text-xl font-bold text-gray-900 flex items-center">
                            {/* 선택된 카테고리에 따라 타이틀 변경 */}
                            <span className="text-blue-500 mr-2">{activeCategory === '베스트 게시물 (HOT)' ? '🔥' : '⭐'}</span>
                            {activeCategory}
                            {searchTerm && <span className="text-sm font-normal text-gray-500 ml-3">"{searchTerm}" 검색 결과</span>}
                        </h2>

                        {/* 한 페이지 표시 개수 변경 */}
                        <select
                            className="border border-gray-300 text-sm rounded-md px-2 py-1 outline-none"
                            value={itemsPerPage}
                            onChange={(e) => {
                                setItemsPerPage(Number(e.target.value));
                                setCurrentPage(1);
                            }}
                        >
                            <option value={5}>5개씩</option>
                            <option value={10}>10개씩</option>
                            <option value={15}>15개씩</option>
                        </select>
                    </div>

                    {/* 게시글 목록 테이블 */}
                    <div className="overflow-x-auto flex-1">
                        <table className="w-full text-left text-sm whitespace-nowrap">
                            <thead>
                            <tr className="border-b border-gray-200 text-gray-500">
                                <th className="py-3 px-4 w-16 text-center">분류</th>
                                <th className="py-3 px-4">제목</th>
                                <th className="py-3 px-4 w-32 text-center">작성자</th>
                                <th className="py-3 px-4 w-24 text-center">작성일</th>
                                <th className="py-3 px-4 w-16 text-center">조회</th>
                            </tr>
                            </thead>
                            <tbody>
                            {currentPosts.length > 0 ? (
                                currentPosts.map((post) => (
                                    <tr key={post.id} className="border-b border-gray-100 hover:bg-gray-50 cursor-pointer transition">
                                        <td className="py-3 px-4 text-center">
                                            {post.type !== '일반' ? (
                                                <span className={`px-2 py-0.5 rounded text-xs font-bold ${post.type === '필독' ? 'bg-red-50 text-red-500' : 'bg-gray-100 text-gray-600'}`}>{post.type}</span>
                                            ) : (<span className="text-gray-400">{post.id}</span>)}
                                        </td>
                                        <td className={`py-3 px-4 ${post.type !== '일반' ? 'font-bold text-gray-900' : 'text-gray-700'}`}>{post.title}</td>
                                        <td className="py-3 px-4 text-center text-gray-600">{post.author}</td>
                                        <td className="py-3 px-4 text-center text-gray-500 text-xs">{post.date}</td>
                                        <td className="py-3 px-4 text-center text-gray-400 text-xs">{post.views.toLocaleString()}</td>
                                    </tr>
                                ))
                            ) : (
                                <tr>
                                    <td colSpan="5" className="py-12 text-center text-gray-500">게시글이 존재하지 않습니다.</td>
                                </tr>
                            )}
                            </tbody>
                        </table>
                    </div>

                    {/* 하단 페이지네이션 및 글쓰기 버튼 */}
                    <div className="mt-6 flex justify-between items-center border-t border-gray-100 pt-4">
                        <div className="w-20"></div> {/* 간격 맞추기용 빈 공간 */}

                        {/* 동적 페이지네이션 */}
                        <div className="flex space-x-1">
                            <button
                                onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
                                disabled={currentPage === 1}
                                className="px-3 py-1 border border-gray-300 rounded text-sm text-gray-600 hover:bg-gray-50 disabled:opacity-50"
                            >
                                &lt; 이전
                            </button>

                            {[...Array(totalPages)].map((_, i) => (
                                <button
                                    key={i + 1}
                                    onClick={() => setCurrentPage(i + 1)}
                                    className={`px-3 py-1 rounded text-sm font-bold border ${currentPage === i + 1 ? 'bg-gray-900 text-white border-gray-900' : 'bg-white text-gray-600 border-gray-300 hover:bg-gray-50'}`}
                                >
                                    {i + 1}
                                </button>
                            ))}

                            <button
                                onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))}
                                disabled={currentPage === totalPages || totalPages === 0}
                                className="px-3 py-1 border border-gray-300 rounded text-sm text-gray-600 hover:bg-gray-50 disabled:opacity-50"
                            >
                                다음 &gt;
                            </button>
                        </div>

                        <button
                            onClick={() => alert('글쓰기 페이지로 이동합니다! (추후 연결)')}
                            className="px-4 py-2 bg-blue-600 text-white font-bold rounded-lg text-sm hover:bg-blue-700 transition"
                        >
                            글쓰기
                        </button>
                    </div>
                </main>
            </div>
        </div>
    );
};

export default CommunityPage;