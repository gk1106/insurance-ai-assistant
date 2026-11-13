import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  CURRENCY_CODE,
  MONEY_LOCALE,
  formatCurrency,
} from '../src/utils/currency.js'

test('all amounts use INR with Indian lakh and crore grouping', () => {
  assert.equal(CURRENCY_CODE, 'INR')
  assert.equal(MONEY_LOCALE, 'en-IN')
  assert.equal(formatCurrency(250000), '\u20b92,50,000.00')
  assert.equal(formatCurrency('10000000.25'), '\u20b91,00,00,000.25')
})
test('retains rupee amounts and paise without applying a conversion', () => {
  assert.equal(formatCurrency(0), '\u20b90.00')
  assert.equal(formatCurrency('1200.5'), '\u20b91,200.50')
  assert.equal(formatCurrency(3582), '\u20b93,582.00')
})
test('missing or invalid amounts do not appear as a zero balance', () => {
  for (const value of [
    null,
    undefined,
    '',
    ' ',
    NaN,
    Infinity,
    'invalid',
    {},
    true,
  ]) {
    assert.equal(formatCurrency(value), '\u2014')
  }
})
