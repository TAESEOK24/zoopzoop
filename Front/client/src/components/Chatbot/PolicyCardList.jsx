import React from 'react';
import PolicyCard from './PolicyCard';

/**
 * @param {{ policies: Array<import('./PolicyCard').Policy> }} props 
 */
const PolicyCardList = ({ policies }) => {
    if (!policies || policies.length === 0) return null;

    return (
        <div className="w-full mt-3">
            <p className="mb-3 text-sm font-semibold text-gray-800 border-b pb-2">📋 추천 정책 확인하기</p>
            <div className="space-y-3">
                {policies.map((policy, idx) => (
                    <PolicyCard key={policy.serviceId || idx} policy={policy} />
                ))}
            </div>
        </div>
    );
};

export default PolicyCardList;
