import { ImageResponse } from 'next/og';

export function generateImageMetadata() {
  return [
    { id: 'tab', size: { width: 64, height: 64 }, contentType: 'image/png' },
    { id: 'small', size: { width: 192, height: 192 }, contentType: 'image/png' },
    { id: 'large', size: { width: 512, height: 512 }, contentType: 'image/png' },
  ];
}

export default async function Icon({ id }: { id: Promise<string | number> }) {
  const which = String(await id);
  const px = which === 'large' ? 512 : which === 'small' ? 192 : 64;
  return new ImageResponse(<div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: px / 4, background: '#252724', color: '#f6f5f1', fontSize: px * 0.77, fontWeight: 800, letterSpacing: -px * 0.08, paddingRight: px * 0.09 }}>s<span style={{ color: '#e07860' }}>:</span></div>, { width: px, height: px });
}
