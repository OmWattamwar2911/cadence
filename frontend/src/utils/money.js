/**
 * Format BIGINT paise (minor units) to human-readable INR string.
 * @param {number|string} paise 
 * @returns {string} e.g. "₹1,250.50"
 */
export function formatPaise(paise) {
  if (paise === null || paise === undefined || isNaN(paise)) {
    return '₹0.00';
  }
  const numeric = typeof paise === 'string' ? parseInt(paise, 10) : paise;
  const rupees = numeric / 100;
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(rupees);
}

/**
 * Convert rupee decimal input (e.g. "12.50") to integer paise (1250).
 * Prevents IEEE-754 precision issues using round.
 * @param {string|number} rupees 
 * @returns {number} integer paise
 */
export function rupeesToPaise(rupees) {
  if (!rupees || isNaN(rupees)) return 0;
  const parsed = parseFloat(rupees);
  if (parsed <= 0) return 0;
  return Math.round(parsed * 100);
}
