const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { resolve } = require('node:path');
const { test } = require('node:test');
const vm = require('node:vm');

function app(instant = '2026-09-29T16:59:59.000Z') {
  let now = new Date(instant).getTime();
  const storage = new Map();
  const listeners = {};
  const elements = {};
  class FakeDate extends Date {
    constructor(...args) { super(...(args.length ? args : [now])); }
    static now() { return now; }
  }
  const context = vm.createContext({
    Date: FakeDate, Intl, console, URLSearchParams,
    localStorage: { getItem: (key) => storage.get(key) || null, setItem: (key, value) => storage.set(key, value) },
    document: {
      hidden: false,
      getElementById: (id) => elements[id] || null,
      addEventListener: (event, callback) => { listeners[`document:${event}`] = callback; },
    },
    window: { addEventListener: (event, callback) => { listeners[event] = callback; } },
    setTimeout: (callback, delay) => { context.timer = { callback, delay }; return 1; },
    clearTimeout: () => {},
  });
  vm.runInContext(readFileSync(resolve(__dirname, '../../main/webapp/resources/js/main.js'), 'utf8'), context);
  vm.runInContext('renderQueueTable = () => {};', context);
  return {
    context, storage, listeners, elements,
    run: (code) => vm.runInContext(code, context),
    advance: (instant) => { now = new Date(instant).getTime(); },
  };
}

test('Vietnam midnight, UTC timestamps and missing legacy dates', () => {
  const a = app();
  assert.equal(a.run("clinicDate('2026-09-29T16:59:59Z')"), '2026-09-29');
  assert.equal(a.run("clinicDate('2026-09-29T17:00:00Z')"), '2026-09-30');
  assert.equal(a.run("clinicDate('2026-09-30T00:00:00')"), '2026-09-30');
  assert.equal(a.run('flowDate(null)'), '');
  assert.equal(a.run("flowDate({createdAt: 'invalid'})"), '');
});

test('reception resets numbering and excludes older and undated records without deleting them', () => {
  const a = app();
  a.run(`patientRecords = {
    old: {patientCode: 'old', createdAt: '2026-09-28T10:00:00'},
    current: {patientCode: 'current', createdAt: '2026-09-29T10:00:00'},
    unknown: {patientCode: 'unknown'},
    returning: {patientCode: 'returning', createdAt: '2026-09-01T10:00:00'}
  };
  updatePatientFlow({id: 'returning', name: 'Returning'}, 'waiting', 'Room');
  renderDailyReceptionQueue();`);
  assert.equal(a.run('queueData.map(item => item.id).join()'), 'current,returning');
  assert.equal(a.run('queueData[0].stt'), 1);
  const saved = a.storage.get('hisPatientFlow');
  a.advance('2026-09-29T17:00:00Z');
  a.run('refreshDailyPatients()');
  assert.equal(a.run('queueData.length'), 0);
  assert.equal(a.run('Object.keys(patientRecords).length'), 4);
  assert.equal(a.storage.get('hisPatientFlow'), saved);
  a.run("updatePatientFlow({id: 'returning', name: 'Returning'}, 'waiting', 'Room'); renderDailyReceptionQueue();");
  assert.equal(a.run('queueData[0].stt'), 1);
  assert.equal(a.run("getFlowForPatient('returning').visitDate"), '2026-09-30');
});

test('midnight timer refreshes once and focus/visibility catch up after sleep', () => {
  const a = app();
  a.run('let refreshes = 0; loadExaminationQueue = (preserve) => { if (preserve) refreshes++; }; scheduleDailyPatientRefresh();');
  assert.equal(a.context.timer.delay, 1000);
  a.advance('2026-09-29T17:00:00Z');
  a.context.timer.callback();
  assert.equal(a.run('refreshes'), 1);
  a.listeners.focus();
  assert.equal(a.run('refreshes'), 1);
  a.advance('2026-10-02T03:00:00Z');
  a.listeners['document:visibilitychange']();
  assert.equal(a.run('refreshes'), 2);
  assert.ok(a.context.timer.delay > 0 && a.context.timer.delay <= 86400000);
});

test('overnight completion retains original reception date and request id', () => {
  const a = app();
  a.run(`updatePatientFlow({id: 'p', name: 'Patient'}, 'examining', 'Room');
    const flow = getPatientFlow(); flow.p.examinationRequestId = 'request'; savePatientFlow(flow);`);
  a.advance('2026-09-29T17:00:00Z');
  a.run("updatePatientFlow({id: 'p', name: 'Patient'}, 'awaiting_payment', 'Room');");
  assert.equal(a.run("getFlowForPatient('p').visitDate"), '2026-09-29');
  assert.equal(a.run("getFlowForPatient('p').examinationRequestId"), 'request');
});

test('examination queue shows only today in the selected room and keeps an open draft on rollover', () => {
  const a = app('2026-09-29T17:00:00Z');
  const element = () => ({
    children: [], dataset: {},
    replaceChildren() { this.children = []; },
    appendChild(child) { this.children.push(child); },
    addEventListener() {},
  });
  a.context.document.createElement = element;
  a.context.window.location = { search: '?phong=Room' };
  a.elements.examinationQueueBody = element();
  a.elements.examPatientCode = { value: 'old' };
  a.run(`let selected = '';
    selectExaminationPatient = (id) => { selected = id; };
    filterExaminationQueue = () => {};
    savePatientFlow({
      old: {id: 'old', room: 'Room', status: 'examining', createdAt: '2026-09-29T16:59:59Z'},
      today: {id: 'today', room: 'Room', status: 'waiting', createdAt: '2026-09-29T17:00:00Z'},
      elsewhere: {id: 'elsewhere', room: 'Other', status: 'waiting', visitDate: '2026-09-30'},
      done: {id: 'done', room: 'Room', status: 'completed', visitDate: '2026-09-30'}
    });
    loadExaminationQueue(true);`);
  assert.equal(a.elements.examinationQueueBody.children.length, 1);
  assert.equal(a.elements.examinationQueueBody.children[0].dataset.patientCode, 'today');
  assert.equal(a.elements.examinationQueueBody.children[0].dataset.queueDate, '2026-09-30');
  assert.equal(a.run('selected'), '');
  assert.equal(a.elements.examPatientCode.value, 'old');
});
