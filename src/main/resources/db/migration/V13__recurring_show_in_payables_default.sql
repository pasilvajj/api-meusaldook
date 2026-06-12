-- Despesas fixas passam a aparecer em Contas a pagar por defeito (opt-out via formulário).
UPDATE finance_recurring_transaction
SET show_in_payables = true
WHERE kind = 'EXPENSE'
  AND show_in_payables = false;
