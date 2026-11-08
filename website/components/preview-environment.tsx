'use client';
import { useEffect, useState } from 'react';
import { GlobeHemisphereWest, MapPin } from '@phosphor-icons/react';
import { useLocalClock } from './local-clock';
export default function PreviewEnvironment() {
  const clock = useLocalClock();
  const [zones, setZones] = useState<string[]>([]);
  useEffect(() => {
    const supported = typeof Intl.supportedValuesOf === 'function' ? Intl.supportedValuesOf('timeZone') : ['Europe/London', 'America/New_York', 'Asia/Kolkata', 'Asia/Tokyo', 'Australia/Sydney'];
    setZones([...new Set([clock.timeZone, 'UTC', ...supported])]);
  }, [clock.timeZone]);
  return <div className="ed-environment">
    <div className="ed-environment-controls">
      <label className="ed-timezone"><GlobeHemisphereWest size={18} aria-hidden="true"/><span className="sr-only">Preview timezone</span><select value={clock.timeZone} onChange={e => clock.setTimeZone(e.target.value)}>{(zones.length ? zones : [clock.timeZone]).map(zone => <option key={zone} value={zone}>{zone.replaceAll('_', ' ')}</option>)}</select></label>
      <button className="ed-location" onClick={clock.requestLocation} disabled={clock.locationStatus === 'loading'}><MapPin size={17} aria-hidden="true"/>{clock.locationStatus === 'loading' ? 'Loading local weather…' : clock.usingLocation ? 'Refresh local weather' : 'Enable local weather'}</button>
    </div>
    <p className="ed-location-privacy">Time follows your device. Optional weather sends approximate coordinates to Open-Meteo.</p>
    {clock.locationError && <p className="ed-location-status" role="status">{clock.locationError}</p>}
    {clock.weather && <p className="ed-location-status" role="status">Weather loaded for {clock.city.replace(/ · timezone region$/, '')}: {Math.round(clock.weather.temperature)}°C.</p>}
  </div>;
}
