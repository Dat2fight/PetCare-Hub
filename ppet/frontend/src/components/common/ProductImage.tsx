import React, { useState } from 'react';
import { FiImage } from 'react-icons/fi';

interface ProductImageProps {
  src?: string;
  alt: string;
  className?: string;
}

export const ProductImage: React.FC<ProductImageProps> = ({ src, alt, className }) => {
  const [failedSrc, setFailedSrc] = useState<string>();

  if (!src?.trim() || failedSrc === src) {
    return (
      <div role="img" aria-label={`${alt}: chưa có ảnh`} className={`flex flex-col items-center justify-center bg-gray-50 text-gray-400 ${className || ''}`}>
        <FiImage size={40} />
        <span className="mt-2 text-xs">Chưa có ảnh sản phẩm</span>
      </div>
    );
  }

  return <img src={src} alt={alt} className={className} onError={() => setFailedSrc(src)} />;
};
