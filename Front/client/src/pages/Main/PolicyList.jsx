const PolicyList = ({ query }) => {
    // 실제로는 API에서 가져올 데이터의 형태(Schema)입니다.
    const dummyData = [
        { id: 1, title: "청년 월세 한시 특별지원", category: "주거", dDay: "D-15", provider: "국토교통부" },
        { id: 2, title: "내일배움카드 발급지원", category: "교육", dDay: "상시", provider: "고용노동부" },
        { id: 3, title: "중소기업 취업자 소득세 감면", category: "금융", dDay: "D-30", provider: "국세청" },
    ];

    return (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {dummyData.map((policy) => (
                <div key={policy.id} className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 hover:shadow-md transition-shadow cursor-pointer">
                    <div className="flex justify-between items-start mb-4">
                        <span className="bg-blue-50 text-blue-600 px-3 py-1 rounded-full text-sm font-bold">{policy.category}</span>
                        <span className="text-orange-500 font-bold">{policy.dDay}</span>
                    </div>
                    <h3 className="text-xl font-bold mb-2 text-gray-900 leading-tight">{policy.title}</h3>
                    <p className="text-gray-500 text-sm">{policy.provider}</p>
                </div>
            ))}
        </div>
    );
};

export default PolicyList;