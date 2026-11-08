import { validTimeZone } from "@/lib/clock-time";

const CODES = new Set([0, 1, 2, 3, 45, 48, 51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 71, 73, 75, 77, 80, 81, 82, 85, 86, 95, 96, 99]);
const finite = (value: unknown): value is number => typeof value === "number" && Number.isFinite(value);
export async function GET(request: Request) {
  const params = new URL(request.url).searchParams;
  const latitude = params.get("latitude"), longitude = params.get("longitude");
  const lat = Number(latitude), lon = Number(longitude);
  if (!latitude?.trim() || !longitude?.trim() || !Number.isFinite(lat) || !Number.isFinite(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180) {
    return Response.json({ error: "Valid latitude and longitude are required." }, { status: 400 });
  }
  // Cache grid-level forecast requests, never persist the precise browser coordinates.
  const query = new URLSearchParams({ latitude: lat.toFixed(2), longitude: lon.toFixed(2), current: "temperature_2m,weather_code", daily: "temperature_2m_max,temperature_2m_min", temperature_unit: "celsius", timezone: "auto", forecast_days: "1", timeformat: "unixtime" });
  try {
    const response = await fetch(`https://api.open-meteo.com/v1/forecast?${query}`, { next: { revalidate: 1200 }, signal: AbortSignal.timeout(8000) });
    if (!response.ok) throw new Error("upstream");
    const data = await response.json();
    const temperature = data.current?.temperature_2m, code = data.current?.weather_code;
    const high = data.daily?.temperature_2m_max?.[0], low = data.daily?.temperature_2m_min?.[0];
    const observedAt = finite(data.current?.time) ? data.current.time * 1000 : NaN;
    const forecastAt = finite(data.daily?.time?.[0]) ? data.daily.time[0] * 1000 : NaN;
    if (typeof data.timezone !== "string" || !validTimeZone(data.timezone) || ![temperature, high, low].every(finite) || !finite(code) || !CODES.has(code) || low > high || data.current_units?.temperature_2m !== "°C") throw new Error("payload");
    const now=Date.now();
    if(!Number.isFinite(observedAt) || !Number.isFinite(forecastAt) || observedAt>now+300000 || now-observedAt>90*60000)throw new Error("stale forecast");
    const dayKey=(at:number)=>{const parts=new Intl.DateTimeFormat("en-US-u-nu-latn",{timeZone:data.timezone,year:"numeric",month:"2-digit",day:"2-digit"}).formatToParts(new Date(at));const part=(type:string)=>parts.find(p=>p.type===type)?.value??"";return `${part("year")}-${part("month")}-${part("day")}`;};
    const forecastDate=dayKey(forecastAt);if(forecastDate!==dayKey(now))throw new Error("forecast day mismatch");
    return Response.json({ temperature, high, low, code, timeZone: data.timezone, unit: "celsius", observedAt, forecastDate }, { headers: { "Cache-Control": "private, no-store" } });
  } catch {
    return Response.json({ error: "Weather is temporarily unavailable." }, { status: 502, headers: { "Cache-Control": "no-store" } });
  }
}
