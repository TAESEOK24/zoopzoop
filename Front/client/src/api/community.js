import axiosInstance from './index';

// 1. 게시글 목록 조회
export const fetchCommunityPosts = (keyword = '', page = 0, size = 15, category = '전체글보기') => {
    return axiosInstance.get('/api/community/posts', {
        params: { keyword, page, size, category }
    });
};

// 2. 새 게시글 작성
export const createCommunityPost = async (postData) => {
    return axiosInstance.post('/api/community/posts', postData);
};

// 3. 게시글 상세 조회
export const fetchCommunityPostDetail = (id) => {
    return axiosInstance.get(`/api/community/posts/${id}`);
};

// 4. 게시글 수정
export const updateCommunityPost = async (id, postData) => {
    return axiosInstance.put(`/api/community/posts/${id}`, postData);
};

// 5. 게시글 삭제
export const deleteCommunityPost = async (id) => {
    return axiosInstance.delete(`/api/community/posts/${id}`);
};

// 6. 댓글 목록 조회
export const fetchComments = (postId) => {
    return axiosInstance.get(`/api/community/posts/${postId}/comments`);
};

// 7. 댓글 작성
export const createComment = async (postId, commentData) => {
    return axiosInstance.post(`/api/community/posts/${postId}/comments`, commentData);
};

// 8. 댓글 수정
export const updateComment = async (commentId, data) => {
    return axiosInstance.put(`/api/community/comments/${commentId}`, data);
};

// 9. 댓글 삭제
export const deleteComment = async (commentId) => {
    return axiosInstance.delete(`/api/community/comments/${commentId}`);
};