import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
    createComment,
    deleteComment,
    deleteCommunityPost,
    fetchComments,
    fetchCommunityPostDetail,
    updateComment,
} from '../../api/community';
import axiosInstance from '../../api/index';
import { getAccessToken } from '../../api/authSession';
import { AlertTriangle, Clock, Eye, MessageSquare, User } from 'lucide-react';

const PostDetail = () => {
    const { id } = useParams();
    const navigate = useNavigate();

    const [post, setPost] = useState(null);
    const [comments, setComments] = useState([]);
    const [newComment, setNewComment] = useState('');
    const [currentUser, setCurrentUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [editingCommentId, setEditingCommentId] = useState(null);
    const [editingCommentContent, setEditingCommentContent] = useState('');
    const [isReportModalOpen, setIsReportModalOpen] = useState(false);
    const [reportTarget, setReportTarget] = useState({ type: '', id: null });
    const [reportReason, setReportReason] = useState('');

    const reloadComments = async () => {
        const commentsRes = await fetchComments(id);
        setComments(commentsRes.data.data || []);
    };

    useEffect(() => {
        const loadData = async () => {
            try {
                if (getAccessToken()) {
                    const userRes = await axiosInstance.get('/api/users/me');
                    setCurrentUser(userRes.data.data);
                }

                const postRes = await fetchCommunityPostDetail(id);
                setPost(postRes.data.data || postRes.data);
                await reloadComments();
            } catch (error) {
                alert('게시글을 불러올 수 없습니다.');
                navigate('/community');
            } finally {
                setLoading(false);
            }
        };

        loadData();
    }, [id, navigate]);

    const isAdmin = currentUser?.role === 'ADMIN';
    const canManagePost = currentUser?.name === post?.author || isAdmin;
    const canReportPost = currentUser && currentUser.name !== post?.author && !isAdmin;
    const canManageComment = (comment) => currentUser?.name === comment.author || isAdmin;
    const canReportComment = (comment) => currentUser && currentUser.name !== comment.author && !isAdmin;

    const handleDeletePost = async () => {
        if (!window.confirm('정말 이 게시글을 삭제하시겠습니까?')) return;
        try {
            await deleteCommunityPost(id);
            alert('삭제되었습니다.');
            navigate('/community');
        } catch (error) {
            alert('삭제에 실패했습니다.');
        }
    };

    const handleAddComment = async (event) => {
        event.preventDefault();
        if (!currentUser) {
            alert('로그인 후 댓글을 작성할 수 있습니다.');
            return;
        }
        if (!newComment.trim()) return;

        try {
            await createComment(id, { content: newComment });
            setNewComment('');
            await reloadComments();
        } catch (error) {
            alert('댓글 작성에 실패했습니다.');
        }
    };

    const startEditComment = (comment) => {
        setEditingCommentId(comment.id);
        setEditingCommentContent(comment.content);
    };

    const cancelEditComment = () => {
        setEditingCommentId(null);
        setEditingCommentContent('');
    };

    const handleUpdateComment = async (commentId) => {
        if (!editingCommentContent.trim()) return;
        try {
            await updateComment(commentId, { content: editingCommentContent });
            await reloadComments();
            cancelEditComment();
        } catch (error) {
            alert('댓글 수정에 실패했습니다.');
        }
    };

    const handleDeleteComment = async (commentId) => {
        if (!window.confirm('이 댓글을 삭제하시겠습니까?')) return;
        try {
            await deleteComment(commentId);
            setComments((prev) => prev.filter((comment) => comment.id !== commentId));
            if (editingCommentId === commentId) cancelEditComment();
        } catch (error) {
            alert('댓글 삭제에 실패했습니다.');
        }
    };

    const openReportModal = (type, targetId) => {
        if (!currentUser) {
            alert('로그인 후 이용할 수 있습니다.');
            return;
        }
        setReportTarget({ type, id: targetId });
        setIsReportModalOpen(true);
    };

    const handleReportSubmit = async () => {
        if (!reportReason.trim()) {
            alert('신고 사유를 입력해주세요.');
            return;
        }

        try {
            await axiosInstance.post('/api/community/reports', {
                targetType: reportTarget.type,
                targetId: reportTarget.id,
                reason: reportReason,
            });
            alert('신고가 정상적으로 접수되었습니다.');
            setIsReportModalOpen(false);
            setReportReason('');
        } catch (error) {
            alert('신고 접수에 실패했습니다.');
        }
    };

    if (loading) return <div className="min-h-screen flex items-center justify-center">로딩 중...</div>;
    if (!post) return <div className="min-h-screen flex items-center justify-center">게시글이 없습니다.</div>;

    return (
        <div className="min-h-screen bg-gray-50 py-10">
            <div className="max-w-4xl mx-auto px-4">
                <div className="bg-white rounded-3xl shadow-sm border border-gray-200 p-8 mb-6">
                    <div className="flex justify-between items-start mb-6">
                        <div>
                            <span className="px-3 py-1 bg-blue-50 text-blue-600 rounded-lg text-sm font-bold mb-3 inline-block">
                                {post.category || '일반'}
                            </span>
                            <h1 className="text-3xl font-black text-gray-900">{post.title}</h1>
                        </div>
                    </div>

                    <div className="flex items-center justify-between text-sm text-gray-500 border-b pb-6 mb-6">
                        <div className="flex items-center space-x-4">
                            <span className="flex items-center"><User className="w-4 h-4 mr-1" /> {post.author}</span>
                            <span className="flex items-center"><Clock className="w-4 h-4 mr-1" /> {post.date}</span>
                            <span className="flex items-center"><Eye className="w-4 h-4 mr-1" /> {post.views}</span>
                        </div>

                        <div className="flex space-x-3">
                            {canManagePost && (
                                <>
                                    <button onClick={() => navigate(`/community/edit/${id}`)} className="text-blue-500 font-bold hover:underline">
                                        수정
                                    </button>
                                    <button onClick={handleDeletePost} className="text-red-500 font-bold hover:underline">
                                        {isAdmin ? '관리자 삭제' : '삭제'}
                                    </button>
                                </>
                            )}

                            {canReportPost && (
                                <button onClick={() => openReportModal('POST', post.id)} className="flex items-center text-orange-500 font-bold hover:underline">
                                    <AlertTriangle className="w-4 h-4 mr-1" /> 신고
                                </button>
                            )}
                        </div>
                    </div>

                    <div className="min-h-[300px] text-gray-800 leading-relaxed whitespace-pre-wrap">
                        {post.content}
                    </div>
                </div>

                <div className="bg-white rounded-3xl shadow-sm border border-gray-200 p-8">
                    <h3 className="text-lg font-black flex items-center mb-6">
                        <MessageSquare className="w-5 h-5 mr-2 text-blue-500" />
                        댓글 <span className="text-blue-600 ml-2">{comments.length}</span>
                    </h3>

                    <form onSubmit={handleAddComment} className="flex gap-4 mb-8">
                        <input
                            type="text"
                            value={newComment}
                            onChange={(event) => setNewComment(event.target.value)}
                            placeholder={currentUser ? '댓글을 남겨보세요.' : '로그인 후 댓글을 작성할 수 있습니다.'}
                            disabled={!currentUser}
                            className="flex-1 bg-gray-50 border border-gray-200 rounded-xl px-4 py-3 focus:outline-none focus:ring-2 focus:ring-blue-500"
                        />
                        <button type="submit" disabled={!currentUser} className="bg-blue-600 text-white font-bold px-6 py-3 rounded-xl hover:bg-blue-700 disabled:bg-gray-300">
                            등록
                        </button>
                    </form>

                    <div className="space-y-6">
                        {comments.map((comment) => (
                            <div key={comment.id} className="border-b border-gray-100 pb-6 last:border-0 last:pb-0">
                                <div className="flex justify-between items-start mb-2">
                                    <div className="flex items-center space-x-3">
                                        <span className="font-bold text-gray-900">{comment.author}</span>
                                        <span className="text-xs text-gray-400">{comment.date}</span>
                                    </div>
                                    <div className="flex space-x-3 text-xs">
                                        {canManageComment(comment) && (
                                            <>
                                                <button onClick={() => startEditComment(comment)} className="text-blue-400 hover:text-blue-600 font-bold">
                                                    수정
                                                </button>
                                                <button onClick={() => handleDeleteComment(comment.id)} className="text-red-400 hover:text-red-600 font-bold">
                                                    삭제
                                                </button>
                                            </>
                                        )}
                                        {canReportComment(comment) && (
                                            <button onClick={() => openReportModal('COMMENT', comment.id)} className="text-orange-400 hover:text-orange-600 font-bold">
                                                신고
                                            </button>
                                        )}
                                    </div>
                                </div>

                                {editingCommentId === comment.id ? (
                                    <div className="flex gap-2">
                                        <input
                                            type="text"
                                            value={editingCommentContent}
                                            onChange={(event) => setEditingCommentContent(event.target.value)}
                                            className="flex-1 bg-gray-50 border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                                        />
                                        <button type="button" onClick={() => handleUpdateComment(comment.id)} className="px-3 py-2 bg-blue-600 text-white rounded-lg text-sm font-bold">
                                            저장
                                        </button>
                                        <button type="button" onClick={cancelEditComment} className="px-3 py-2 bg-gray-100 text-gray-600 rounded-lg text-sm font-bold">
                                            취소
                                        </button>
                                    </div>
                                ) : (
                                    <p className="text-gray-700 text-sm">{comment.content}</p>
                                )}
                            </div>
                        ))}
                    </div>
                </div>
            </div>

            {isReportModalOpen && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-2xl p-8 max-w-md w-full shadow-2xl">
                        <div className="flex items-center text-orange-500 mb-4">
                            <AlertTriangle className="w-8 h-8 mr-2" />
                            <h2 className="text-2xl font-black text-gray-900">신고하기</h2>
                        </div>
                        <p className="text-sm text-gray-500 mb-6">부적절한 내용을 관리자에게 신고합니다.</p>

                        <textarea
                            value={reportReason}
                            onChange={(event) => setReportReason(event.target.value)}
                            placeholder="신고 사유를 상세히 적어주세요."
                            className="w-full h-32 border border-gray-200 rounded-xl p-4 text-sm mb-6 resize-none focus:outline-none focus:ring-2 focus:ring-orange-500"
                        />

                        <div className="flex space-x-3">
                            <button onClick={() => setIsReportModalOpen(false)} className="flex-1 py-3 bg-gray-100 text-gray-600 font-bold rounded-xl hover:bg-gray-200">
                                취소
                            </button>
                            <button onClick={handleReportSubmit} className="flex-1 py-3 bg-orange-500 text-white font-bold rounded-xl hover:bg-orange-600">
                                신고 접수하기
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default PostDetail;
