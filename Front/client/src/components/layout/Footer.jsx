import React from 'react';
import { Link } from 'react-router-dom';

const Footer = () => {
    return (
        <footer className="mt-20 py-8 bg-white border-t border-gray-200 text-center text-gray-500 text-sm">
            <div className="flex justify-center space-x-6 mb-4">
                <Link to="/terms" className="hover:text-gray-900">이용약관</Link>
                <Link to="/privacy" className="hover:text-gray-900 font-bold">개인정보처리방침</Link>
            </div>
            <p>© 2026 ZoopZoop. All rights reserved.</p>
        </footer>
    );
};

export default Footer;
