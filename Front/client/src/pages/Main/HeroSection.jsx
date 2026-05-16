import React, { useEffect, useState } from 'react';
import { fetchRecentPolicySearches } from '../../api/policies';

const HeroSection = ({ onSearch }) => {
    const [searchText, setSearchText] = useState('');
    const [showDropdown, setShowDropdown] = useState(false);
    const [recentSearches, setRecentSearches] = useState([]);

    useEffect(() => {
        let cancelled = false;

        const loadRecentSearches = async () => {
            const token = localStorage.getItem('accessToken');
            if (!token) {
                setRecentSearches([]);
                return;
            }

            try {
                const result = await fetchRecentPolicySearches(5);
                if (!cancelled) {
                    setRecentSearches(result?.data?.keywords ?? []);
                }
            } catch {
                if (!cancelled) {
                    setRecentSearches([]);
                }
            }
        };

        loadRecentSearches();

        const handleLoginStateChange = () => loadRecentSearches();
        window.addEventListener('loginStateChange', handleLoginStateChange);

        return () => {
            cancelled = true;
            window.removeEventListener('loginStateChange', handleLoginStateChange);
        };
    }, []);

    const submitSearch = (value = searchText) => {
        const keyword = value.trim();
        if (!keyword) {
            return;
        }

        onSearch(keyword);
        setShowDropdown(false);
    };

    const handleSubmit = (event) => {
        event.preventDefault();
        submitSearch();
    };

    const handleRecentSearchClick = (item) => {
        setSearchText(item);
        submitSearch(item);
    };

    return (
        <div className="bg-blue-600 text-white py-16 px-4 text-center relative z-30">
            <h1 className="text-4xl font-extrabold mb-4">
                내게 필요한 정부 혜택,
                <br />
                놓치지 말고 줍줍하세요
            </h1>
            <p className="text-blue-100 mb-8 text-lg">복잡한 지원 정책, 한 번의 검색으로 확인하세요</p>

            <form className="max-w-2xl mx-auto relative" onSubmit={handleSubmit}>
                <input
                    type="text"
                    value={searchText}
                    placeholder="관심 있는 혜택이나 키워드를 검색해 보세요"
                    className="w-full p-5 rounded-full text-gray-900 shadow-lg outline-none focus:ring-4 focus:ring-blue-300 transition-all"
                    onChange={(event) => setSearchText(event.target.value)}
                    onFocus={() => setShowDropdown(true)}
                    onBlur={() => setTimeout(() => setShowDropdown(false), 200)}
                />
                <button
                    type="submit"
                    className="absolute right-3 top-3 bg-blue-700 p-2 px-6 rounded-full hover:bg-blue-800 transition-colors text-white font-bold"
                >
                    검색
                </button>

                {showDropdown && recentSearches.length > 0 && (
                    <div className="absolute top-full left-0 right-0 mt-3 p-6 bg-white rounded-2xl shadow-xl border border-gray-100 text-left z-50">
                        <h5 className="text-sm font-bold text-gray-500 mb-3">최근 검색어</h5>
                        <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-5">
                            {recentSearches.map((item) => (
                                <button
                                    key={item}
                                    type="button"
                                    onMouseDown={(event) => event.preventDefault()}
                                    onClick={() => handleRecentSearchClick(item)}
                                    className="min-w-0 truncate px-3 py-2 bg-gray-100 text-gray-800 rounded-full text-sm font-medium hover:bg-blue-50 hover:text-blue-700 cursor-pointer"
                                    title={item}
                                >
                                    {item}
                                </button>
                            ))}
                        </div>
                    </div>
                )}
            </form>
        </div>
    );
};

export default HeroSection;
