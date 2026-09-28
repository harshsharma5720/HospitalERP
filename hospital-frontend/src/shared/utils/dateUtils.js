// "yyyy-MM-dd" in the user's local time zone.
// (toISOString() uses UTC, which gives yesterday's date in India before 05:30.)
export const toLocalISODate = (date = new Date()) => {
  const d = new Date(date);
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${month}-${day}`;
};

export const addDays = (date, days) => {
  const d = new Date(date);
  d.setDate(d.getDate() + days);
  return d;
};
