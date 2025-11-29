'use client';

import { ReactNode } from 'react';
import { motion } from 'framer-motion';

interface Icon3DProps {
  children: ReactNode;
  className?: string;
  size?: number;
  color?: string;
}

export default function Icon3D({ 
  children, 
  className = '',
  size = 24,
  color = 'currentColor'
}: Icon3DProps) {
  return (
    <motion.div
      className={className}
      style={{
        perspective: '1000px',
        transformStyle: 'preserve-3d',
      }}
      whileHover={{ 
        scale: 1.1,
        rotateY: 15,
        rotateX: 5,
      }}
      transition={{ type: 'spring', stiffness: 300, damping: 20 }}
    >
      <div
        style={{
          transform: 'translateZ(20px)',
          transformStyle: 'preserve-3d',
          filter: 'drop-shadow(0 10px 20px rgba(0,0,0,0.3))',
          color: color,
        }}
      >
        {children}
      </div>
    </motion.div>
  );
}
