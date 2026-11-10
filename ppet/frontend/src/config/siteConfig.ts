const missing = 'Chưa cập nhật';
export const siteConfig = {
  hotline: import.meta.env.VITE_SITE_HOTLINE?.trim() || missing,
  address: import.meta.env.VITE_SITE_ADDRESS?.trim() || missing,
  email: import.meta.env.VITE_SITE_EMAIL?.trim() || missing,
  hours: import.meta.env.VITE_SITE_HOURS?.trim() || missing,
};
