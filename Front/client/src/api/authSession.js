export const getAccessToken = () => {
    let token = sessionStorage.getItem('accessToken');
    if (!token) {
        token = localStorage.getItem('accessToken');
        if (token) {
            sessionStorage.setItem('accessToken', token);
            localStorage.removeItem('accessToken');
        }
    }
    return token;
};

export const setAuthSession = (authData) => {
    const accessToken = authData?.accessToken;
    const userName = authData?.user?.name || authData?.name || authData?.userName;

    if (accessToken) {
        sessionStorage.setItem('accessToken', accessToken);
        localStorage.removeItem('accessToken');
    }

    if (userName) {
        sessionStorage.setItem('userName', userName);
        localStorage.removeItem('userName');
    }

    window.dispatchEvent(new Event('loginStateChange'));
};

export const clearAuthSession = () => {
    sessionStorage.removeItem('accessToken');
    sessionStorage.removeItem('userName');
    localStorage.removeItem('accessToken');
    localStorage.removeItem('userName');
    window.dispatchEvent(new Event('loginStateChange'));
};

export const isAuthenticated = () => Boolean(getAccessToken());

export const getUserName = () => {
    let name = sessionStorage.getItem('userName');
    if (!name) {
        name = localStorage.getItem('userName');
        if (name) {
            sessionStorage.setItem('userName', name);
            localStorage.removeItem('userName');
        }
    }
    return name;
};
