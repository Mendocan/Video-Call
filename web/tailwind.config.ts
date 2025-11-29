import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        // Video Call App Renkleri
        navy: {
          DEFAULT: '#0B1120',
          light: '#1C2538',
        },
        teal: {
          DEFAULT: '#00B8D4',
          light: '#7CFFCB',
          dark: '#0097A7',
        },
        slate: {
          DEFAULT: '#1C2538',
        },
        accent: {
          DEFAULT: '#7CFFCB',
        },
        danger: {
          DEFAULT: '#FF6B6B',
        },
        background: {
          light: '#F4F7FB',
          DEFAULT: '#FFFFFF',
        },
      },
    },
  },
  plugins: [],
};
export default config;

