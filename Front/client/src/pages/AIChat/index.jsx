import React, { useEffect, useState } from 'react';
import { Bot, Clock, RotateCcw } from 'lucide-react';
import ChatMessageList from '../../components/Chatbot/ChatMessageList';
import ChatInput from '../../components/Chatbot/ChatInput';
import ChatHistorySidebar from '../../components/Chatbot/ChatHistorySidebar';

const RESPONSE_TYPES = {
    POLICY_SEARCH: 'POLICY_SEARCH',
    CLARIFICATION_NEEDED: 'CLARIFICATION_NEEDED',
    SMALLTALK: 'SMALLTALK',
    OFF_TOPIC: 'OFF_TOPIC',
    SAFETY: 'SAFETY'
};

const normalizeMessage = (value) => (value || '').trim().toLowerCase().replace(/\s+/g, ' ');
const matchesAny = (text, patterns) => patterns.some((pattern) => pattern.test(text));

const classifierRules = {
    safety: [
        /죽고\s?싶/,
        /자살/,
        /응급/,
        /숨이?\s?안/,
        /호흡이?\s?안/,
        /너무\s?아파/,
        /심하게\s?아파/,
        /쓰러질\s?것\s?같/,
        /의식이\s?흐려/
    ],
    clarification: [
        /가난/,
        /생활이?\s?너무\s?힘들/,
        /생활이?\s?힘들/,
        /돈이?\s?없/,
        /월세.*힘들/,
        /생계.*어렵/,
        /버티기\s?힘들/,
        /실직했/,
        /구직\s?중/,
        /막막/
    ],
    policy: [
        /청년.*주거.*지원/,
        /주거.*지원.*알려/,
        /지원금.*알려/,
        /정책.*추천/,
        /청년.*지원/,
        /신청\s?조건/,
        /조건\s?더\s?보기/,
        /주거\s?지원/,
        /생계\s?지원/,
        /긴급복지/
    ],
    smalltalk: [
        /안녕/,
        /반가/,
        /고마/,
        /감사/,
        /하이/
    ]
};

const createEmptyIntakeProfile = () => ({
    concernMessage: '',
    ageGroup: '',
    householdType: '',
    employmentStatus: '',
    housingStatus: '',
    awaitingField: ''
});

const classifyMockMessage = (message) => {
    const normalized = normalizeMessage(message);

    if (matchesAny(normalized, classifierRules.safety)) {
        return RESPONSE_TYPES.SAFETY;
    }
    if (matchesAny(normalized, classifierRules.clarification)) {
        return RESPONSE_TYPES.CLARIFICATION_NEEDED;
    }
    if (matchesAny(normalized, classifierRules.policy)) {
        return RESPONSE_TYPES.POLICY_SEARCH;
    }
    if (matchesAny(normalized, classifierRules.smalltalk)) {
        return RESPONSE_TYPES.SMALLTALK;
    }
    return RESPONSE_TYPES.OFF_TOPIC;
};

const extractProfileFromMessage = (message) => {
    const normalized = normalizeMessage(message);

    const nextProfile = {
        ageGroup: '',
        householdType: '',
        employmentStatus: '',
        housingStatus: ''
    };

    if (/10대|십대|고등학생|중학생/.test(normalized)) {
        nextProfile.ageGroup = '10대';
    } else if (/20대|이십대|청년|대학생|스무살|20살/.test(normalized)) {
        nextProfile.ageGroup = '청년';
    } else if (/30대|40대|50대|중장년|삼십대|사십대|오십대/.test(normalized)) {
        nextProfile.ageGroup = '중장년';
    }

    if (/혼자|1인 가구|자취|독립해서/.test(normalized)) {
        nextProfile.householdType = '혼자 거주';
    } else if (/가족|부모|함께 살아|같이 살아|동거/.test(normalized)) {
        nextProfile.householdType = '가족과 거주';
    }

    if (/구직 중|취준|취업 준비/.test(normalized)) {
        nextProfile.employmentStatus = '구직 중';
    } else if (/무직|백수|실직/.test(normalized)) {
        nextProfile.employmentStatus = '무직';
    } else if (/일하고 있어|재직|직장/.test(normalized)) {
        nextProfile.employmentStatus = '재직 중';
    }

    if (/월세/.test(normalized)) {
        nextProfile.housingStatus = '월세 거주';
    } else if (/전세/.test(normalized)) {
        nextProfile.housingStatus = '전세 거주';
    }

    return nextProfile;
};

const profileHasAnyValue = (profile) =>
    Boolean(profile.ageGroup || profile.householdType || profile.employmentStatus || profile.housingStatus);

const buildPolicyReason = (profile) => {
    const segments = [];
    if (profile.ageGroup) {
        segments.push(profile.ageGroup);
    }
    if (profile.householdType) {
        segments.push(profile.householdType);
    }
    if (profile.employmentStatus) {
        segments.push(profile.employmentStatus);
    }
    if (profile.housingStatus) {
        segments.push(profile.housingStatus);
    }
    return segments.length > 0 ? segments.join(', ') : '현재 상황';
};

const filterPoliciesByMessage = (policies, message) => {
    const normalized = normalizeMessage(message);

    if (/주거/.test(normalized)) {
        return policies.filter((policy) => /주거|월세|전세/.test(policy.purposeSummary + policy.serviceName));
    }
    if (/청년/.test(normalized)) {
        return policies.filter((policy) => /청년/.test(policy.serviceName + policy.target));
    }
    if (/생계|긴급복지/.test(normalized)) {
        return policies.filter((policy) => /생계|긴급복지/.test(policy.serviceName + policy.purposeSummary));
    }
    return policies;
};

const buildPolicySearchPayload = (base, message, profile, answerOverride = '') => {
    const policies = filterPoliciesByMessage(buildPolicyCards(), message);
    return {
        ...base,
        answer:
            answerOverride ||
            `${buildPolicyReason(profile)} 기준으로 확인해볼 수 있는 정책들을 찾았어요. 아래 추천 정책과 신청 조건을 함께 확인해보세요.`,
        policies,
        references: policies.map(({ serviceId, serviceName, detailUrl }) => ({ serviceId, serviceName, detailUrl })),
        matchedPolicyCount: policies.length,
        suggestedReplies: [
            { label: '신청 조건 더 보기', value: '신청 조건 더 보기' },
            { label: '주거 지원만 다시 보기', value: '주거 지원만 다시 보기' },
            { label: '청년 대상만 보기', value: '청년 대상만 보기' }
        ]
    };
};

const mergeProfile = (profile, extracted, fallbackConcernMessage = '') => ({
    concernMessage: profile.concernMessage || fallbackConcernMessage,
    ageGroup: profile.ageGroup || extracted.ageGroup,
    householdType: profile.householdType || extracted.householdType,
    employmentStatus: profile.employmentStatus || extracted.employmentStatus,
    housingStatus: profile.housingStatus || extracted.housingStatus,
    awaitingField: profile.awaitingField
});

const getNextQuestionField = (profile) => {
    if (!profile.ageGroup) {
        return 'ageGroup';
    }
    if (!profile.householdType) {
        return 'householdType';
    }
    if (!profile.employmentStatus) {
        return 'employmentStatus';
    }
    if (!profile.housingStatus) {
        return 'housingStatus';
    }
    return '';
};

const getQuestionCopy = (field) => {
    switch (field) {
        case 'ageGroup':
            return {
                answer: '생활이 많이 부담되실 수 있겠어요. 맞는 지원 정책을 찾으려면 현재 연령대를 알려주세요.',
                suggestedReplies: [
                    { label: '10대', value: '지금 10대야' },
                    { label: '청년', value: '지금 20대야' },
                    { label: '중장년', value: '지금 40대야' }
                ]
            };
        case 'householdType':
            return {
                answer: '알겠습니다. 현재 혼자 거주 중이신가요, 가족과 함께 거주 중이신가요?',
                suggestedReplies: [
                    { label: '혼자 거주', value: '혼자 살아' },
                    { label: '가족과 거주', value: '가족과 같이 살아' }
                ]
            };
        case 'employmentStatus':
            return {
                answer: '좋아요. 현재 일하고 계신가요, 아니면 구직 중이신가요?',
                suggestedReplies: [
                    { label: '구직 중', value: '구직 중이야' },
                    { label: '무직', value: '지금 무직이야' },
                    { label: '재직 중', value: '현재 일하고 있어' }
                ]
            };
        case 'housingStatus':
            return {
                answer: '마지막으로 현재 월세, 전세, 자가 중 어떤 주거 형태인지 알려주세요.',
                suggestedReplies: [
                    { label: '월세 거주', value: '월세에 살고 있어' },
                    { label: '전세 거주', value: '전세에 살고 있어' }
                ]
            };
        default:
            return {
                answer: '',
                suggestedReplies: []
            };
    }
};

const buildRetryQuestion = (field) => {
    const question = getQuestionCopy(field);
    return {
        answer: `잘 들었어요. ${question.answer}`,
        suggestedReplies: question.suggestedReplies
    };
};

const buildPolicyCards = () => ([
    {
        serviceId: 'p1',
        serviceName: '청년 월세 한시 특별지원',
        purposeSummary: '청년층의 월세 부담을 줄이기 위한 주거 지원 정책입니다.',
        target: '만 19세~34세의 독립 거주 청년',
        supportContent: '월 최대 20만 원씩 최대 12개월 지원',
        applicationMethod: '복지로 온라인 또는 주민센터 방문 신청',
        applicationDeadline: '상시 또는 공고 확인',
        detailUrl: 'https://www.bokjiro.go.kr',
        orgName: '국토교통부',
        departmentName: '청년주거지원과',
        reason: '청년과 주거비 부담을 함께 언급해 주거 지원 정책으로 연결했습니다.'
    },
    {
        serviceId: 'p2',
        serviceName: '청년 전세보증금 대출 이자지원',
        purposeSummary: '전세보증금 대출 이자 부담을 완화하는 청년 대상 정책입니다.',
        target: '무주택 청년 가구',
        supportContent: '전세대출 이자 일부 지원',
        applicationMethod: '지자체 홈페이지 또는 위탁기관 신청',
        applicationDeadline: '상시 또는 지자체 공고 확인',
        detailUrl: 'https://www.bokjiro.go.kr',
        orgName: '지방자치단체',
        departmentName: '청년정책과',
        reason: '청년 주거 안정과 직접 연결되는 정책입니다.'
    },
    {
        serviceId: 'p3',
        serviceName: '긴급복지 생계지원',
        purposeSummary: '갑작스러운 위기 상황에 생계비를 지원하는 정책입니다.',
        target: '실직, 소득 감소 등으로 생계가 어려운 가구',
        supportContent: '생계비, 의료비 등 긴급 지원',
        applicationMethod: '읍면동 주민센터 또는 보건복지상담센터 문의',
        applicationDeadline: '수시',
        detailUrl: 'https://www.bokjiro.go.kr',
        orgName: '보건복지부',
        departmentName: '긴급복지지원과',
        reason: '생활고나 생계 곤란 표현과 연결되는 정책이라 함께 확인할 수 있습니다.'
    }
]);

const buildMockResponse = (text, sessionId, intakeProfile) => {
    const normalized = normalizeMessage(text);
    const extractedProfile = extractProfileFromMessage(text);
    const isClarificationReply =
        Boolean(intakeProfile.awaitingField) &&
        Object.values(extractedProfile).some(Boolean);

    if (Boolean(intakeProfile.awaitingField) && !profileHasAnyValue(extractedProfile)) {
        const retryQuestion = buildRetryQuestion(intakeProfile.awaitingField);
        return {
            data: {
                sessionId: sessionId || 'mock-session-123',
                answer: retryQuestion.answer,
                responseType: RESPONSE_TYPES.CLARIFICATION_NEEDED,
                suggestedReplies: retryQuestion.suggestedReplies,
                policies: [],
                references: [],
                matchedPolicyCount: 0
            },
            nextProfile: intakeProfile
        };
    }

    if (isClarificationReply) {
        const mergedProfile = mergeProfile(intakeProfile, extractedProfile);

        if (mergedProfile.ageGroup === '10대') {
            return {
                data: {
                    sessionId: sessionId || 'mock-session-123',
                    answer: '알겠습니다. 10대라면 본인 대상 정책뿐 아니라 보호자와 함께 신청하는 지원도 확인해볼 수 있어요. 현재 혼자 거주 중이신가요, 가족과 함께 거주 중이신가요?',
                    responseType: RESPONSE_TYPES.CLARIFICATION_NEEDED,
                    suggestedReplies: [
                        { label: '가족과 거주', value: '가족과 같이 살아' },
                        { label: '혼자 거주', value: '혼자 살아' }
                    ],
                    policies: [],
                    references: [],
                    matchedPolicyCount: 0
                },
                nextProfile: {
                    ...mergedProfile,
                    awaitingField: 'householdType'
                }
            };
        }

        const nextField = getNextQuestionField(mergedProfile);

        if (nextField) {
            const nextQuestion = getQuestionCopy(nextField);
            return {
                data: {
                    sessionId: sessionId || 'mock-session-123',
                    answer: nextQuestion.answer,
                    responseType: RESPONSE_TYPES.CLARIFICATION_NEEDED,
                    suggestedReplies: nextQuestion.suggestedReplies,
                    policies: [],
                    references: [],
                    matchedPolicyCount: 0
                },
                nextProfile: {
                    ...mergedProfile,
                    awaitingField: nextField
                }
            };
        }

        return {
            data: {
                ...buildPolicySearchPayload(
                    {
                        sessionId: sessionId || 'mock-session-123',
                        answer: '',
                        responseType: RESPONSE_TYPES.POLICY_SEARCH,
                        suggestedReplies: [],
                        policies: [],
                        references: [],
                        matchedPolicyCount: 0
                    },
                    mergedProfile.concernMessage || text,
                    mergedProfile
                )
            },
            nextProfile: createEmptyIntakeProfile()
        };
    }

    const responseType = classifyMockMessage(text);
    const base = {
        sessionId: sessionId || 'mock-session-123',
        answer: '',
        responseType,
        suggestedReplies: [],
        policies: [],
        references: [],
        matchedPolicyCount: 0
    };

    switch (responseType) {
        case RESPONSE_TYPES.SAFETY:
            return {
                data: {
                    ...base,
                    answer: '지금은 정책 안내보다 즉시 도움을 받는 게 우선일 수 있어요. 응급 상황이면 119나 가까운 응급실에 바로 연락해 주세요.',
                    suggestedReplies: [
                        { label: '긴급복지 문의', value: '긴급복지 지원 알려줘' },
                        { label: '생계 지원 문의', value: '생계 지원 정책 알려줘' }
                    ]
                },
                nextProfile: createEmptyIntakeProfile()
            };
        case RESPONSE_TYPES.CLARIFICATION_NEEDED:
            return {
                data: {
                    ...base,
                    answer: '생활이 많이 부담되실 수 있겠어요. 맞는 지원 정책을 찾으려면 현재 연령대를 알려주세요.',
                    suggestedReplies: [
                        { label: '10대', value: '지금 10대야' },
                        { label: '청년', value: '지금 20대야' },
                        { label: '중장년', value: '지금 40대야' }
                    ]
                },
                nextProfile: {
                    concernMessage: normalized,
                    ageGroup: '',
                    householdType: '',
                    employmentStatus: '',
                    housingStatus: '',
                    awaitingField: 'ageGroup'
                }
            };
        case RESPONSE_TYPES.POLICY_SEARCH: {
            return {
                data: {
                    ...buildPolicySearchPayload(
                        base,
                        text,
                        createEmptyIntakeProfile(),
                        /신청\s?조건/.test(normalized)
                            ? '신청 조건을 중심으로 다시 볼 수 있는 정책들을 정리했어요. 카드에서 대상과 신청 방법을 먼저 확인해보세요.'
                            : ''
                    )
                },
                nextProfile: createEmptyIntakeProfile()
            };
        }
        case RESPONSE_TYPES.SMALLTALK:
            return {
                data: {
                    ...base,
                    answer: '안녕하세요. 복지 정책 안내 챗봇이에요. 궁금한 지원 분야를 말씀해 주시면 도와드릴게요.',
                    suggestedReplies: [
                        { label: '청년 주거 지원', value: '청년 주거 지원 알려줘' },
                        { label: '생활비 지원', value: '생활이 너무 힘들어' }
                    ]
                },
                nextProfile: createEmptyIntakeProfile()
            };
        default:
            return {
                data: {
                    ...base,
                    answer: '저는 복지 정책 안내 챗봇이에요. 청년, 주거, 취업, 생계 지원 같은 질문을 주시면 더 정확하게 도와드릴 수 있어요.',
                    suggestedReplies: [
                        { label: '청년 지원', value: '청년 지원 정책 알려줘' },
                        { label: '주거 지원', value: '주거 지원 정책 알려줘' },
                        { label: '취업 지원', value: '취업 지원 정책 알려줘' }
                    ]
                },
                nextProfile: createEmptyIntakeProfile()
            };
    }
};

const getInitialMessage = () => ({
    id: 'welcome',
    sender: 'bot',
    answer: '안녕하세요. 복지 정책 상담형 안내 도우미입니다.\n\n현재 겪고 계신 어려움이나 궁금하신 지원 분야를 편하게 말씀해주세요. 예: 생활이 너무 힘들어, 청년 주거 지원 알려줘',
    responseType: RESPONSE_TYPES.SMALLTALK,
    suggestedReplies: [
        { label: '청년 주거 지원', value: '청년 주거 지원 알려줘' },
        { label: '취업 지원금', value: '취업 지원 정책 알려줘' },
        { label: '긴급 생계 지원', value: '생활이 너무 힘들어' }
    ],
    timestamp: new Date().toISOString()
});

const mockAskChatbot = async (text, sessionId, intakeProfile) =>
    new Promise((resolve) => {
        setTimeout(() => {
            resolve({
                resultCode: 'S-1',
                ...buildMockResponse(text, sessionId, intakeProfile)
            });
        }, 500);
    });

const AIChatPage = () => {
    const [messages, setMessages] = useState([getInitialMessage()]);
    const [sessionId, setSessionId] = useState(null);
    const [localSessionId, setLocalSessionId] = useState(() => Date.now().toString());
    const [isLoading, setIsLoading] = useState(false);
    const [chatSessions, setChatSessions] = useState([]);
    const [isSidebarOpen, setIsSidebarOpen] = useState(false);
    const [mockIntakeProfile, setMockIntakeProfile] = useState(createEmptyIntakeProfile());

    useEffect(() => {
        const stored = localStorage.getItem('chat_sessions');
        if (!stored) {
            return;
        }

        try {
            setChatSessions(JSON.parse(stored));
        } catch (error) {
            console.error('Failed to parse chat sessions', error);
        }
    }, []);

    useEffect(() => {
        if (messages.length <= 1) {
            return;
        }

        setChatSessions((previousSessions) => {
            const existingIndex = previousSessions.findIndex((session) => session.id === localSessionId);
            const userMessages = messages.filter((message) => message.sender === 'user');
            const title = userMessages.length > 0 ? userMessages[0].text : '새로운 대화';

            const nextSession = {
                id: localSessionId,
                backendSessionId: sessionId,
                title,
                timestamp: new Date().toISOString(),
                messages
            };

            let updatedSessions;
            if (existingIndex >= 0) {
                updatedSessions = [...previousSessions];
                updatedSessions[existingIndex] = nextSession;
            } else {
                updatedSessions = [nextSession, ...previousSessions];
            }

            localStorage.setItem('chat_sessions', JSON.stringify(updatedSessions));
            return updatedSessions;
        });
    }, [localSessionId, messages, sessionId]);

    const handleReset = () => {
        const shouldReset = window.confirm('새로운 대화를 시작할까요? 이전 대화는 히스토리에 저장됩니다.');
        if (!shouldReset) {
            return;
        }

        setMessages([getInitialMessage()]);
        setSessionId(null);
        setLocalSessionId(Date.now().toString());
        setIsSidebarOpen(false);
        setMockIntakeProfile(createEmptyIntakeProfile());
    };

    const handleSelectSession = (session) => {
        setMessages(session.messages);
        setSessionId(session.backendSessionId);
        setLocalSessionId(session.id);
        setIsSidebarOpen(false);
        setMockIntakeProfile(createEmptyIntakeProfile());
    };

    const handleSend = async (text) => {
        if (!text.trim() || isLoading) {
            return;
        }

        const userMessageId = Date.now().toString();
        setMessages((previous) => [
            ...previous,
            {
                id: userMessageId,
                sender: 'user',
                text,
                timestamp: new Date().toISOString()
            }
        ]);

        const typingMessageId = (Date.now() + 1).toString();
        setMessages((previous) => [...previous, { id: typingMessageId, sender: 'bot', isTyping: true }]);
        setIsLoading(true);

        try {
            const response = await mockAskChatbot(text, sessionId, mockIntakeProfile);
            setMessages((previous) => previous.filter((message) => message.id !== typingMessageId));

            if (response?.resultCode !== 'S-1' || !response.data) {
                throw new Error(response?.message || 'Invalid response');
            }

            const {
                answer,
                matchedPolicyCount,
                policies,
                references,
                responseType,
                suggestedReplies
            } = response.data;

            if (response.data.sessionId) {
                setSessionId(response.data.sessionId);
            }
            setMockIntakeProfile(response.nextProfile || createEmptyIntakeProfile());

            const botMessageId = (Date.now() + 2).toString();
            setMessages((previous) => [
                ...previous,
                {
                    id: botMessageId,
                    sender: 'bot',
                    answer,
                    responseType: responseType || RESPONSE_TYPES.POLICY_SEARCH,
                    suggestedReplies: suggestedReplies || [],
                    policies: policies || [],
                    references: references || [],
                    matchedPolicyCount: matchedPolicyCount ?? 0,
                    timestamp: new Date().toISOString()
                }
            ]);
        } catch (error) {
            console.error('Chat API error:', error);
            setMessages((previous) => previous.filter((message) => message.id !== typingMessageId));

            const errorMessageId = (Date.now() + 2).toString();
            setMessages((previous) => [
                ...previous,
                {
                    id: errorMessageId,
                    sender: 'system',
                    text: '서버와 연결하는 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.',
                    timestamp: new Date().toISOString()
                }
            ]);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="relative flex min-h-[calc(100vh-140px)] flex-col overflow-hidden bg-gray-50 px-4 pb-4 pt-20 md:px-0">
            <div className="relative mx-auto flex w-full max-w-3xl flex-1 flex-col overflow-hidden rounded-2xl border border-gray-100 bg-white shadow-lg">
                <div className="relative z-10 flex shrink-0 items-center justify-between bg-blue-600 p-4 text-center text-white shadow-md">
                    <div className="w-16">
                        {sessionId && (
                            <span className="whitespace-nowrap rounded-full bg-blue-700 px-2 py-1 text-[10px] opacity-80">
                                이전 질문 기준 추천
                            </span>
                        )}
                    </div>

                    <div className="flex flex-col items-center">
                        <h2 className="flex items-center justify-center text-lg font-bold">
                            <Bot className="mr-2" />
                            복지 정책 안내 챗봇
                        </h2>
                        <p className="mt-1 text-xs text-blue-100">상황에 맞는 복지 정책을 찾아드려요.</p>
                    </div>

                    <div className="flex w-16 justify-end gap-1">
                        <button
                            onClick={handleReset}
                            className="rounded p-2 text-white transition-colors hover:bg-blue-700"
                            title="새 대화 시작"
                            aria-label="새 대화 시작"
                        >
                            <RotateCcw size={18} />
                        </button>
                        <button
                            onClick={() => setIsSidebarOpen(true)}
                            className="rounded p-2 text-white transition-colors hover:bg-blue-700"
                            title="대화 히스토리"
                            aria-label="대화 히스토리"
                        >
                            <Clock size={18} />
                        </button>
                    </div>
                </div>

                <ChatMessageList messages={messages} onChipClick={handleSend} />
                <ChatInput onSend={handleSend} disabled={isLoading} />
            </div>

            <ChatHistorySidebar
                isOpen={isSidebarOpen}
                onClose={() => setIsSidebarOpen(false)}
                sessions={chatSessions}
                onSelectSession={handleSelectSession}
            />
        </div>
    );
};

export default AIChatPage;
