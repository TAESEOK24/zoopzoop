import React from 'react';
// 아이콘 라이브러리 (npm install lucide-react가 되어 있어야 합니다)
import {
    Home,
    Wallet,
    BookOpen,
    Briefcase,
    HeartPulse,
    Palette,
    Baby,
    LayoutGrid
} from 'lucide-react';

// 1. 카테고리 데이터 구성 (수정이 편하도록 배열로 관리)
const categories = [
    { id: 'housing', name: '주거', icon: <Home size={32} />, color: 'text-blue-500' },
    { id: 'finance', name: '금융', icon: <Wallet size={32} />, color: 'text-green-500' },
    { id: 'education', name: '교육', icon: <BookOpen size={32} />, color: 'text-yellow-500' },
    { id: 'employment', name: '취업', icon: <Briefcase size={32} />, color: 'text-orange-500' },
    { id: 'medical', name: '의료', icon: <HeartPulse size={32} />, color: 'text-red-500' },
    { id: 'culture', name: '문화', icon: <Palette size={32} />, color: 'text-purple-500' },
    { id: 'childcare', name: '육아', icon: <Baby size={32} />, color: 'text-pink-500' },
    { id: 'etc', name: '기타', icon: <LayoutGrid size={32} />, color: 'text-gray-500' },
];

const CategoryGrid = () => {
    // 클릭했을 때 실행될 함수 (나중에 송재경 님이 필터링 로직을 넣을 자리)
    const handleCategoryClick = (categoryName) => {
        console.log(`${categoryName} 카테고리 클릭됨`);
        // alert(`${categoryName} 혜택을 불러옵니다!`);
    };

    return (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 py-6">
            {categories.map((item) => (
                <button
                    key={item.id}
                    onClick={() => handleCategoryClick(item.name)}
                    className="flex flex-col items-center justify-center p-6 bg-white rounded-2xl shadow-sm border border-gray-100
                     hover:shadow-md hover:border-primary hover:-translate-y-1 transition-all duration-200 group"
                >
                    {/* 아이콘 영역: 마우스를 올리면 색상이 변함 */}
                    <div className={`mb-3 ${item.color} group-hover:scale-110 transition-transform`}>
                        {item.icon}
                    </div>

                    {/* 텍스트 영역 */}
                    <span className="font-bold text-gray-700 group-hover:text-primary">
            {item.name}
          </span>
                </button>
            ))}
        </div>
    );
};

export default CategoryGrid;