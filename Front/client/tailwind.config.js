/** @type {import('tailwindcss').Config} */
module.exports = {
    content: [
        "./src/**/*.{js,jsx,ts,tsx}",
    ],
    theme: {
        extend: {
            colors: {
                primary: "#2563eb", // 혜택 줍줍 포인트 블루
                accent: "#facc15",  // 줍줍 강조 옐로우
            },
        },
    },
    plugins: [],
}