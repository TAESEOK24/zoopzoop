import axios from './index';

// 1. 게시글 목록 조회 (검색 및 페이징)
export const fetchCommunityPosts = (search, page = 0, size = 15) => {
    return axios.get('/api/community/posts', {
        params: { search, page, size }
    });
};

// 2. 새 게시글 작성 (🚀 토큰 필요)
export const createCommunityPost = async (postData) => {
    return axios.post('/api/community/posts', postData);
};

// 3. 게시글 상세 조회
export const fetchCommunityPostDetail = (id) => axios.get(`/api/community/posts/${id}`);

// 4. 게시글 수정 (🚀 토큰 필요)
export const updateCommunityPost = async (id, postData) => {
    return axios.put(`/api/community/posts/${id}`, postData);
};

// 5. 게시글 삭제 (🚀 토큰 필요)
export const deleteCommunityPost = async (id) => {
    return axios.delete(`/api/community/posts/${id}`);
};

// ================= 🚀 댓글 API ================= //

// 6. 댓글 목록 조회
export const fetchComments = (postId) => axios.get(`/api/community/posts/${postId}/comments`);

// 7. 댓글 작성 (🚀 토큰 필요)
export const createComment = async (postId, commentData) => {
    return axios.post(`/api/community/posts/${postId}/comments`, commentData);
};

// 8. 댓글 삭제 (🚀 토큰 필요)
export const deleteComment = async (commentId) => {
    return axios.delete(`/api/community/comments/${commentId}`);
};

// 9. 댓글 수정 (🚀 토큰 필요)
export const updateComment = async (commentId, data) => {
    return axios.put(`/api/community/comments/${commentId}`, data);
};