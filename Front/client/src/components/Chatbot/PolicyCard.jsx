import React, { useState } from 'react';
import { ChevronDown, ChevronUp, ExternalLink } from 'lucide-react';

/**
 * @typedef {Object} Policy
 * @property {string} serviceId
 * @property {string} serviceName
 * @property {string} purposeSummary
 * @property {string} target
 * @property {string} supportContent
 * @property {string} applicationMethod
 * @property {string} applicationDeadline
 * @property {string} detailUrl
 * @property {string} orgName
 * @property {string} departmentName
 * @property {string} recommendationReason
 */

/**
 * @param {{ policy: Policy }} props 
 */
const PolicyCard = ({ policy }) => {
    const [expanded, setExpanded] = useState(false);

    return (
        <div className="bg-white border border-gray-200 rounded-lg p-4 mb-3 shadow-sm text-sm w-full">
            <h4 className="font-bold text-blue-700 mb-1 leading-snug">{policy.serviceName || '정책 이름 없음'}</h4>
            <p className="text-gray-700 mb-2 text-xs leading-relaxed line-clamp-2">{policy.purposeSummary || ''}</p>
            
            {policy.recommendationReason && (
                <div className="text-xs bg-yellow-50 text-yellow-800 p-2 rounded mb-3 border border-yellow-100">
                    <span className="font-semibold mr-1">💡 추천 이유:</span>
                    {policy.recommendationReason}
                </div>
            )}

            <div className="flex flex-col gap-2 mt-2">
                <div className="text-gray-500 text-xs flex justify-between items-center">
                    <span className="font-medium bg-gray-100 px-2 py-1 rounded text-[10px]">
                        {policy.orgName} {policy.departmentName ? `> ${policy.departmentName}` : ''}
                    </span>
                    <button 
                        onClick={() => setExpanded(!expanded)}
                        className="flex items-center text-blue-600 hover:text-blue-800 focus:outline-none transition-colors border border-blue-200 hover:border-blue-400 px-2 py-1 rounded"
                        aria-expanded={expanded}
                    >
                        {expanded ? <><ChevronUp size={14} className="mr-1"/>접기</> : <><ChevronDown size={14} className="mr-1"/>상세보기</>}
                    </button>
                </div>
                
                {expanded && (
                    <div className="mt-3 pt-3 border-t border-gray-100 text-gray-700 text-xs space-y-2 bg-gray-50 p-3 rounded-md animate-in slide-in-from-top-2 duration-200">
                        {policy.target && <p><strong>지원대상:</strong> {policy.target}</p>}
                        {policy.supportContent && <p><strong>지원내용:</strong> {policy.supportContent}</p>}
                        {policy.applicationMethod && <p><strong>신청방법:</strong> {policy.applicationMethod}</p>}
                        {policy.applicationDeadline && <p><strong>신청기한:</strong> {policy.applicationDeadline}</p>}
                        {policy.detailUrl && (
                            <a href={policy.detailUrl} target="_blank" rel="noreferrer" className="flex items-center text-blue-600 underline mt-2 font-medium hover:text-blue-800 transition-colors">
                                상세정보 확인하기 <ExternalLink size={12} className="ml-1" />
                            </a>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

export default PolicyCard;
