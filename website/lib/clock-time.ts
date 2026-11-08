export function validTimeZone(zone: string): boolean { try { new Intl.DateTimeFormat("en", { timeZone: zone }).format(); return true; } catch { return false; } }
/** IANA region names are representative timezone labels, not a geolocated city. */
export function timeZoneRegion(zone: string): string { return `${zone.split("/").at(-1)?.replaceAll("_", " ") || "UTC"} · timezone region`; }
export function getClockParts(now: Date, locale: string, timeZone: string) {
 const parts = new Intl.DateTimeFormat(locale,{timeZone,hourCycle:"h23",hour:"2-digit",minute:"2-digit",second:"2-digit",weekday:"long",day:"numeric",month:"long",year:"numeric"}).formatToParts(now);
 const get=(type:Intl.DateTimeFormatPartTypes)=>parts.find(part=>part.type===type)?.value??"";
 const numeric=new Intl.DateTimeFormat("en-US-u-nu-latn",{timeZone,hourCycle:"h23",hour:"2-digit",minute:"2-digit",second:"2-digit"}).formatToParts(now);
 const number=(type:Intl.DateTimeFormatPartTypes)=>Number(numeric.find(part=>part.type===type)?.value??0);
 return {hour:get("hour"),minute:get("minute"),second:get("second"),weekday:get("weekday"),day:get("day"),month:get("month"),year:get("year"),hour24:number("hour"),hour12:number("hour")%12||12,minuteNumber:number("minute"),secondNumber:number("second")};
}
