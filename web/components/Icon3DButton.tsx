'use client';

import { ReactNode } from 'react';
import { motion } from 'framer-motion';

interface Icon3DButtonProps {
  children: ReactNode;
  className?: string;
  isActive?: boolean;
  onClick?: () => void;
  title?: string;
}

export default function Icon3DButton({ 
  children, 
  className = '',
  isActive = false,
  onClick,
  title
}: Icon3DButtonProps) {
  return (
    <motion.button
      onClick={onClick}
      title={title}
      className={className}
      style={{
        perspective: '1000px',
        transformStyle: 'preserve-3d',
      }}
      whileHover={{ 
        scale: 1.15,
        rotateY: 20,
        rotateX: 10,
        z: 30,
      }}
      whileTap={{ 
        scale: 0.95,
        rotateY: 0,
        rotateX: 0,
      }}
      animate={isActive ? {
        rotateY: [0, 5, -5, 5, 0],
      } : {}}
      transition={{ 
        type: 'tween',
        duration: 0.5,
        repeat: isActive ? Infinity : 0,
        repeatDelay: 2,
      }}
    >
      <div
        style={{
          transform: 'translateZ(25px)',
          transformStyle: 'preserve-3d',
          filter: isActive 
            ? 'drop-shadow(0 15px 30px rgba(59, 130, 246, 0.5)) brightness(1.2)'
            : 'drop-shadow(0 8px 16px rgba(0,0,0,0.2))',
          transition: 'filter 0.3s ease',
        }}
      >
        {children}
      </div>
    </motion.button>
  );
}

