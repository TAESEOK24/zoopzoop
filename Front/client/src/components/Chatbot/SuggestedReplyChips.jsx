import React from 'react';

const SuggestedReplyChips = ({ chips, onChipClick, size = 'default' }) => {
    if (!chips || chips.length === 0) return null;

    return (
        <div className={`flex flex-wrap gap-2 mt-3`}>
            {chips.map((chip, index) => (
                <button
                    key={index}
                    onClick={() => onChipClick(chip.value)}
                    className={`
                        transition-all duration-200 border rounded-full text-blue-600 bg-white hover:bg-blue-50
                        ${size === 'large' 
                            ? 'px-4 py-2.5 text-[15px] border-blue-400 font-medium shadow-sm w-full md:w-auto text-center justify-center' 
                            : 'px-3 py-1.5 text-[13px] border-blue-200'}
                    `}
                >
                    {chip.label}
                </button>
            ))}
        </div>
    );
};

export default SuggestedReplyChips;
