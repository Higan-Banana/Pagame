'use strict';
class DomainError extends Error {
  constructor(message, code = 'failed-precondition') { super(message); this.code = code; }
}
const requireThat = (ok, message, code) => { if (!ok) throw new DomainError(message, code); };
const text = (value, max = 100) => {
  requireThat(typeof value === 'string' && value.trim().length > 0 && value.trim().length <= max,
    'Revisa los campos del formulario.', 'invalid-argument');
  return value.trim();
};
function ids(value) {
  requireThat(Array.isArray(value) && value.length > 0 && value.length <= 30 &&
    value.every(x => typeof x === 'string' && x.length > 0 && !x.includes('/')),
    'Selecciona entre 1 y 30 participantes.', 'invalid-argument');
  return [...new Set(value)].sort();
}
function emojiFor(title) {
  const normalized = title.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
  const groups = [
    [/pizza/, '🍕'], [/taco|mexican/, '🌮'], [/hamburgues|burger/, '🍔'],
    [/cafe|coffee/, '☕'], [/cerveza|birra|bar\b/, '🍻'], [/cumple|fiesta|celebra/, '🎉'],
    [/viaje|vacacion|vuelo/, '✈️'], [/playa|mar\b/, '🏖️'], [/cine|pelicula/, '🎬'],
    [/futbol|partido/, '⚽'], [/juego|gaming/, '🎮'], [/casa|alquiler|renta/, '🏠'],
    [/comida|cena|almuerzo|desayuno|restaurante/, '🍽️'], [/compra|supermercado/, '🛒']
  ];
  return groups.find(([pattern]) => pattern.test(normalized))?.[1] ?? '📅';
}
function balances(event) {
  const result = Object.fromEntries(event.participantIds.map(id => [id, 0]));
  for (const expense of event.expenses) {
    result[expense.payerId] += expense.amountMinor;
    const ordered = [...expense.includedIds].sort();
    const base = Math.floor(expense.amountMinor / ordered.length);
    const remainder = expense.amountMinor % ordered.length;
    ordered.forEach((id, i) => { result[id] -= base + (i < remainder ? 1 : 0); });
  }
  for (const payment of event.payments.filter(p => p.status === 'confirmed')) {
    result[payment.fromId] += payment.amountMinor;
    result[payment.toId] -= payment.amountMinor;
  }
  return result;
}
function suggestions(event) {
  const entries = Object.entries(balances(event)).sort(([a], [b]) => a < b ? -1 : a > b ? 1 : 0);
  const debtors = entries.filter(([, n]) => n < 0).map(([id, n]) => ({ id, amount: -n }));
  const creditors = entries.filter(([, n]) => n > 0).map(([id, n]) => ({ id, amount: n }));
  const result = [];
  let a = 0, b = 0;
  while (a < debtors.length && b < creditors.length) {
    const amountMinor = Math.min(debtors[a].amount, creditors[b].amount);
    result.push({ fromId: debtors[a].id, toId: creditors[b].id, amountMinor });
    if ((debtors[a].amount -= amountMinor) === 0) a++;
    if ((creditors[b].amount -= amountMinor) === 0) b++;
  }
  return result;
}
function changeEvent(original, actor, action, input, now) {
  const event = structuredClone(original);
  requireThat(event.participantIds.includes(actor), 'No perteneces a este evento.', 'permission-denied');
  const owner = actor === event.ownerId;
  const ownerOnly = () => requireThat(owner, 'Solo el organizador puede hacer este cambio.', 'permission-denied');
  if (action === 'deleteEvent') {
    ownerOnly();
    requireThat(event.expenses.length === 0 && event.payments.length === 0,
      'No puedes eliminar un evento que tiene gastos o pagos registrados.');
    return null;
  }
  if (action === 'updateParticipants') {
    ownerOnly();
    const next = ids(input.participantIds);
    requireThat(next.includes(event.ownerId), 'El organizador debe permanecer en el evento.');
    const removed = event.participantIds.filter(id => !next.includes(id));
    for (const id of removed) {
      const count = event.expenses.filter(e => e.payerId === id || e.includedIds.includes(id)).length;
      requireThat(count === 0, `No puedes quitar a este participante: tiene ${count} gastos asociados. Edita o borra esos gastos primero.`);
      requireThat(!event.payments.some(p => p.fromId === id || p.toId === id),
        'No puedes quitar a un participante con pagos registrados.');
    }
    event.participantIds = next;
  } else if (['addExpense', 'updateExpense', 'deleteExpense'].includes(action)) {
    const index = event.expenses.findIndex(e => e.id === input.expenseId);
    if (action !== 'addExpense') {
      requireThat(index >= 0, 'Este gasto ya no existe.', 'not-found');
      requireThat(owner || event.expenses[index].createdBy === actor,
        'Solo el autor o el organizador puede editar este gasto.', 'permission-denied');
    }
    if (action === 'deleteExpense') event.expenses.splice(index, 1);
    else {
      const includedIds = ids(input.includedIds);
      requireThat(includedIds.every(id => event.participantIds.includes(id)) && event.participantIds.includes(input.payerId),
        'El pagador y los participantes deben pertenecer al evento.');
      requireThat(Number.isSafeInteger(input.amountMinor) && input.amountMinor > 0 && input.amountMinor <= 100000000,
        'El monto debe ser mayor que cero y tener como máximo dos decimales.', 'invalid-argument');
      const expense = {
        id: text(input.expenseId), title: text(input.title), amountMinor: input.amountMinor,
        payerId: input.payerId, includedIds,
        createdBy: action === 'addExpense' ? actor : event.expenses[index].createdBy,
        createdAt: action === 'addExpense' ? now : event.expenses[index].createdAt
      };
      if (action === 'addExpense') {
        if (index !== -1) {
          const previous = event.expenses[index];
          requireThat(previous.createdBy === actor && previous.title === expense.title &&
            previous.amountMinor === expense.amountMinor && previous.payerId === expense.payerId &&
            JSON.stringify(previous.includedIds) === JSON.stringify(expense.includedIds),
            'Este gasto ya fue registrado. Cierra el formulario y edítalo desde la lista.');
          return event;
        }
        requireThat(event.expenses.length < 500, 'Este evento alcanzó el límite de 500 gastos.');
        event.expenses.push(expense);
      } else event.expenses[index] = expense;
    }
    // Editing a bill does not erase money that somebody reported transferring.
    // Only the receiver can confirm or reject that transfer.
  } else if (action === 'reportPayment') {
    requireThat(actor === input.fromId, 'Solo puedes informar tus propios pagos.', 'permission-denied');
    requireThat(input.revision === event.revision, 'El balance cambió. Revisa los importes antes de confirmar.');
    requireThat(!event.payments.some(p => p.status === 'pending'), 'Primero debe confirmarse o rechazarse el pago pendiente.');
    const suggested = suggestions(event).find(p => p.fromId === actor && p.toId === input.toId && p.amountMinor === input.amountMinor);
    requireThat(suggested, 'El importe ya no coincide con el balance actual.');
    requireThat(event.payments.length < 500, 'Este evento alcanzó el límite de pagos.');
    const paymentId = text(input.paymentId);
    requireThat(!event.payments.some(p => p.id === paymentId), 'Este identificador de pago ya fue utilizado.');
    event.payments.push({ ...suggested, id: paymentId, status: 'pending', createdAt: now });
  } else if (['confirmPayment', 'rejectPayment'].includes(action)) {
    const index = event.payments.findIndex(p => p.id === input.paymentId && p.status === 'pending');
    requireThat(index >= 0, 'Este pago ya fue resuelto o cambió el balance.');
    requireThat(event.payments[index].toId === actor, 'Solo quien recibe puede confirmar o rechazar el pago.', 'permission-denied');
    if (action === 'confirmPayment') event.payments[index].status = 'confirmed';
    else event.payments.splice(index, 1);
  } else throw new DomainError('Operación desconocida.', 'invalid-argument');
  event.updatedAt = now;
  event.revision += 1;
  return event;
}
module.exports = { DomainError, requireThat, text, ids, emojiFor, balances, suggestions, changeEvent };
