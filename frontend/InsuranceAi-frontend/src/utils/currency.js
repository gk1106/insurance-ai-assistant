export const CURRENCY_CODE = 'INR'
export const MONEY_LOCALE = 'en-IN'

const rupees = new Intl.NumberFormat(MONEY_LOCALE, {
  style: 'currency',
  currency: CURRENCY_CODE,
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

// API amounts are rupees, not paise. Formatting never applies an exchange rate.
export function formatCurrency(amount) {
  if (amount == null || (typeof amount === 'string' && !amount.trim()))
    return '\u2014'
  if (typeof amount !== 'number' && typeof amount !== 'string') return '\u2014'
  const numeric = Number(amount)
  return Number.isFinite(numeric) ? rupees.format(numeric) : '\u2014'
}
