import { ImageResponse } from 'next/og';
export const size = { width: 180, height: 180 };
export const contentType = 'image/png';
export default function AppleIcon() {
  return new ImageResponse(<div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#252724', color: '#f6f5f1', fontSize: 139, fontWeight: 800, letterSpacing: -14, paddingRight: 16 }}>s<span style={{ color: '#e07860' }}>:</span></div>, size);
}
