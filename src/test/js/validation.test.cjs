const assert = require('node:assert/strict');
const { test } = require('node:test');
const HisValidation = require('../../main/webapp/resources/js/validation.js');

test('HisValidation.personName rules', () => {
  assert.equal(HisValidation.personName(null), null);
  assert.equal(HisValidation.personName(''), null);
  assert.equal(HisValidation.personName('Nguyễn Văn A'), 'Nguyễn Văn A');
  assert.equal(HisValidation.personName('Trần Thị Bích Ngọc'), 'Trần Thị Bích Ngọc');
  assert.equal(HisValidation.personName('John Doe'), 'John Doe');

  assert.throws(() => HisValidation.personName('A'), /ít nhất 2 ký tự/);
  assert.throws(() => HisValidation.personName('Nguyen Van 123'), /không được chứa chữ số/);
  assert.throws(() => HisValidation.personName('Bệnh nhân @#!'), /không chứa ký tự đặc biệt/);
});

test('HisValidation.birthDate rules', () => {
  assert.equal(HisValidation.birthDate(null), null);
  assert.equal(HisValidation.birthDate(''), null);
  assert.equal(HisValidation.birthDate('1990'), '1990-01-01');
  assert.equal(HisValidation.birthDate('15/08/1995'), '1995-08-15');
  assert.equal(HisValidation.birthDate('2000-12-31'), '2000-12-31');

  assert.throws(() => HisValidation.birthDate('31/02/2020'), /không tồn tại/);
  assert.throws(() => HisValidation.birthDate('1899'), /từ năm 1900/);
  assert.throws(() => HisValidation.birthDate('2099-01-01'), /ngày hiện tại/);
  assert.throws(() => HisValidation.birthDate('abc'), /đủ 4 chữ số/);
});

test('HisValidation.phone rules', () => {
  assert.equal(HisValidation.phone(null), null);
  assert.equal(HisValidation.phone(''), null);
  assert.equal(HisValidation.phone('0912345678'), '0912345678');
  assert.equal(HisValidation.phone('+84912345678'), '+84912345678');
  assert.equal(HisValidation.phone('(028) 3999 8888'), '(028) 3999 8888');

  assert.throws(() => HisValidation.phone('0912'), /8 đến 15 chữ số/);
  assert.throws(() => HisValidation.phone('09123456'), /10 chữ số/);
  assert.throws(() => HisValidation.phone('12345'), /8 đến 15 chữ số/);
  assert.throws(() => HisValidation.phone('12345678901234567'), /8 đến 15 chữ số/);
  assert.throws(() => HisValidation.phone('09abcxyz12'), /chỉ được chứa chữ số/);
});

test('HisValidation.identityNumber rules', () => {
  assert.equal(HisValidation.identityNumber(null), null);
  assert.equal(HisValidation.identityNumber(''), null);
  assert.equal(HisValidation.identityNumber('079195001234'), '079195001234');
  assert.equal(HisValidation.identityNumber('024567891'), '024567891');

  assert.throws(() => HisValidation.identityNumber('12345'), /9 hoặc 12 chữ số/);
  assert.throws(() => HisValidation.identityNumber('079195001234567'), /9 hoặc 12 chữ số/);
  assert.throws(() => HisValidation.identityNumber('079CCCD12345'), /9 hoặc 12 chữ số/);
});

test('HisValidation.email rules', () => {
  assert.equal(HisValidation.email(null), null);
  assert.equal(HisValidation.email(''), null);
  assert.equal(HisValidation.email('bacsi@clinic.vn'), 'bacsi@clinic.vn');
  assert.equal(HisValidation.email('admin.his@medicare.com.vn'), 'admin.his@medicare.com.vn');

  assert.throws(() => HisValidation.email('invalid-email'), /không đúng định dạng/);
  assert.throws(() => HisValidation.email('bacsi@'), /không đúng định dạng/);
  assert.throws(() => HisValidation.email('@clinic.vn'), /không đúng định dạng/);
});

test('HisValidation.bloodPressure rules', () => {
  assert.equal(HisValidation.bloodPressure(null), null);
  assert.equal(HisValidation.bloodPressure(''), null);
  assert.deepEqual(HisValidation.bloodPressure('120/80'), { systolic: 120, diastolic: 80 });
  assert.deepEqual(HisValidation.bloodPressure('135 / 85'), { systolic: 135, diastolic: 85 });

  assert.throws(() => HisValidation.bloodPressure('120'), /định dạng Tâm thu\/Tâm trương/);
  assert.throws(() => HisValidation.bloodPressure('80/120'), /lớn hơn huyết áp tâm trương/);
  assert.throws(() => HisValidation.bloodPressure('300/80'), /ngoài khoảng cho phép/);
  assert.throws(() => HisValidation.bloodPressure('120/20'), /ngoài khoảng cho phép/);
});

test('HisValidation.username rules', () => {
  assert.equal(HisValidation.username(null), null);
  assert.equal(HisValidation.username(''), null);
  assert.equal(HisValidation.username('admin'), 'admin');
  assert.equal(HisValidation.username('bacsi_ck1'), 'bacsi_ck1');
  assert.equal(HisValidation.username('letan.ca1'), 'letan.ca1');

  assert.throws(() => HisValidation.username('ab'), /3 đến 50 ký tự/);
  assert.throws(() => HisValidation.username('user with space'), /chỉ chứa chữ không dấu/);
  assert.throws(() => HisValidation.username('user@special!'), /chỉ chứa chữ không dấu/);
});

test('HisValidation.number rules', () => {
  assert.equal(HisValidation.number(null), null);
  assert.equal(HisValidation.number(''), null);
  assert.equal(HisValidation.number('80', { min: 30, max: 250, integer: true, label: 'Mạch' }), 80);
  assert.equal(HisValidation.number('36.5', { min: 34, max: 43, label: 'Nhiệt độ' }), 36.5);

  assert.throws(() => HisValidation.number('abc', { label: 'Mạch' }), /phải là số/);
  assert.throws(() => HisValidation.number('80.5', { integer: true, label: 'Mạch' }), /phải là số nguyên/);
  assert.throws(() => HisValidation.number('20', { min: 30, max: 250, label: 'Mạch' }), /từ 30 đến 250/);
});
