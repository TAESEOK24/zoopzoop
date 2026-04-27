import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
    fetchCommunityPostDetail,
    deleteCommunityPost,
    fetchComments,
    createComment,
    deleteComment,
    updateComment // 🚀 댓글 수정 API 추가됨
} from '../../api/community';

const PostDetail = () => {
    const { id } = useParams();
    const navigate = useNavigate();

    const [post, setPost] = useState(null);
    const [loading, setLoading] = useState(true);

    // 댓글 관련 상태 관리
    const [comments, setComments] = useState([]);
    const [newComment, setNewComment] = useState('');

    // 🚀 댓글 수정 모드 관련 상태 관리
    const [editingCommentId, setEditingCommentId] = useState(null); // 수정 중인 댓글의 ID
    const [editCommentContent, setEditCommentContent] = useState(''); // 수정 중인 텍스트 내용

    // 로컬 스토리지에서 내 이름(명찰) 꺼내기
    const currentUserName = localStorage.getItem('userName');

    useEffect(() => {
        const loadData = async () => {
            try {
                const postResponse = await fetchCommunityPostDetail(id);
                setPost(postResponse.data.data);

                const commentResponse = await fetchComments(id);
                setComments(commentResponse.data.data);
            } catch (error) {
                alert("게시글을 불러올 수 없습니다.");
                navigate('/community');
            } finally {
                setLoading(false);
            }
        };
        loadData();
    }, [id, navigate]);

    // 게시글 삭제
    const handleDeletePost = async () => {
        if (window.confirm("정말로 이 게시글을 삭제하시겠습니까?")) {
            try {
                await deleteCommunityPost(id);
                alert("삭제되었습니다.");
                navigate('/community');
            } catch (error) {
                alert("게시글 삭제 중 오류가 발생했습니다.");
            }
        }
    };

    // 새 댓글 등록
    const handleAddComment = async (e) => {
        e.preventDefault();

        if (!newComment.trim()) return alert("댓글 내용을 입력해주세요.");

        const token = localStorage.getItem('accessToken');
        if (!token) {
            alert("로그인 후 댓글을 작성할 수 있습니다.");
            navigate('/login');
            return;
        }

        try {
            await createComment(id, { content: newComment });
            setNewComment(''); // 입력창 비우기

            // 등록 후 댓글 목록 새로고침
            const commentResponse = await fetchComments(id);
            setComments(commentResponse.data.data);
        } catch (error) {
            alert("댓글 등록에 실패했습니다.");
        }
    };

    // 댓글 삭제
    const handleDeleteComment = async (commentId) => {
        if (window.confirm("댓글을 삭제하시겠습니까?")) {
            try {
                await deleteComment(commentId);
                // 삭제 후 댓글 목록 새로고침
                const commentResponse = await fetchComments(id);
                setComments(commentResponse.data.data);
            } catch (error) {
                alert("댓글 삭제에 실패했습니다.");
            }
        }
    };

    // 🚀 수정 버튼을 눌렀을 때 작동
    const handleEditClick = (comment) => {
        setEditingCommentId(comment.id); // 현재 댓글을 '수정 모드'로 변경
        setEditCommentContent(comment.content); // 기존 내용을 입력창에 세팅
    };

    // 🚀 수정 취소
    const handleCancelEdit = () => {
        setEditingCommentId(null); // 수정 모드 해제
        setEditCommentContent('');
    };

    // 🚀 수정 완료(저장) 백엔드 전송
    const handleUpdateComment = async (commentId) => {
        if (!editCommentContent.trim()) return alert("수정할 내용을 입력해주세요.");

        try {
            await updateComment(commentId, { content: editCommentContent });
            alert("댓글이 수정되었습니다.");

            setEditingCommentId(null); // 수정 모드 해제

            // 수정 후 댓글 목록 새로고침
            const commentResponse = await fetchComments(id);
            setComments(commentResponse.data.data);
        } catch (error) {
            alert("댓글 수정에 실패했습니다.");
        }
    };

    if (loading) return <div className="min-h-screen flex justify-center items-center bg-gray-50 font-bold text-gray-500">불러오는 중...</div>;
    if (!post) return null;

    return (
        <div className="min-h-screen bg-gray-50 py-8">
            <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">

                {/* 상단 버튼 영역 */}
                <div className="flex justify-between items-center mb-6">
                    <button onClick={() => navigate('/community')} className="text-gray-500 hover:text-blue-600 font-bold flex items-center">
                        ← 목록으로 돌아가기
                    </button>

                    {/* 내 게시글일 때만 수정/삭제 버튼 노출 */}
                    {currentUserName === post.author && (
                        <div className="space-x-3">
                            <button onClick={() => navigate(`/community/edit/${id}`)} className="px-4 py-2 bg-gray-100 text-gray-700 rounded-md hover:bg-gray-200 font-bold text-sm">
                                수정
                            </button>
                            <button onClick={handleDeletePost} className="px-4 py-2 bg-red-100 text-red-600 rounded-md hover:bg-red-200 font-bold text-sm">
                                삭제
                            </button>
                        </div>
                    )}
                </div>

                {/* 게시글 본문 */}
                <div className="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden mb-6">
                    <div className="p-8 border-b border-gray-100 bg-gray-50/50">
                        <div className="mb-3">
                            <span className="px-3 py-1 bg-blue-100 text-blue-700 text-sm font-bold rounded-md">{post.category}</span>
                        </div>
                        <h1 className="text-3xl font-bold text-gray-900 mb-4">{post.title}</h1>
                        <div className="flex text-sm text-gray-500 space-x-4 font-medium">
                            <span className="text-gray-700">{post.author}</span>
                            <span>|</span><span>{post.date}</span>
                            <span>|</span><span>조회수 {post.views}</span>
                        </div>
                    </div>
                    <div className="p-8 min-h-[300px] text-gray-800 whitespace-pre-wrap leading-relaxed text-lg">
                        {post.content}
                    </div>
                </div>

                {/* 댓글 영역 */}
                <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-8">
                    <h3 className="text-lg font-bold text-gray-900 mb-4">
                        댓글 <span className="text-blue-600">{comments.length}</span>
                    </h3>

                    {/* 새 댓글 작성 폼 */}
                    <form onSubmit={handleAddComment} className="mb-8">
                        <div className="flex gap-4">
                            <textarea
                                rows="3"
                                value={newComment}
                                onChange={(e) => setNewComment(e.target.value)}
                                placeholder="댓글을 남겨보세요."
                                className="flex-1 border border-gray-300 rounded-lg p-4 focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
                            ></textarea>
                            <button type="submit" className="px-6 py-2 bg-blue-600 text-white font-bold rounded-lg hover:bg-blue-700 transition">
                                등록
                            </button>
                        </div>
                    </form>

                    {/* 댓글 목록 */}
                    <div className="space-y-6">
                        {comments.length === 0 ? (
                            <div className="text-center text-gray-500 py-4">아직 작성된 댓글이 없습니다.</div>
                        ) : (
                            comments.map((comment) => (
                                <div key={comment.id} className="border-b border-gray-100 pb-6 last:border-0">
                                    <div className="flex justify-between items-start mb-2">
                                        <div className="flex items-center space-x-3">
                                            <span className="font-bold text-gray-900">{comment.author}</span>
                                            <span className="text-sm text-gray-400">{comment.date}</span>
                                        </div>

                                        {/* 내 댓글일 때만 노출되는 버튼들 */}
                                        {currentUserName === comment.author && (
                                            <div className="space-x-3">
                                                <button
                                                    onClick={() => handleEditClick(comment)}
                                                    className="text-sm text-gray-400 hover:text-blue-600 font-medium"
                                                >
                                                    수정
                                                </button>
                                                <button
                                                    onClick={() => handleDeleteComment(comment.id)}
                                                    className="text-sm text-red-400 hover:text-red-600 font-medium"
                                                >
                                                    삭제
                                                </button>
                                            </div>
                                        )}
                                    </div>

                                    {/* 🚀 현재 이 댓글이 '수정 모드'인지 확인해서 화면을 다르게 보여줌 */}
                                    {editingCommentId === comment.id ? (
                                        <div className="mt-2 bg-gray-50 p-4 rounded-lg border border-gray-200">
                                            <textarea
                                                value={editCommentContent}
                                                onChange={(e) => setEditCommentContent(e.target.value)}
                                                className="w-full border border-gray-300 rounded-lg p-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none bg-white"
                                                rows="3"
                                            ></textarea>
                                            <div className="flex justify-end space-x-2 mt-3">
                                                <button onClick={handleCancelEdit} className="px-4 py-2 bg-white border border-gray-300 text-gray-700 rounded-md hover:bg-gray-50 text-sm font-bold transition shadow-sm">
                                                    취소
                                                </button>
                                                <button onClick={() => handleUpdateComment(comment.id)} className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 text-sm font-bold transition shadow-sm">
                                                    수정 완료
                                                </button>
                                            </div>
                                        </div>
                                    ) : (
                                        <p className="text-gray-700 whitespace-pre-wrap">{comment.content}</p>
                                    )}
                                </div>
                            ))
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default PostDetail;