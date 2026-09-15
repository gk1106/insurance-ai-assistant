const BASE_PROPS = {
  width: 18,
  height: 18,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.8,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': true,
}

export function DashboardIcon(props) {
  return (
    <svg {...BASE_PROPS} {...props}>
      <rect x="3" y="3" width="7" height="9" rx="1.5" />
      <rect x="14" y="3" width="7" height="5" rx="1.5" />
      <rect x="14" y="12" width="7" height="9" rx="1.5" />
      <rect x="3" y="16" width="7" height="5" rx="1.5" />
    </svg>
  )
}

export function PolicyIcon(props) {
  return (
    <svg {...BASE_PROPS} {...props}>
      <path d="M7 3h8l4 4v13a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1Z" />
      <path d="M15 3v4h4" />
      <path d="M8.5 12h7M8.5 15.5h7M8.5 8.5h3" />
    </svg>
  )
}

export function ClaimIcon(props) {
  return (
    <svg {...BASE_PROPS} {...props}>
      <path d="M4 12 8.5 7.5 12.5 11l6-6" />
      <path d="M14.5 5H19v4.5" />
      <path d="M4 17.5h16" />
    </svg>
  )
}

export function RenewalIcon(props) {
  return (
    <svg {...BASE_PROPS} {...props}>
      <path d="M4 12a8 8 0 0 1 13.66-5.66L20 8.5" />
      <path d="M20 4v4.5h-4.5" />
      <path d="M20 12a8 8 0 0 1-13.66 5.66L4 15.5" />
      <path d="M4 20v-4.5h4.5" />
    </svg>
  )
}

export function AssistantIcon(props) {
  return (
    <svg {...BASE_PROPS} {...props}>
      <rect x="4" y="5" width="16" height="12" rx="3" />
      <path d="M9 21h6l-1.5-4h-3L9 21Z" />
      <circle cx="9" cy="11" r="1.1" fill="currentColor" stroke="none" />
      <circle cx="15" cy="11" r="1.1" fill="currentColor" stroke="none" />
    </svg>
  )
}

export function MenuIcon(props) {
  return (
    <svg {...BASE_PROPS} {...props}>
      <path d="M4 6h16M4 12h16M4 18h16" />
    </svg>
  )
}

export function LogoutIcon(props) {
  return (
    <svg {...BASE_PROPS} {...props}>
      <path d="M9 4H6a1 1 0 0 0-1 1v14a1 1 0 0 0 1 1h3" />
      <path d="M15 16l4-4-4-4" />
      <path d="M19 12H9" />
    </svg>
  )
}

export function AlertIcon(props) {
  return (
    <svg {...BASE_PROPS} {...props}>
      <path d="M12 3 2 20h20L12 3Z" />
      <path d="M12 10v4" />
      <circle cx="12" cy="17" r="0.6" fill="currentColor" stroke="none" />
    </svg>
  )
}

export function UsersIcon(props) {
  return (
    <svg {...BASE_PROPS} {...props}>
      <circle cx="9" cy="8" r="3" />
      <path d="M3 20c0-3.3 2.7-5.5 6-5.5s6 2.2 6 5.5" />
      <path d="M16 4.5c1.7.4 3 2 3 3.8 0 1.7-1.1 3.1-2.6 3.6" />
      <path d="M17.5 14.7c2 .6 3.5 2.4 3.5 4.6" />
    </svg>
  )
}

export function SunIcon(props) {
  return (
    <svg {...BASE_PROPS} {...props}>
      <circle cx="12" cy="12" r="4.2" />
      <path d="M12 2.5v2.3M12 19.2v2.3M4.6 4.6l1.6 1.6M17.8 17.8l1.6 1.6M2.5 12h2.3M19.2 12h2.3M4.6 19.4l1.6-1.6M17.8 6.2l1.6-1.6" />
    </svg>
  )
}

export function MoonIcon(props) {
  return (
    <svg {...BASE_PROPS} {...props}>
      <path d="M20 14.5A8.5 8.5 0 1 1 9.5 4a6.8 6.8 0 0 0 10.5 10.5Z" />
    </svg>
  )
}

export function DocumentIcon(props) {
  return (
    <svg {...BASE_PROPS} {...props}>
      <path d="M6 3.5h8l4 4V20a.75.75 0 0 1-.75.75H6a.75.75 0 0 1-.75-.75V4.25A.75.75 0 0 1 6 3.5Z" />
      <path d="M14 3.5V8h4" />
      <path d="M8 12.5h6M8 16h4" />
    </svg>
  )
}

export function InboxIcon(props) {
  return (
    <svg {...BASE_PROPS} {...props}>
      <path d="M4 12h4l2 3h4l2-3h4" />
      <path d="M5.5 5h13l2 7v6a1 1 0 0 1-1 1h-15a1 1 0 0 1-1-1v-6l2-7Z" />
    </svg>
  )
}
