import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { loginAPI, getGoogleAuthUrlAPI, resetPasswordAPI } from '../../api/auth';
import { setAuthSession } from '../../api/authSession';
import { Mail, Lock, ArrowRight, AlertCircle, X } from 'lucide-react';

const LoginPage = () => {
    const navigate = useNavigate();
    const [formData, setFormData] = useState({ email: '', password: '' });
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    // 비밀번호 찾기 모달 상태
    const [isResetModalOpen, setIsResetModalOpen] = useState(false);
    const [resetData, setResetData] = useState({ email: '', name: '' });
    const [resetError, setResetError] = useState('');
    const [isResetLoading, setIsResetLoading] = useState(false);
    const [tempPassword, setTempPassword] = useState('');

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
        if (error) setError('');
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!formData.email || !formData.password) {
            setError('이메일과 비밀번호를 모두 입력해주세요.');
            return;
        }

        setIsLoading(true);
        try {
            const result = await loginAPI(formData.email, formData.password);

            const token = result?.data?.accessToken || result?.accessToken;

            if (token) {
                // 🚀 [여기가 추가된 부분입니다!]
                // 백엔드에서 넘겨주는 유저 이름(name)을 찾아서 저장하고, 만약 못 찾으면 로그인한 이메일을 대신 저장합니다.
                const userName = result?.data?.user?.name || result?.data?.name || formData.email;
                setAuthSession({ accessToken: token, userName });
            } else {
                console.warn('[LoginPage] 토큰을 응답에서 찾을 수 없습니다.', result);
            }

            // SPA 네비게이션을 사용하여 XHR 로그가 날아가지 않도록 navigate 사용
            navigate('/');
        } catch (err) {
            console.error('[LoginPage] 로그인 에러:', err);
            setError(err.response?.data?.message || '로그인에 실패했습니다. 이메일과 비밀번호를 확인해주세요.');
        } finally {
            setIsLoading(false);
        }
    };

    const handleGoogleLogin = async () => {
        try {
            const result = await getGoogleAuthUrlAPI();
            if (result?.data?.redirectUrl) {
                window.location.href = result.data.redirectUrl;
            } else {
                setError('Google 로그인 URL을 받아오지 못했습니다.');
            }
        } catch (err) {
            console.error('[LoginPage] Google 로그인 URL 요청 에러:', err);
            setError('Google 로그인 초기화에 실패했습니다.');
        }
    };

    const handleResetPassword = async (e) => {
        e.preventDefault();
        
        if (!resetData.email || !resetData.name) {
            setResetError('이메일과 이름을 모두 입력해주세요.');
            return;
        }

        setIsResetLoading(true);
        setResetError('');
        setTempPassword('');
        
        try {
            const result = await resetPasswordAPI(resetData.email, resetData.name);
            if (result?.data?.temporaryPassword) {
                const newTempPassword = result.data.temporaryPassword;
                setTempPassword(newTempPassword);
                
                // 로그인 폼 자동 채우기
                setFormData(prev => ({
                    ...prev,
                    email: resetData.email,
                    password: newTempPassword
                }));
            } else {
                setResetError('임시 비밀번호를 받아오지 못했습니다.');
            }
        } catch (err) {
            console.error('[LoginPage] 비밀번호 찾기 에러:', err);
            setResetError(err.response?.data?.message || '비밀번호 찾기에 실패했습니다. 입력 정보를 확인해주세요.');
        } finally {
            setIsResetLoading(false);
        }
    };

    return (
        <div className="min-h-[calc(100vh-80px)] flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
            <div className="max-w-md w-full space-y-8 bg-white p-10 rounded-2xl shadow-xl border border-gray-100">
                <div>
                    <h2 className="mt-2 text-center text-3xl font-extrabold text-gray-900 tracking-tight">
                        환영합니다! 👋
                    </h2>
                    <p className="mt-4 text-center text-sm text-gray-600">
                        계정에 로그인하여 서비스를 자유롭게 이용해보세요.
                    </p>
                </div>
                
                <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
                    <div className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">
                                이메일
                            </label>
                            <div className="relative">
                                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                    <Mail className="h-5 w-5 text-gray-400" />
                                </div>
                                <input
                                    name="email"
                                    type="email"
                                    required
                                    className="pl-10 appearance-none block w-full px-3 py-3 border border-gray-300 rounded-xl placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors sm:text-sm"
                                    placeholder="your@email.com"
                                    value={formData.email}
                                    onChange={handleChange}
                                />
                            </div>
                        </div>
                        
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">
                                비밀번호
                            </label>
                            <div className="relative">
                                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                    <Lock className="h-5 w-5 text-gray-400" />
                                </div>
                                <input
                                    name="password"
                                    type="password"
                                    required
                                    className="pl-10 appearance-none block w-full px-3 py-3 border border-gray-300 rounded-xl placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors sm:text-sm"
                                    placeholder="••••••••"
                                    value={formData.password}
                                    onChange={handleChange}
                                />
                            </div>
                        </div>
                    </div>

                    {error && (
                        <div className="flex items-center space-x-2 text-red-600 text-sm bg-red-50 p-3 rounded-lg">
                            <AlertCircle className="h-4 w-4 shrink-0" />
                            <span>{error}</span>
                        </div>
                    )}

                    <div className="flex items-center justify-between">
                        <div className="flex items-center">
                            <input
                                id="remember-me"
                                name="remember-me"
                                type="checkbox"
                                className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                            />
                            <label htmlFor="remember-me" className="ml-2 block text-sm text-gray-700">
                                로그인 유지
                            </label>
                        </div>

                        <div className="text-sm">
                            <button
                                type="button"
                                onClick={() => {
                                    setIsResetModalOpen(true);
                                    setResetData({ email: formData.email, name: '' }); // 이메일이 입력되어 있다면 기본값으로 세팅
                                    setResetError('');
                                    setTempPassword('');
                                }}
                                className="font-medium text-blue-600 hover:text-blue-500 bg-transparent border-none p-0 cursor-pointer"
                            >
                                비밀번호 찾기
                            </button>
                        </div>
                    </div>

                    <div>
                        <button
                            type="submit"
                            disabled={isLoading}
                            className="group relative w-full flex justify-center py-3 px-4 border border-transparent text-sm font-medium rounded-xl text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-all disabled:opacity-70 disabled:cursor-not-allowed shadow-md hover:shadow-lg"
                        >
                            {isLoading ? (
                                <span className="flex items-center">
                                    <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                    </svg>
                                    로그인 중...
                                </span>
                            ) : (
                                <span className="flex items-center">
                                    로그인
                                    <ArrowRight className="ml-2 h-4 w-4 group-hover:translate-x-1 transition-transform" />
                                </span>
                            )}
                        </button>
                    </div>

                    <div className="relative my-4">
                        <div className="absolute inset-0 flex items-center">
                            <div className="w-full border-t border-gray-200"></div>
                        </div>
                        <div className="relative flex justify-center text-sm">
                            <span className="px-2 bg-white text-gray-500">또는</span>
                        </div>
                    </div>

                    <div>
                        <button
                            type="button"
                            onClick={handleGoogleLogin}
                            className="w-full flex justify-center py-3 px-4 border border-gray-300 text-sm font-medium rounded-xl text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 shadow-sm hover:shadow transition-all items-center"
                        >
                            <svg className="w-5 h-5 mr-2" viewBox="0 0 24 24">
                                <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
                                <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
                                <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/>
                                <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
                            </svg>
                            Google로 로그인
                        </button>
                    </div>
                </form>
                
                <div className="mt-6">
                    <div className="relative">
                        <div className="absolute inset-0 flex items-center">
                            <div className="w-full border-t border-gray-200"></div>
                        </div>
                        <div className="relative flex justify-center text-sm">
                            <span className="px-2 bg-white text-gray-500">계정이 없으신가요?</span>
                        </div>
                    </div>

                    <div className="mt-6 text-center">
                        <Link to="/signup" className="font-medium text-blue-600 hover:text-blue-500">
                            회원가입 하러가기
                        </Link>
                    </div>
                </div>
            </div>

            {/* 비밀번호 찾기 모달 */}
            {isResetModalOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50 p-4">
                    <div className="w-full max-w-md rounded-2xl bg-white p-8 shadow-xl">
                        <div className="flex items-center justify-between mb-6">
                            <h3 className="text-xl font-bold text-gray-900">비밀번호 찾기</h3>
                            <button 
                                onClick={() => setIsResetModalOpen(false)}
                                className="text-gray-400 hover:text-gray-600"
                            >
                                <X className="h-6 w-6" />
                            </button>
                        </div>

                        {tempPassword ? (
                            <div className="text-center space-y-6">
                                <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-green-100">
                                    <Lock className="h-8 w-8 text-green-600" />
                                </div>
                                <div>
                                    <p className="text-sm text-gray-600 mb-2">임시 비밀번호가 발급되었습니다.</p>
                                    <div className="bg-gray-100 p-4 rounded-xl font-mono text-lg font-bold tracking-wider text-gray-900 break-all">
                                        {tempPassword}
                                    </div>
                                    <p className="text-xs text-gray-500 mt-2">
                                        보안을 위해 로그인 후 반드시 비밀번호를 변경해주세요.<br/>
                                        (입력창에 자동 입력되었습니다.)
                                    </p>
                                </div>
                                <button
                                    onClick={() => setIsResetModalOpen(false)}
                                    className="w-full rounded-xl bg-blue-600 py-3 text-sm font-bold text-white transition-colors hover:bg-blue-700"
                                >
                                    로그인 하러가기
                                </button>
                            </div>
                        ) : (
                            <form onSubmit={handleResetPassword} className="space-y-4">
                                <p className="text-sm text-gray-600 mb-4">
                                    가입 시 등록한 이메일과 이름을 입력하시면 임시 비밀번호를 발급해 드립니다.
                                </p>
                                
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        이메일
                                    </label>
                                    <input
                                        type="email"
                                        required
                                        className="appearance-none block w-full px-3 py-3 border border-gray-300 rounded-xl placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent sm:text-sm"
                                        placeholder="your@email.com"
                                        value={resetData.email}
                                        onChange={(e) => {
                                            setResetData({ ...resetData, email: e.target.value });
                                            if (resetError) setResetError('');
                                        }}
                                    />
                                </div>
                                
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        이름
                                    </label>
                                    <input
                                        type="text"
                                        required
                                        className="appearance-none block w-full px-3 py-3 border border-gray-300 rounded-xl placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent sm:text-sm"
                                        placeholder="홍길동"
                                        value={resetData.name}
                                        onChange={(e) => {
                                            setResetData({ ...resetData, name: e.target.value });
                                            if (resetError) setResetError('');
                                        }}
                                    />
                                </div>

                                {resetError && (
                                    <div className="flex items-center space-x-2 text-red-600 text-sm bg-red-50 p-3 rounded-lg">
                                        <AlertCircle className="h-4 w-4 shrink-0" />
                                        <span>{resetError}</span>
                                    </div>
                                )}

                                <button
                                    type="submit"
                                    disabled={isResetLoading}
                                    className="mt-6 w-full flex justify-center py-3 px-4 border border-transparent text-sm font-medium rounded-xl text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-70"
                                >
                                    {isResetLoading ? '처리 중...' : '임시 비밀번호 발급'}
                                </button>
                            </form>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
};

export default LoginPage;
