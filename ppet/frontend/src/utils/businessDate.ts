export const businessTimeZone = import.meta.env.VITE_BUSINESS_TIMEZONE || 'Asia/Ho_Chi_Minh';

const businessDateFormatter = new Intl.DateTimeFormat('en', {
  timeZone: businessTimeZone, year: 'numeric', month: '2-digit', day: '2-digit',
});

export const getBusinessDate = (date = new Date()): string => {
  const parts = businessDateFormatter.formatToParts(date);
  const part = (type: string) => parts.find(value => value.type === type)!.value;
  return `${part('year')}-${part('month')}-${part('day')}`;
};

// API LocalDate/LocalDateTime fields already represent wall time in the business zone.
export function formatBusinessTime(value: string, withTime = false): string {
  const hasOffset = /(?:Z|[+-]\d{2}:?\d{2})$/i.test(value);
  const date = new Date(hasOffset ? value : value.length === 10 ? value + 'T00:00:00Z' : value + 'Z');
  if (Number.isNaN(date.getTime())) return '—';
  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: hasOffset ? businessTimeZone : 'UTC',
    dateStyle: 'short',
    ...(withTime && { timeStyle: 'short' }),
  }).format(date);
}
