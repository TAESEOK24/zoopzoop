import React, { useState } from 'react';

const HeroSection = ({ onSearch }) => {
    const [showDropdown, setShowDropdown] = useState(false);
    const recentSearches = ['청년도약계좌', '알뜰폰 할인', '전세자금대출'];

    return (
        <div className="bg-blue-600 text-white py-16 px-4 text-center relative z-30">
            <h1 className="text-4xl font-extrabold mb-4">내게 필요한 정부 혜택, <br/>놓치지 말고 줍줍하세요!</h1>
            <p className="text-blue-100 mb-8 text-lg">복잡한 지원 정책, 한 번의 검색으로 확인하세요.</p>

            <div className="max-w-2xl mx-auto relative">
                <input
                    type="text"
                    placeholder="관심 있는 혜택이나 키워드를 검색해 보세요."
                    className="w-full p-5 rounded-full text-gray-900 shadow-lg outline-none focus:ring-4 focus:ring-blue-300 transition-all"
                    onChange={(e) => onSearch(e.target.value)}
                    onFocus={() => setShowDropdown(true)}
                    onBlur={() => setTimeout(() => setShowDropdown(false), 200)}
                />
                <button className="absolute right-3 top-3 bg-blue-700 p-2 px-6 rounded-full hover:bg-blue-800 transition-colors text-white font-bold">
                    검색
                </button>

                {/* 검색 자동완성 드롭다운 */}
                {showDropdown && (
                    <div className="absolute top-full left-0 right-0 mt-3 p-6 bg-white rounded-2xl shadow-xl border border-gray-100 text-left z-50">
                        <h5 className="text-sm font-bold text-gray-500 mb-3">최근 검색어</h5>
                        <div className="flex flex-wrap gap-2">
                            {recentSearches.map((item, idx) => (
                                <span key={idx} className="px-3 py-1 bg-gray-100 text-gray-800 rounded-full text-sm font-medium hover:bg-blue-50 hover:text-blue-700 cursor-pointer">
                                    {item}
                                </span>
                            ))}
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

export default HeroSection;