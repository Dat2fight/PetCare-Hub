import React from 'react';

interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  children: React.ReactNode;
  noPadding?: boolean;
}

export const Card: React.FC<CardProps> = ({ children, className = '', noPadding = false, ...props }) => {
  const paddingClass = noPadding ? '' : 'p-6';

  return (
    <div
      className={`bg-white rounded-2xl shadow-card border border-gray-100 overflow-hidden ${paddingClass} ${className}`}
      {...props}
    >
      {children}
    </div>
  );
};