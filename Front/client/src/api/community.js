import axios from './index';

// 기존 fetchCommunityPosts 함수를 아래와 같이 수정
export const fetchCommunityPosts = (search, page = 0, size = 15) => {
    return axios.get('/api/community/posts', {
        params: { search, page, size }
    });
};
// 새 게시글 작성 (이 함수만 수정!)
export const createCommunityPost = async (postData) => {
    // 1. 요청을 보내기 직전에 로컬 스토리지에서 토큰을 꺼냅니다.
    const token = localStorage.getItem('accessToken');

    // 2. 백엔드에 요청할 때, 세 번째 자리에 몰래 토큰(헤더)을 끼워 넣습니다.
    return axios.post('/api/community/posts', postData, {
        headers: {
            Authorization: `Bearer ${token}` // 🚀 딱 이 요청에만 신분증 제시!
        }
    });
};
export const fetchCommunityPostDetail = (id) => axios.get(`/api/community/posts/${id}`);

// 🚀 수정/삭제 API 함수 추가!
export const updateCommunityPost = (id, postData) => axios.put(`/api/community/posts/${id}`, postData);
export const deleteCommunityPost = (id) => axios.delete(`/api/community/posts/${id}`);
// ================= 🚀 댓글 API 추가 ================= //
export const fetchComments = (postId) => axios.get(`/api/community/posts/${postId}/comments`);
export const createComment = (postId, commentData) => axios.post(`/api/community/posts/${postId}/comments`, commentData);
export const deleteComment = (commentId) => axios.delete(`/api/community/comments/${commentId}`);