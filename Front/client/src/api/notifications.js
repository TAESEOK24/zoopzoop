import axiosInstance from './index';

export const fetchRecentNotifications = async (size = 5) => {
    const response = await axiosInstance.get('/api/notifications/recent', {
        params: { size },
    });
    return response.data;
};

export const fetchUnreadNotificationCount = async () => {
    const response = await axiosInstance.get('/api/notifications/unread-count');
    return response.data;
};

export const markNotificationRead = async (notificationId) => {
    const response = await axiosInstance.patch(`/api/notifications/${notificationId}/read`);
    return response.data;
};

export const markAllNotificationsRead = async () => {
    const response = await axiosInstance.patch('/api/notifications/read-all');
    return response.data;
};

export const fetchNotificationSettings = async () => {
    const response = await axiosInstance.get('/api/notifications/settings');
    return response.data;
};

export const updateNotificationSettings = async (settings) => {
    const response = await axiosInstance.put('/api/notifications/settings', settings);
    return response.data;
};
