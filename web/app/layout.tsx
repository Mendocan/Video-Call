import type { Metadata, Viewport } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import AuthProviderWrapper from "@/components/AuthProviderWrapper";
import { LanguageProvider } from "@/contexts/LanguageContext";
import AuthenticatedLayout from "@/components/AuthenticatedLayout";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Video Call - Desktop Application",
  description: "Güvenli ve özel görüntülü görüşme desktop uygulaması.",
  keywords: ["video görüşme", "güvenli görüşme", "desktop video call", "gizlilik odaklı görüşme"],
  authors: [{ name: "Video Call" }],
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  maximumScale: 5,
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="tr" className="h-full">
      <body className={`${geistSans.variable} ${geistMono.variable} antialiased h-full bg-gray-900`}>
        <LanguageProvider>
          <AuthProviderWrapper>
            <AuthenticatedLayout>
              {children}
            </AuthenticatedLayout>
          </AuthProviderWrapper>
        </LanguageProvider>
      </body>
    </html>
  );
}
