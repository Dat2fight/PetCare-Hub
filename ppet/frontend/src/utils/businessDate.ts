const businessDateFormatter = new Intl.DateTimeFormat('en', {
  timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: '2-digit', day: '2-digit',
});

export const getBusinessDate = (date = new Date()): string => {
  const parts = businessDateFormatter.formatToParts(date);
  const part = (type: string) => parts.find(value => value.type === type)!.value;
  return `${part('year')}-${part('month')}-${part('day')}`;
};
