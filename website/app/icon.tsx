import { ImageResponse } from 'next/og';
export const size={width:64,height:64};
export const contentType='image/png';
export default function Icon(){return new ImageResponse(<div style={{width:'100%',height:'100%',display:'flex',alignItems:'center',justifyContent:'center',borderRadius:16,background:'#252724',color:'#f6f5f1',fontSize:49,fontWeight:800,letterSpacing:-5,paddingRight:6}}>s<span style={{color:'#e07860'}}>:</span></div>,size);}
