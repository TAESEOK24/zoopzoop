import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { googleLoginCallbackAPI } from '../../api/auth';
import { setAuthSession } from '../../api/authSession';
import { AlertCircle } from 'lucide-react';

const GoogleCallback = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const [error, setError] = useState('');
    const [isProcessing, setIsProcessing] = useState(true);

    const hasProcessed = React.useRef(false);

    useEffect(() => {
        const processCallback = async () => {
            if (hasProcessed.current) return;
            hasProcessed.current = true;

            try {
                const code = searchParams.get('code');
                if (!code) {
                    throw new Error('Google 인증 코드를 찾을 수 없습니다.');
                }

                const result = await googleLoginCallbackAPI(code);
                
                const token = result?.data?.accessToken || result?.accessToken;
                if (!token) {
                    throw new Error('인증 토큰을 받아오지 못했습니다.');
                }

                // 유저 정보 저장 (이름 또는 이메일)
                const user = result?.data?.user;
                const userName = user?.name || result?.data?.name || user?.email || 'Google User';

                setAuthSession({ accessToken: token, userName });

                // 메인 페이지로 이동
                navigate('/');
            } catch (err) {
                console.error('[GoogleCallback] Google 로그인 처리 중 오류 발생:', err);
                setError(err.response?.data?.message || err.message || 'Google 로그인 처리 중 오류가 발생했습니다.');
                setIsProcessing(false);
            }
        };

        processCallback();
    }, [searchParams, navigate]);

    if (error) {
        return (
            <div className="min-h-[calc(100vh-80px)] flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
                <div className="max-w-md w-full space-y-8 bg-white p-10 rounded-2xl shadow-xl border border-gray-100 text-center">
                    <div className="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-red-100">
                        <AlertCircle className="h-6 w-6 text-red-600" />
                    </div>
                    <h2 className="mt-6 text-center text-2xl font-extrabold text-gray-900">로그인 실패</h2>
                    <p className="mt-2 text-center text-sm text-gray-600">
                        {error}
                    </p>
                    <div className="mt-6">
                        <button
                            onClick={() => navigate('/login')}
                            className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                        >
                            로그인 페이지로 돌아가기
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-[calc(100vh-80px)] flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
            <div className="max-w-md w-full space-y-8 bg-white p-10 rounded-2xl shadow-xl border border-gray-100 text-center">
                <div className="flex justify-center">
                    <svg className="animate-spin h-10 w-10 text-blue-600" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                </div>
                <h2 className="mt-6 text-center text-2xl font-extrabold text-gray-900">Google 로그인 처리 중...</h2>
                <p className="mt-2 text-center text-sm text-gray-600">
                    잠시만 기다려주세요.
                </p>
            </div>
        </div>
    );
};

export default GoogleCallback;
