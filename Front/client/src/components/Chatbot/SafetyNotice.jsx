import React from 'react';
import { AlertTriangle, Phone } from 'lucide-react';

const SafetyNotice = () => {
    return (
        <div className="mt-4 p-4 bg-red-50 border border-red-200 rounded-2xl shadow-sm w-full max-w-sm">
            <div className="flex items-center text-red-600 mb-2 font-bold text-[15px]">
                <AlertTriangle size={18} className="mr-2" />
                긴급한 도움이 필요하신가요?
            </div>
            <p className="text-sm text-gray-700 mb-4 leading-relaxed">
                현재 많이 지치고 힘드신 상황일 수 있습니다. 혼자 고민하지 마시고 꼭 아래의 번호로 연락하여 전문가의 도움을 받아보세요. 24시간 언제든 도움을 받을 수 있습니다.
            </p>
            <div className="space-y-2">
                <a 
                    href="tel:129" 
                    className="flex items-center justify-between bg-white p-3 rounded-xl border border-red-100 hover:bg-red-100/50 transition-colors"
                >
                    <span className="font-medium text-[14px] text-gray-800">보건복지상담센터</span>
                    <span className="flex items-center text-red-600 font-bold text-[16px]"><Phone size={14} className="mr-1" /> 129</span>
                </a>
                <a 
                    href="tel:15770199" 
                    className="flex items-center justify-between bg-white p-3 rounded-xl border border-red-100 hover:bg-red-100/50 transition-colors"
                >
                    <span className="font-medium text-[14px] text-gray-800">정신건강위기상담</span>
                    <span className="flex items-center text-red-600 font-bold text-[16px]"><Phone size={14} className="mr-1" /> 1577-0199</span>
                </a>
            </div>
        </div>
    );
};

export default SafetyNotice;
