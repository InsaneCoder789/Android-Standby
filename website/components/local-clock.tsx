"use client";
import {createContext,useCallback,useContext,useEffect,useMemo,useRef,useState,type ReactNode} from "react";
import {timeZoneRegion,validTimeZone} from "@/lib/clock-time";
export type LocalWeather={temperature:number;high:number;low:number;code:number};
export type LocationStatus="idle"|"loading"|"denied"|"unavailable"|"live";
export type LocalClock={now:Date|null;locale:string;timeZone:string;city:string;weather:LocalWeather|null;locationStatus:LocationStatus;locationError:string|null;usingLocation:boolean;selectedTimeZone:string|null;timeZoneSource:"browser"|"selected"|"location";requestLocation:()=>void;setTimeZone:(zone:string)=>void};
function localDateKey(at:number,timeZone:string){
 const parts=new Intl.DateTimeFormat("en-US-u-nu-latn",{timeZone,year:"numeric",month:"2-digit",day:"2-digit"}).formatToParts(new Date(at));
 const part=(type:string)=>parts.find(p=>p.type===type)?.value??"";return `${part("year")}-${part("month")}-${part("day")}`;
}
const ClockContext=createContext<LocalClock|null>(null);
export function LocalClockProvider({children}:{children:ReactNode}){
 const [now,setNow]=useState<Date|null>(null),[locale,setLocale]=useState("en"),[zone,setZone]=useState("UTC");
 const [selectedTimeZone,setSelected]=useState<string|null>(null),[timeZoneSource,setSource]=useState<LocalClock["timeZoneSource"]>("browser");
 const [weather,setWeather]=useState<LocalWeather|null>(null),[locationStatus,setStatus]=useState<LocationStatus>("idle"),[locationError,setError]=useState<string|null>(null),[usingLocation,setUsingLocation]=useState(false);
 const generation=useRef(0),pending=useRef<AbortController|null>(null);
 const freshness=useRef<{expiresAt:number;date:string;timeZone:string;request:number}|null>(null);
 useEffect(()=>{
  const settings=Intl.DateTimeFormat().resolvedOptions();setLocale(settings.locale||navigator.language||"en");setZone(validTimeZone(settings.timeZone)?settings.timeZone:"UTC");
  let timer:ReturnType<typeof setTimeout>;
  const tick=()=>{
   const at=Date.now();setNow(new Date(at));const saved=freshness.current;
   if(saved && (at>=saved.expiresAt || localDateKey(at,saved.timeZone)!==saved.date)){
    freshness.current=null;setWeather(null);
    if(saved.request===generation.current){setStatus("unavailable");setError("Weather needs a refresh. Select Refresh location to load the current temperature and today’s high and low.");}
   }
   timer=setTimeout(tick,1000-Date.now()%1000);
  };
  const resume=()=>{clearTimeout(timer);if(!document.hidden)tick();};
  tick();document.addEventListener("visibilitychange",resume);
  return()=>{clearTimeout(timer);document.removeEventListener("visibilitychange",resume);generation.current++;pending.current?.abort();};
 },[]);
 const setTimeZone=useCallback((value:string)=>{
  if(!validTimeZone(value)){setError("Choose a valid timezone.");return;}
  generation.current++;pending.current?.abort();pending.current=null;freshness.current=null;setZone(value);setSelected(value);setSource("selected");setUsingLocation(false);setWeather(null);setStatus("idle");setError(null);
 },[]);
 const requestLocation=useCallback(()=>{
  const request=++generation.current;pending.current?.abort();setError(null);
  if(!navigator.geolocation){setStatus("unavailable");setError("This browser does not provide location. You can choose a timezone instead.");return;}
  setStatus("loading");
  navigator.geolocation.getCurrentPosition(async position=>{
   if(request!==generation.current)return;
   const requestedAt=Date.now();const controller=new AbortController();pending.current=controller;const timeout=setTimeout(()=>controller.abort(),10000);
   try{
    const query=new URLSearchParams({latitude:position.coords.latitude.toFixed(2),longitude:position.coords.longitude.toFixed(2)});
    const response=await fetch(`/api/weather?${query}`,{signal:controller.signal,cache:"no-store"});if(!response.ok)throw new Error("weather");const data=await response.json();
    if(typeof data.timeZone!=="string"||!validTimeZone(data.timeZone)||![data.temperature,data.high,data.low,data.code].every(value=>typeof value==="number"&&Number.isFinite(value)))throw new Error("weather");
    if(request!==generation.current)return;
    const receivedAt=Date.now();const day=localDateKey(receivedAt,data.timeZone);
    if(typeof data.observedAt!=="number" || !Number.isFinite(data.observedAt) || data.observedAt>receivedAt+300000 || receivedAt-data.observedAt>90*60000 || data.forecastDate!==day || localDateKey(requestedAt,data.timeZone)!==day)throw new Error("stale weather");
    freshness.current={expiresAt:Math.min(requestedAt+20*60000,data.observedAt+90*60000),date:day,timeZone:data.timeZone,request};
    setWeather({temperature:data.temperature,high:data.high,low:data.low,code:data.code});setZone(data.timeZone);setSelected(null);setSource("location");setUsingLocation(true);setStatus("live");setError(null);
   }catch{if(request!==generation.current)return;setStatus("unavailable");setError("Weather could not be loaded. Your clock keeps its current timezone. Try again or choose a timezone.");}
   finally{clearTimeout(timeout);if(pending.current===controller)pending.current=null;}
  },error=>{if(request!==generation.current)return;setStatus(error.code===1?"denied":"unavailable");setError(error.code===1?"Location access was denied. Your clock keeps its current timezone; you can choose another below.":"Location is unavailable. Try again or choose a timezone.");},{enableHighAccuracy:false,timeout:10000,maximumAge:300000});
 },[]);
 const value=useMemo<LocalClock>(()=>({now,locale,timeZone:zone,city:timeZoneRegion(zone),weather,locationStatus,locationError,usingLocation,selectedTimeZone,timeZoneSource,requestLocation,setTimeZone}),[now,locale,zone,weather,locationStatus,locationError,usingLocation,selectedTimeZone,timeZoneSource,requestLocation,setTimeZone]);
 return <ClockContext.Provider value={value}>{children}</ClockContext.Provider>;
}
export function useLocalClock():LocalClock{const clock=useContext(ClockContext);if(!clock)throw new Error("useLocalClock must be used within LocalClockProvider");return clock;}
