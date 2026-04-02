import React from 'react';

const Footer = () => {
    return (
        <footer className="mt-20 py-8 bg-white border-t border-gray-200 text-center text-gray-500 text-sm">
            <div className="flex justify-center space-x-6 mb-4">
                <a href="#" className="hover:text-gray-900">이용약관</a>
                <a href="#" className="hover:text-gray-900 font-bold">개인정보처리방침</a>
                <a href="#" className="hover:text-gray-900">고객센터</a>
            </div>
            <p>© 2026 ZoopZoop. All rights reserved.</p>
        </footer>
    );
};

export default Footer;