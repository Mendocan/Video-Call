'use client';

import { motion } from 'framer-motion';
import { ReactNode } from 'react';

interface AnimatedIconProps {
  children: ReactNode;
  className?: string;
  hover?: boolean;
  scale?: number;
}

export default function AnimatedIcon({ 
  children, 
  className = '', 
  hover = true,
  scale = 1.1 
}: AnimatedIconProps) {
  return (
    <motion.div
      className={className}
      whileHover={hover ? { scale, rotate: 5 } : {}}
      whileTap={{ scale: 0.95 }}
      transition={{ type: 'spring', stiffness: 300, damping: 20 }}
    >
      {children}
    </motion.div>
  );
}

