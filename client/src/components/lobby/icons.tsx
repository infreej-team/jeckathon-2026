type IconProps = { className?: string };

export function AntIcon({ className }: IconProps) {
  return (
    <svg viewBox="0 0 32 24" className={className} fill="currentColor" aria-hidden="true">
      <ellipse cx="7" cy="13" rx="5.5" ry="4.5" />
      <ellipse cx="16" cy="13" rx="3.5" ry="3" />
      <circle cx="24" cy="12" r="3.8" />
      <g stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" fill="none">
        <path d="M13 11 9 4M16 10V3M19 11l4-7" />
        <path d="M13 15 9 22M16 16v6M19 15l4 7" />
        <path d="M26 9l3-5M27 11l4-2" />
      </g>
    </svg>
  );
}

export function UserIcon({ className }: IconProps) {
  return (
    <svg viewBox="0 0 24 24" className={className} fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21c0-4 3.6-6 8-6s8 2 8 6" strokeLinecap="round" />
    </svg>
  );
}

export function ArrowIcon({ className }: IconProps) {
  return (
    <svg viewBox="0 0 24 24" className={className} fill="none" stroke="currentColor" strokeWidth="2.2" aria-hidden="true">
      <path d="M5 12h14M13 6l6 6-6 6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

export function TrendIcon({ className }: IconProps) {
  return (
    <svg viewBox="0 0 24 24" className={className} fill="none" stroke="currentColor" strokeWidth="2.2" aria-hidden="true">
      <path d="M3 17l6-6 4 4 8-8M15 7h6v6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

export function ReceiptIcon({ className }: IconProps) {
  return (
    <svg viewBox="0 0 24 24" className={className} fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <rect x="5" y="3" width="14" height="18" rx="2" />
      <path d="M9 8h6M9 12h6M9 16h4" strokeLinecap="round" />
    </svg>
  );
}

export function TrophyIcon({ className }: IconProps) {
  return (
    <svg viewBox="0 0 24 24" className={className} fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <path d="M8 4h8v6a4 4 0 0 1-8 0V4Z" strokeLinejoin="round" />
      <path d="M8 6H4v1a4 4 0 0 0 4 4M16 6h4v1a4 4 0 0 1-4 4M12 14v4M8 20h8" strokeLinecap="round" />
    </svg>
  );
}
