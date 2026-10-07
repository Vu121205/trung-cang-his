let queueData = [];
let patientRecords = {};

let icdCatalog = {};
let medicineCatalog = [];
let supplyCatalog = [];

const FLOW_STORAGE_KEY = 'hisPatientFlow';
const CLINIC_TIME_ZONE = 'Asia/Ho_Chi_Minh';
const clinicDateFormatter = new Intl.DateTimeFormat('en-CA', {
  timeZone: CLINIC_TIME_ZONE, year: 'numeric', month: '2-digit', day: '2-digit',
});

function clinicDate(value = new Date()) {
  if (!value) return '';
  // Backend LocalDate/LocalDateTime values already represent the clinic's local date.
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}(?:T[\d:.]+)?$/.test(value)) return value.slice(0, 10);
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  const parts = Object.fromEntries(clinicDateFormatter.formatToParts(date).map((part) => [part.type, part.value]));
  return `${parts.year}-${parts.month}-${parts.day}`;
}

function flowDate(flow) {
  return clinicDate(flow?.visitDate || flow?.createdAt || flow?.updatedAt || '');
}

let displayedClinicDate = clinicDate();
let dailyRefreshTimer;

function refreshDailyPatients() {
  const today = clinicDate();
  if (today === displayedClinicDate) return;
  displayedClinicDate = today;
  renderDailyReceptionQueue();
  ['examFrom', 'examTo'].forEach((id) => {
    const input = document.getElementById(id);
    if (input) input.value = '';
  });
  // Keep an open examination draft so work in progress can still be completed.
  loadExaminationQueue(true);
  if (document.getElementById('receptionTableBody')) {
    loadPatientsFromApi().catch((error) => console.warn('Không tải được danh sách ngày mới:', error.message));
  }
}

function scheduleDailyPatientRefresh() {
  clearTimeout(dailyRefreshTimer);
  refreshDailyPatients();
  const nextMidnight = new Date(`${clinicDate()}T00:00:00+07:00`).getTime() + 24 * 60 * 60 * 1000;
  dailyRefreshTimer = setTimeout(scheduleDailyPatientRefresh, Math.max(100, nextMidnight - Date.now()));
}

document.addEventListener('DOMContentLoaded', scheduleDailyPatientRefresh);
window.addEventListener('focus', scheduleDailyPatientRefresh);
window.addEventListener('pageshow', scheduleDailyPatientRefresh);
document.addEventListener('visibilitychange', () => {
  if (!document.hidden) scheduleDailyPatientRefresh();
});

function getPatientFlow() {
  try {
    return JSON.parse(localStorage.getItem(FLOW_STORAGE_KEY) || '{}');
  } catch (error) {
    return {};
  }
}

function savePatientFlow(flow) {
  localStorage.setItem(FLOW_STORAGE_KEY, JSON.stringify(flow));
}

function updatePatientFlow(patient, status, room) {
  const flow = getPatientFlow();
  const now = new Date().toISOString();
  const previous = status === 'waiting' && flowDate(flow[patient.id]) !== clinicDate(now) ? null : flow[patient.id];
  flow[patient.id] = {
    ...(previous || {}),
    id: patient.id,
    name: patient.name,
    room: room || previous?.room || 'Chưa phân phòng',
    status,
    visitDate: flowDate(previous) || clinicDate(now),
    createdAt: previous?.createdAt || now,
    updatedAt: now,
  };
  savePatientFlow(flow);
  return flow[patient.id];
}

function getFlowForPatient(id) {
  return getPatientFlow()[id] || null;
}

function lookupIcdDiagnosis(value) {
  const code = value.trim().toUpperCase();
  const diagnosis = icdCatalog[code] || '';
  document.getElementById('docDiagnosis').value = diagnosis;
  document.getElementById('icdSuggestion').textContent = diagnosis ? `Bệnh chính: ${diagnosis}` : code ? 'Chưa tìm thấy mã ICD trong danh mục.' : '';
  const container = document.getElementById('icdSuggestions');
  container.replaceChildren();
  container.classList.toggle('d-none', !code);
  if (!code) return;
  const matches = Object.entries(icdCatalog)
    .filter(([key, name]) => `${key} ${name}`.toLowerCase().includes(code.toLowerCase()))
    .slice(0, 20);
  for (const [key, name] of matches) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'list-group-item list-group-item-action py-2 text-start';
    button.textContent = `${key} — ${name}`;
    button.addEventListener('click', () => selectIcdDiagnosis(key));
    container.appendChild(button);
  }
  if (!matches.length) container.textContent = 'Không tìm thấy mã ICD phù hợp.';
}

function selectIcdDiagnosis(code) {
  document.getElementById('docIcdCode').value = code;
  lookupIcdDiagnosis(code);
  document.getElementById('icdSuggestions').classList.add('d-none');
}

function suggestMedicines(value) {
  const container = document.getElementById('medicineSuggestions');
  const keyword = value.trim().toLowerCase();
  container.replaceChildren();
  container.dataset.activeIndex = '-1';
  container.classList.toggle('d-none', !keyword);
  if (!keyword) return;
  const matches = medicineCatalog.filter((medicine) => keyword.split(/\s+/).every((term) => `${medicine.name} ${medicine.active} ${medicine.hint}`.toLowerCase().includes(term))).slice(0, 20);
  for (const medicine of matches) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'list-group-item list-group-item-action py-2';
    button.textContent = `${medicine.name} · ${medicine.active} · ${medicine.hint}`;
    button.dataset.itemId = medicine.id;
    button.addEventListener('click', () => selectMedicine(medicine.id));
    container.appendChild(button);
  }
  if (!matches.length) container.textContent = 'Không tìm thấy thuốc phù hợp.';
}

function handleSuggestionKeydown(event, type) {
  const searchId = type === 'medicine' ? 'docMedicineSearch' : 'docSupplySearch';
  const suggestionId = type === 'medicine' ? 'medicineSuggestions' : 'supplySuggestions';
  const container = document.getElementById(suggestionId);
  const buttons = [...container.querySelectorAll('button[data-item-id]')];
  if (event.key === 'Escape') {
    container.classList.add('d-none');
    return;
  }
  if (!buttons.length || container.classList.contains('d-none')) return;
  let index = Number(container.dataset.activeIndex || -1);
  if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
    event.preventDefault();
    index = (index + (event.key === 'ArrowDown' ? 1 : -1) + buttons.length) % buttons.length;
    buttons.forEach((button, buttonIndex) => {
      const active = buttonIndex === index;
      button.classList.toggle('active', active);
      button.setAttribute('aria-selected', String(active));
    });
    container.dataset.activeIndex = String(index);
    buttons[index].scrollIntoView({ block: 'nearest' });
    return;
  }
  if (event.key !== 'Enter') return;
  event.preventDefault();
  const selected = buttons[index >= 0 ? index : 0];
  const id = Number(selected.dataset.itemId);
  const listId = type === 'medicine' ? 'selectedMedicineList' : 'selectedSupplyList';
  const list = document.getElementById(listId);
  const previousCount = list.children.length;
  if (type === 'medicine') selectMedicine(id);
  else selectSupply(id);
  const row = list.children.length > previousCount ? list.lastElementChild : null;
  if (row) focusScheduleField(row, 0);
  else document.getElementById(searchId).focus();
}

function focusScheduleField(row, index) {
  const fields = [...row.querySelectorAll('select, input[type="number"]')];
  if (fields[index]) fields[index].focus();
}

function handleScheduleEnter(event, row, searchId) {
  if (event.key !== 'Enter') return;
  event.preventDefault();
  const fields = [...row.querySelectorAll('select, input[type="number"]')];
  const currentIndex = fields.indexOf(event.currentTarget);
  if (currentIndex >= 0 && currentIndex < fields.length - 1) focusScheduleField(row, currentIndex + 1);
  else document.getElementById(searchId).focus();
}

function createScheduleInput(type, ariaLabel, options = null, value = null) {
  const input = document.createElement(type === 'select' ? 'select' : 'input');
  input.className = type === 'select' ? 'form-select form-select-sm' : 'form-control form-control-sm text-center';
  input.setAttribute('aria-label', ariaLabel);
  if (type === 'select') options.forEach((option) => input.add(new Option(option, option)));
  else { input.type = type; input.min = '0'; input.max = '10000'; input.step = '1'; input.value = value; }
  return input;
}

function updateScheduleRow(row) {
  const days = Number(row.children[2].value) || 0;
  const doses = [...row.querySelectorAll('input[type="number"]')].slice(1).map((input) => Number(input.value) || 0);
  const quantity = days * doses.reduce((sum, dose) => sum + dose, 0);
  row.querySelector('[data-quantity]').value = String(quantity);
  row.querySelector('[data-quantity]').textContent = quantity;
  const periods = ['sáng', 'trưa', 'chiều', 'tối'].filter((period, index) => doses[index] > 0).map((period, index) => `${doses[index]} đơn vị buổi ${period}`);
  const route = row.children[1].value.toLowerCase();
  row.querySelector('[data-instruction]').value = periods.length && days > 0
    ? `${route.charAt(0).toUpperCase()}${route.slice(1)} ${periods.join(', ')} trong ${days} ngày.`
    : '';
}

function selectMedicine(id) {
  const medicine = medicineCatalog.find((item) => item.id === id);
  if (!medicine) return;
  const container = document.getElementById('selectedMedicineList');
  if ([...container.children].some((row) => Number(row.dataset.medicineId) === id)) return;
  const row = document.createElement('div');
  row.className = 'medication-grid-row mb-2';
  row.dataset.medicineId = id;
  const label = document.createElement('span');
  label.className = 'medication-name';
  label.textContent = `${medicine.name} (${medicine.unit})`;
  const route = createScheduleInput('select', 'Đường dùng ' + medicine.name, ['Uống', 'Ngậm', 'Bôi', 'Nhỏ', 'Tiêm']);
  const days = createScheduleInput('number', 'Số ngày dùng ' + medicine.name, null, 1);
  const doses = ['Sáng', 'Trưa', 'Chiều', 'Tối'].map((label) => createScheduleInput('number', `${label} ${medicine.name}`, null, 0));
  const quantity = document.createElement('output');
  quantity.dataset.quantity = '';
  quantity.className = 'medication-total text-center';
  quantity.setAttribute('aria-label', 'Tổng số lượng ' + medicine.name);
  const instruction = document.createElement('input');
  instruction.dataset.instruction = '';
  instruction.className = 'form-control form-control-sm medication-instruction';
  instruction.readOnly = true;
  instruction.placeholder = 'Tự động theo buổi';
  instruction.maxLength = 2000;
  instruction.setAttribute('aria-label', 'Cách dùng ' + medicine.name);
  const remove = document.createElement('button');
  remove.type = 'button';
  remove.className = 'btn btn-outline-danger btn-sm';
  remove.setAttribute('aria-label', 'Xóa ' + medicine.name);
  remove.innerHTML = '<i class="fa-solid fa-trash"></i>';
  remove.addEventListener('click', () => row.remove());
  row.append(label, route, days, ...doses, quantity, instruction, remove);
  row.querySelectorAll('input, select').forEach((input) => {
    input.addEventListener('input', () => updateScheduleRow(row));
    input.addEventListener('keydown', (event) => handleScheduleEnter(event, row, 'docMedicineSearch'));
  });
  updateScheduleRow(row);
  container.appendChild(row);
  document.getElementById('docMedicineSearch').value = '';
  document.getElementById('medicineSuggestions').classList.add('d-none');
}

function suggestSupplies(value) {
  const container = document.getElementById('supplySuggestions');
  const keyword = value.trim().toLowerCase();
  container.replaceChildren();
  container.dataset.activeIndex = '-1';
  container.classList.toggle('d-none', !keyword);
  if (!keyword) return;
  const matches = supplyCatalog.filter((supply) => keyword.split(/\s+/).every((term) => `${supply.code} ${supply.name} ${supply.hint}`.toLowerCase().includes(term))).slice(0, 20);
  for (const supply of matches) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'list-group-item list-group-item-action py-2';
    button.textContent = `${supply.code} · ${supply.name} · ${supply.unit}`;
    button.dataset.itemId = supply.id;
    button.addEventListener('click', () => selectSupply(supply.id));
    container.appendChild(button);
  }
  if (!matches.length) container.textContent = 'Không tìm thấy vật tư phù hợp.';
}

function selectSupply(id) {
  const supply = supplyCatalog.find((item) => item.id === id);
  if (!supply) return;
  const container = document.getElementById('selectedSupplyList');
  if ([...container.children].some((row) => Number(row.dataset.supplyId) === id)) return;
  const row = document.createElement('div');
  row.className = 'medication-grid-row mb-2';
  row.dataset.supplyId = id;
  const label = document.createElement('span');
  label.className = 'medication-name';
  label.textContent = `${supply.name} (${supply.unit})`;
  const route = createScheduleInput('select', 'Cách sử dụng ' + supply.name, ['Sử dụng', 'Bôi', 'Đắp', 'Thay']);
  const days = createScheduleInput('number', 'Số ngày sử dụng ' + supply.name, null, 1);
  const doses = ['Sáng', 'Trưa', 'Chiều', 'Tối'].map((label) => createScheduleInput('number', `${label} ${supply.name}`, null, 0));
  const quantity = document.createElement('output');
  quantity.dataset.quantity = '';
  quantity.className = 'medication-total text-center';
  const instruction = document.createElement('input');
  instruction.dataset.instruction = '';
  instruction.className = 'form-control form-control-sm medication-instruction';
  instruction.readOnly = true;
  instruction.placeholder = 'Tự động theo buổi';
  const remove = document.createElement('button');
  remove.type = 'button';
  remove.className = 'btn btn-outline-danger btn-sm';
  remove.setAttribute('aria-label', 'Xóa ' + supply.name);
  remove.innerHTML = '<i class="fa-solid fa-trash"></i>';
  remove.addEventListener('click', () => row.remove());
  row.append(label, route, days, ...doses, quantity, instruction, remove);
  row.querySelectorAll('input, select').forEach((input) => {
    input.addEventListener('input', () => updateScheduleRow(row));
    input.addEventListener('keydown', (event) => handleScheduleEnter(event, row, 'docSupplySearch'));
  });
  updateScheduleRow(row);
  container.appendChild(row);
  document.getElementById('docSupplySearch').value = '';
  document.getElementById('supplySuggestions').classList.add('d-none');
}

// Init App
document.addEventListener('DOMContentLoaded', () => {
  renderQueueTable();
});

// Fill form from old patient lookup selection
function fillPatientForm(id, name, phone, dateOfBirth, gender, address, identity) {
  const recPatientId = document.getElementById('recPatientId');
  if (recPatientId) recPatientId.value = id || '';

  const recName = document.getElementById('recName');
  if (recName) recName.value = name || '';

  const recPhone = document.getElementById('recPhone');
  if (recPhone) recPhone.value = phone || '';

  const recDob = document.getElementById('recDob');
  if (recDob) recDob.value = dateOfBirth || '';

  const recGender = document.getElementById('recGender');
  if (recGender) recGender.value = gender || 'Nam';

  const recAddress = document.getElementById('recAddress');
  if (recAddress) recAddress.value = address || '';

  const recIdentity = document.getElementById('recIdentity');
  if (recIdentity) recIdentity.value = identity || '';

  const flow = getFlowForPatient(id);
  const roomInput = document.getElementById('recRoom');
  if (roomInput && flow?.room) roomInput.value = flow.room;
  const submitLabel = document.getElementById('receptionSubmitLabel');
  if (submitLabel) submitLabel.textContent = 'Cập nhật thông tin tiếp nhận';

  showToast(`Đã tải thông tin bệnh nhân ${name} vào form!`);
}

// Clear reception inline form
function clearReceptionForm() {
  const fields = ['recPatientId', 'recName', 'recPhone', 'recDob', 'recAddress', 'recIdentity', 'recReason'];
  fields.forEach((id) => {
    const el = document.getElementById(id);
    if (el) el.value = '';
  });

  const recGender = document.getElementById('recGender');
  if (recGender) recGender.value = 'Nam';

  const recInsuranceType = document.getElementById('recInsuranceType');
  if (recInsuranceType) recInsuranceType.value = 'Dịch vụ';
  const submitLabel = document.getElementById('receptionSubmitLabel');
  if (submitLabel) submitLabel.textContent = 'Đăng Ký & Cấp Số Thứ Tự';
}

// Render Reception Table
function renderQueueTable() {
  const tbody = document.getElementById('receptionTableBody');
  if (!tbody) return;
  tbody.innerHTML = '';

  queueData.forEach((item) => {
    let badgeClass = 'badge-status-waiting';
    let statusText = 'Chờ khám';
    if (item.status === 'examining') {
      badgeClass = 'badge-status-examining';
      statusText = 'Đang khám';
    } else if (item.status === 'done') {
      badgeClass = 'badge-status-done';
      statusText = 'Đã hoàn tất';
    }

    const canEdit = item.status === 'waiting';
    const tr = document.createElement('tr');
    tr.classList.add('reception-patient-row');
    tr.dataset.patientId = item.id;
    tr.innerHTML = `
      <td class="ps-3 fw-bold">${item.stt}</td>
      <td><span class="badge bg-light text-dark border">${item.id}</span></td>
      <td class="fw-semibold">${item.name}</td>
      <td class="text-muted">${item.genderYear}</td>
      <td>${item.room}</td>
      <td><span class="badge ${badgeClass}">${statusText}</span></td>
      <td class="text-end pe-3">
          ${canEdit ? `<button class="btn btn-xs btn-outline-secondary me-1" onclick="editReceptionPatient('${item.id}')"><i class="fa-solid fa-pen me-1"></i>Sửa</button>` : '<span class="text-muted small me-2">Đã bắt đầu khám</span>'}
          ${canEdit && document.querySelector('.navbar-nav a[href="/examination"]') ? `<button class="btn btn-xs btn-outline-primary" onclick="callPatientIntoDoctor('${item.id}', '${item.name.replace(/'/g, "\\'")}')"><i class="fa-solid fa-arrow-right me-1"></i>Gọi khám</button>` : ''}
      </td>
    `;
    tr.addEventListener('click', (event) => {
      if (event.target.closest('button')) return;
      editReceptionPatient(item.id);
    });
    tbody.appendChild(tr);
  });

  const totalQueueCount = document.getElementById('totalQueueCount');
  if (totalQueueCount) {
    totalQueueCount.innerText = `${queueData.length} Bệnh nhân`;
  }
}

// Search Patient Simulation
function handleSearchPatient() {
  const kwInput = document.getElementById('searchKeyword');
  if (!kwInput) return;
  const kw = kwInput.value.trim();
  if (!kw) return;
  showToast(`Đã tìm kiếm với từ khóa: "${kw}"`);
}

function selectPatientToQueue(id, name, phone, year, gender) {
  const nextStt = 100 + queueData.length + 1;
  queueData.push({
    stt: nextStt,
    id: id,
    name: name,
    genderYear: `${gender} / ${year}`,
    room: 'Phòng Khám Nội 01',
    status: 'waiting',
  });
  updatePatientFlow({ id, name }, 'waiting', 'Phòng khám số 1');
  renderQueueTable();
  showToast(`Đã cấp số ${nextStt} cho bệnh nhân ${name}`);
}

// Modal Handlers
function openNewPatientModal() {
  const modalEl = document.getElementById('newPatientModal');
  if (!modalEl) return;
  const modal = new bootstrap.Modal(modalEl);
  modal.show();
}

async function submitNewPatient() {
  const form = document.getElementById('newPatientForm');
  if (typeof HisValidation !== 'undefined' && !HisValidation.validate(form)) {
    return;
  }
  const copy = {
    recName: 'newPtName', recPhone: 'newPtPhone', recDob: 'newPtDob', recGender: 'newPtGender',
    recIdentity: 'newPtIdentity', recAddress: 'newPtAddress',
    recRoom: 'newPtRoom', recReason: 'newPtReason'
  };
  Object.entries(copy).forEach(([target, source]) => {
    const targetEl = document.getElementById(target);
    const sourceEl = document.getElementById(source);
    if (targetEl && sourceEl) targetEl.value = sourceEl.value;
  });
  bootstrap.Modal.getInstance(document.getElementById('newPatientModal'))?.hide();
  await submitInlinePatient();
}

function editReceptionPatient(id) {
  const patient = patientRecords[id];
  const flow = getFlowForPatient(id);
  if (!patient || (flow && flow.status !== 'waiting')) {
    alert('Không thể sửa vì bệnh nhân đã bắt đầu được xử lý tại phòng khám.');
    return;
  }
  fillPatientForm(
    patient.patientCode,
    patient.fullName,
    patient.phone,
    patient.dateOfBirth || '',
    patient.gender === 'FEMALE' ? 'Nữ' : patient.gender === 'OTHER' ? 'Khác' : 'Nam',
    patient.address || '',
    patient.identityNumber || ''
  );
  document.getElementById('recName')?.focus();
}

// Doctor Section Logic
function callPatientIntoDoctor(id, name) {
  const current = getFlowForPatient(id);
  updatePatientFlow({ id, name }, 'examining', current?.room);
  const docNameEl = document.getElementById('docPatientName');
  if (docNameEl) docNameEl.innerText = name;

  const docMetaEl = document.getElementById('docPatientMeta');
  if (docMetaEl) docMetaEl.innerText = `Mã BN: ${id} | Nam | 1992 (34 tuổi)`;

  showToast(`Đã tiếp nhận bệnh nhân ${name} vào phòng khám`);
  window.location.href = '/examination';
}

function addLabOrder() {
  const tbody = document.getElementById('docLabBody');
  if (!tbody) return;
  const tr = document.createElement('tr');
  tr.innerHTML = `
    <td>
        <select class="form-select form-select-sm">
            <option>X-Quang Ngực Thẳng</option>
            <option>Xét nghiệm Công thức máu (CBC)</option>
            <option>Nội soi dạ dày</option>
            <option>Điện tâm đồ (ECG)</option>
        </select>
    </td>
    <td>180,000đ</td>
    <td><span class="badge bg-warning text-dark">Chờ thực hiện</span></td>
    <td class="text-center"><button class="btn btn-xs btn-link text-danger" onclick="removeRow(this)"><i class="fa-solid fa-trash"></i></button></td>
  `;
  tbody.appendChild(tr);
}

function addMedicineRow() {
  const tbody = document.getElementById('docDrugBody');
  if (!tbody) return;
  const tr = document.createElement('tr');
  tr.innerHTML = `
    <td><input type="text" class="form-control form-control-sm" placeholder="Tên thuốc..."></td>
    <td><input type="number" class="form-control form-control-sm" value="10"></td>
    <td><input type="text" class="form-control form-control-sm" placeholder="Cách dùng..."></td>
    <td class="text-center"><button class="btn btn-xs btn-link text-danger" onclick="removeRow(this)"><i class="fa-solid fa-trash"></i></button></td>
  `;
  tbody.appendChild(tr);
}

function removeRow(btn) {
  btn.closest('tr').remove();
}

function finishExamination() {
  return persistExamination();
}

function loadExaminationQueue(preserveSelection = false) {
  const table = document.getElementById('examinationQueueBody');
  if (!table) return;
  let selection;
  try {
    selection = JSON.parse(localStorage.getItem('hisRoomSelection') || 'null');
  } catch {
    selection = null;
  }
  const room = new URLSearchParams(window.location.search).get('phong') || (selection?.department === 'Khoa khám bệnh' ? selection.room : null);
  const today = clinicDate();
  const flow = Object.values(getPatientFlow()).filter((item) => flowDate(item) === today && ['waiting', 'examining', 'awaiting_payment'].includes(item.status) && (!room || item.room === room));
  table.replaceChildren();
  flow.forEach((item, index) => {
    const row = document.createElement('tr');
    row.dataset.patientCode = item.id;
    row.dataset.patientStatus = item.status === 'examining' ? 'EXAMINING' : item.status === 'awaiting_payment' ? 'ENDED' : 'WAITING';
    row.dataset.queueDate = flowDate(item);
    for (const value of [index + 1, item.id, item.name, item.room, item.status === 'examining' ? 'Đang khám' : item.status === 'awaiting_payment' ? 'Kết thúc' : 'Chờ khám']) {
      const cell = document.createElement('td');
      cell.textContent = value;
      row.appendChild(cell);
    }
    const action = document.createElement('td');
    const statusCell = row.children[4];
    const button = document.createElement('button');
    button.type = 'button';
    button.className = item.status === 'awaiting_payment' ? 'btn btn-sm btn-outline-warning' : 'btn btn-sm btn-outline-primary';
    button.textContent = item.status === 'awaiting_payment' ? 'Mở lại bệnh án' : 'Mở hồ sơ';
    if (item.status === 'awaiting_payment') {
      if (!item.examinationId) button.disabled = true;
      else apiRequest(`/examination-history/${item.examinationId}/reopen-eligibility`)
        .then((eligible) => {
          if (!eligible) {
            button.disabled = true;
            button.textContent = 'Viện phí đã duyệt';
            button.title = 'Không thể mở lại bệnh án sau khi viện phí được duyệt.';
          }
        }).catch(() => {});
    }
    button.addEventListener('click', async (event) => {
      event.stopPropagation();
      if (item.status !== 'awaiting_payment') return;
      try {
        if (!item.examinationId) throw new Error('Không tìm thấy mã hồ sơ khám để mở lại.');
        const detail = await apiRequest(`/examination-history/${item.examinationId}/reopen`, { method: 'POST' });
        updatePatientFlow({ id: item.id, name: item.name }, 'examining', item.room);
        selectExaminationPatient(item.id);
        restoreExaminationFromHistory(detail);
        loadExaminationQueue(true);
        showToast('Đã mở lại bệnh án để bác sĩ chỉnh sửa.');
      } catch (error) { alert(error.message); }
    });
    action.appendChild(button);
    row.appendChild(action);
    row.addEventListener('click', () => {
      if (item.status === 'awaiting_payment') return;
      if (flowDate(item) !== clinicDate()) {
        refreshDailyPatients();
        return;
      }
      if (item.status === 'waiting') {
        updatePatientFlow({ id: item.id, name: item.name }, 'examining', item.room);
        item.status = 'examining';
        row.dataset.patientStatus = 'EXAMINING';
        statusCell.textContent = 'Đang khám';
      }
      selectExaminationPatient(item.id);
      filterExaminationQueue();
    });
    table.appendChild(row);
  });
  const activeFlow = flow.find((item) => ['waiting', 'examining'].includes(item.status));
  if (activeFlow && !(preserveSelection && document.getElementById('examPatientCode')?.value)) selectExaminationPatient(activeFlow.id);
  if (!flow.length) table.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">Chưa có bệnh nhân được phân vào phòng này trong ngày.</td></tr>';
  filterExaminationQueue();
}

function filterExaminationQueue() {
  const query = document.getElementById('examSearch')?.value.trim().toLocaleLowerCase('vi');
  const from = document.getElementById('examFrom')?.value || '';
  const to = document.getElementById('examTo')?.value || '';
  const status = document.getElementById('examStatusFilter')?.value || 'ALL';
  const message = document.getElementById('examQueueMessage');
  if (from && to && from > to) {
    document.querySelectorAll('#examinationQueueBody tr[data-patient-code]').forEach((row) => {
      row.hidden = true;
    });
    if (message) message.textContent = 'Từ ngày phải trước hoặc bằng đến ngày.';
    return;
  }
  let visible = 0;
  document.querySelectorAll('#examinationQueueBody tr[data-patient-code]').forEach((row) => {
    const date = row.dataset.queueDate;
    const matches =
      (!query || row.dataset.patientCode.toLocaleLowerCase('vi').includes(query)) &&
      (status === 'ALL' || row.dataset.patientStatus === status) &&
      (!from || !date || date >= from) &&
      (!to || !date || date <= to);
    row.hidden = !matches;
    if (matches) visible++;
  });
  if (message) message.textContent = visible ? `${visible} bệnh nhân phù hợp.` : 'Không có bệnh nhân phù hợp với bộ lọc.';
}

function selectExaminationPatient(id) {
  const item = getFlowForPatient(id);
  if (!item || (typeof savingExamination !== 'undefined' && savingExamination)) return;
  const code = document.getElementById('examPatientCode');
  if (code.value !== id) {
    ['docSymptoms', 'docMedicalHistory', 'docIcdCode', 'docDiagnosis', 'docClinicalNote', 'docAdvice', 'docMedicineSearch', 'docSupplySearch'].forEach((key) => {
      document.getElementById(key).value = '';
    });
    document.getElementById('docSymptoms').value = item.reason || '';
    document.getElementById('selectedMedicineList').replaceChildren();
    document.getElementById('selectedSupplyList').replaceChildren();
    document.getElementById('icdSuggestion').textContent = '';
    ['icdSuggestions', 'medicineSuggestions', 'supplySuggestions'].forEach((key) => document.getElementById(key).classList.add('d-none'));
  }
  code.value = id;
  document.getElementById('docPatientName').textContent = item.name;
  document.getElementById('docPatientMeta').textContent = `Mã BN: ${id} | Đối tượng: Dịch vụ | Phòng: ${item.room}`;
  document.getElementById('examRoomLabel').textContent = item.room;
  document.querySelectorAll('#examinationQueueBody tr').forEach((row) => row.classList.toggle('table-primary', row.dataset.patientCode === id));
}

function restoreExaminationFromHistory(detail) {
  document.getElementById('selectedMedicineList').replaceChildren();
  document.getElementById('selectedSupplyList').replaceChildren();
  ['docBloodPressure', 'docPulse', 'docTemperature', 'docWeight'].forEach((id) => { document.getElementById(id).value = ''; });
  const diagnosis = detail.diagnoses?.[0];
  if (diagnosis) {
    document.getElementById('docIcdCode').value = diagnosis.code;
    lookupIcdDiagnosis(diagnosis.code);
  }
  document.getElementById('docSymptoms').value = detail.symptoms || '';
  document.getElementById('docMedicalHistory').value = detail.medicalHistory || '';
  document.getElementById('docClinicalNote').value = detail.clinicalNote || '';
  document.getElementById('docAdvice').value = detail.advice || '';
  const vital = detail.vitalSigns;
  if (vital) {
    document.getElementById('docBloodPressure').value = vital.systolic && vital.diastolic ? `${vital.systolic}/${vital.diastolic}` : '';
    document.getElementById('docPulse').value = vital.pulse ?? '';
    document.getElementById('docTemperature').value = vital.temperature ?? '';
    document.getElementById('docWeight').value = vital.weight ?? '';
  }
  for (const line of detail.medicines || []) {
    if (line.inventoryType === 'SUPPLY') selectSupply(line.medicineId);
    else selectMedicine(line.medicineId);
    const row = document.querySelector(line.inventoryType === 'SUPPLY'
      ? `#selectedSupplyList [data-supply-id="${line.medicineId}"]`
      : `#selectedMedicineList [data-medicine-id="${line.medicineId}"]`);
    if (!row) continue;
    if (line.route && row.children[1]) row.children[1].value = line.route;
    if (row.children[2]) row.children[2].value = Number((line.duration || '').match(/\d+/)?.[0]) || 1;
    const doseFields = [...row.querySelectorAll('input[type="number"]')].slice(1);
    const totalDose = Number((line.dosage || '').match(/[\d.]+/)?.[0]) || Number(line.quantity) || 1;
    const frequency = Number((line.frequency || '').match(/\d+/)?.[0]) || 1;
    doseFields.forEach((field, index) => { field.value = index < Math.min(frequency, doseFields.length) ? String(totalDose / Math.min(frequency, doseFields.length)) : '0'; });
    updateScheduleRow(row);
    if (line.instruction) row.querySelector('[data-instruction]').value = line.instruction;
  }
}

// Lab Section Logic
function loadLabPatient(id, name, service) {
  const nameEl = document.getElementById('labPatientName');
  if (nameEl) nameEl.value = name;

  const serviceEl = document.getElementById('labSelectedService');
  if (serviceEl) serviceEl.innerText = service;

  document.querySelectorAll('#labQueueList .list-group-item').forEach((el) => el.classList.remove('active'));
  if (window.event && window.event.currentTarget) {
    window.event.currentTarget.classList.add('active');
  }
}

function saveLabResult() {
  showToast('Đã lưu & gửi trả kết quả Cận Lâm Sàng cho Bác sĩ!');
}

let pendingBillingItems = [];
let selectedBillingItem = null;
let pendingDispenseItems = [];
let selectedDispenseItem = null;
let selectedDispenseStockAvailable = false;

function workflowMoney(value) {
  return `${Number(value || 0).toLocaleString('vi-VN')} đ`;
}

function workflowMessage(id, text, isError = false) {
  const message = document.getElementById(id);
  if (!message) return;
  message.textContent = text;
  message.classList.toggle('d-none', !text);
  message.classList.toggle('alert-danger', isError);
  message.classList.toggle('alert-info', !isError);
}

function workflowTableRow(body, values) {
  const row = document.createElement('tr');
  values.forEach((value, index) => {
    const cell = document.createElement('td');
    cell.textContent = value;
    if (index > 0) cell.classList.add('text-end');
    row.appendChild(cell);
  });
  body.appendChild(row);
}

async function loadWorkflowBilling() {
  const queue = document.getElementById('cashierQueueList');
  if (!queue) return;
  try {
    pendingBillingItems = await apiRequest('/workflow/billing');
    queue.replaceChildren();
    for (const item of pendingBillingItems) {
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'list-group-item list-group-item-action p-3 text-start';
      const heading = document.createElement('div');
      heading.className = 'd-flex w-100 justify-content-between';
      const name = document.createElement('strong');
      name.textContent = `${item.patientCode} - ${item.patientName}`;
      const status = document.createElement('span');
      status.className = 'badge bg-warning text-dark';
      status.textContent = 'Chờ thu';
      heading.append(name, status);
      const total = document.createElement('div');
      total.className = 'small mt-2';
      total.textContent = `Tổng phải thu: ${workflowMoney(item.totalAmount)}`;
      button.append(heading, total);
      button.addEventListener('click', () => selectBillingItem(item));
      queue.appendChild(button);
    }
    if (!pendingBillingItems.length) {
      const empty = document.createElement('div');
      empty.className = 'list-group-item text-muted py-4 text-center';
      empty.textContent = 'Không có lượt khám chờ thanh toán.';
      queue.appendChild(empty);
      selectBillingItem(null);
    } else {
      selectBillingItem(pendingBillingItems.find((item) => item.visitId === selectedBillingItem?.visitId) || pendingBillingItems[0]);
    }
    workflowMessage('billingMessage', '');
  } catch (error) {
    workflowMessage('billingMessage', error.message, true);
  }
}

function selectBillingItem(item) {
  selectedBillingItem = item;
  document.querySelectorAll('#cashierQueueList button').forEach((button, index) => {
    button.classList.toggle('active', pendingBillingItems[index]?.visitId === item?.visitId);
  });
  const code = item?.patientCode || '';
  document.getElementById('billPatientCode').value = code;
  document.getElementById('billPatientCodeLabel').textContent = code || '—';
  document.getElementById('billPatientName').textContent = item?.patientName || 'Chọn người bệnh';
  document.getElementById('billVisitCode').textContent = item?.visitCode || '—';
  document.getElementById('billDate').textContent = item?.visitDate || '—';
  let cashier = '—';
  try {
    cashier = JSON.parse(sessionStorage.getItem('hisUser') || '{}').fullName || cashier;
  } catch (error) {
    /* Ignore invalid cached user data. */
  }
  document.getElementById('billCashier').textContent = cashier;
  document.getElementById('billGrandTotal').textContent = `${workflowMoney(item?.totalAmount)} VNĐ`;
  const body = document.getElementById('billItemsBody');
  body.replaceChildren();
  (item?.lines || []).forEach((line, index) =>
    workflowTableRow(body, [`${index + 1}. ${line.description}`, `${line.quantity} ${line.unit || ''}`.trim(), workflowMoney(line.unitPrice), workflowMoney(line.totalPrice)])
  );
  if (item && !item.lines.length) {
    const empty = document.createElement('tr');
    const cell = document.createElement('td');
    cell.colSpan = 4;
    cell.className = 'text-center text-muted';
    cell.textContent = 'Lượt khám này chưa có thuốc được kê.';
    empty.appendChild(cell);
    body.appendChild(empty);
  }
  document.getElementById('confirmPaymentButton').disabled = !item || Number(item.totalAmount) <= 0;
}

function toggleQrModal(show) {
  const qrBlock = document.getElementById('qrCodeBlock');
  if (!qrBlock) return;
  if (show) {
    qrBlock.classList.remove('d-none');
  } else {
    qrBlock.classList.add('d-none');
  }
}

function openPrintModal() {
  if (!selectedBillingItem) return;
  document.getElementById('invPtName').textContent = selectedBillingItem.patientName;
  document.getElementById('invPtCode').textContent = selectedBillingItem.patientCode;
  document.getElementById('invVisitCode').textContent = selectedBillingItem.visitCode;
  document.getElementById('invDate').textContent = new Date().toLocaleDateString('vi-VN');
  document.getElementById('invPtDob').textContent = selectedBillingItem.dateOfBirth || '—';
  document.getElementById('invPtGender').textContent = ({ MALE: 'Nam', FEMALE: 'Nữ', OTHER: 'Khác' })[selectedBillingItem.gender] || '—';
  document.getElementById('invPtAddress').textContent = selectedBillingItem.address || '—';
  document.getElementById('invRoom').textContent = selectedBillingItem.roomName || '—';
  document.getElementById('invDiagnosis').textContent = selectedBillingItem.diagnosis || '—';
  document.getElementById('invCashier').textContent = document.getElementById('billCashier').textContent;
  document.getElementById('invTotalAmount').textContent = `${workflowMoney(selectedBillingItem.totalAmount)} VNĐ`;
  document.getElementById('invPatientShare').textContent = `${workflowMoney(selectedBillingItem.totalAmount)} VNĐ`;
  const body = document.getElementById('invoiceItemsBody');
  body.replaceChildren();
  selectedBillingItem.lines.forEach((line, index) => {
    const row = document.createElement('tr');
    const amount = workflowMoney(line.totalPrice);
    [`${line.statementCategory || '12. Dịch vụ khác'}: ${line.description}`,
      line.unit || '—', line.quantity, workflowMoney(line.unitPrice), '0 đ', '100%', amount,
      '0%', '0 đ', '0 đ', '0 đ', '0 đ', amount].forEach((value, column) => {
      const cell = document.createElement('td');
      cell.textContent = value;
      if (column >= 2) cell.classList.add('text-end');
      row.appendChild(cell);
    });
    body.appendChild(row);
  });
  const modalElement = document.getElementById('printInvoiceModal');
  if (!modalElement) return;
  const modal = new bootstrap.Modal(modalElement);
  modal.show();
}

async function confirmPayment() {
  if (!selectedBillingItem) return;
  const button = document.getElementById('confirmPaymentButton');
  button.disabled = true;
  try {
    const method = 'CASH';
    const receipt = await apiRequest(`/workflow/billing/${selectedBillingItem.visitId}/pay`, {
      method: 'POST',
      body: JSON.stringify({ method }),
    });
    showToast(`Đã thanh toán ${workflowMoney(receipt.totalAmount)}. Mã hóa đơn: ${receipt.invoiceCode}`);
    selectedBillingItem = null;
    await loadWorkflowBilling();
  } catch (error) {
    workflowMessage('billingMessage', error.message, true);
  } finally {
    if (selectedBillingItem) button.disabled = false;
  }
}

async function loadWorkflowDispensing() {
  const queue = document.getElementById('pharmacyQueueList');
  if (!queue) return;
  try {
    pendingDispenseItems = await apiRequest('/workflow/dispensing');
    queue.replaceChildren();
    for (const item of pendingDispenseItems) {
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'list-group-item list-group-item-action p-3 text-start';
      const heading = document.createElement('div');
      heading.className = 'd-flex w-100 justify-content-between';
      const name = document.createElement('strong');
      name.textContent = `${item.patientCode} - ${item.patientName}`;
      const status = document.createElement('span');
      status.className = 'badge bg-success';
      status.textContent = 'Đã thanh toán';
      heading.append(name, status);
      const details = document.createElement('div');
      details.className = 'small mt-2';
      details.textContent = `${item.prescriptionCode} · ${item.lines.length} loại thuốc`;
      button.append(heading, details);
      button.addEventListener('click', () => selectDispenseItem(item));
      queue.appendChild(button);
    }
    if (!pendingDispenseItems.length) {
      const empty = document.createElement('div');
      empty.className = 'list-group-item text-muted py-4 text-center';
      empty.textContent = 'Không có đơn thuốc đã thanh toán chờ cấp.';
      queue.appendChild(empty);
      selectDispenseItem(null);
    } else {
      selectDispenseItem(pendingDispenseItems.find((item) => item.prescriptionId === selectedDispenseItem?.prescriptionId) || pendingDispenseItems[0]);
    }
    workflowMessage('dispensingMessage', '');
  } catch (error) {
    workflowMessage('dispensingMessage', error.message, true);
  }
}

function selectDispenseItem(item) {
  selectedDispenseItem = item;
  document.querySelectorAll('#pharmacyQueueList button').forEach((button, index) => {
    button.classList.toggle('active', pendingDispenseItems[index]?.prescriptionId === item?.prescriptionId);
  });
  document.getElementById('pharmaPatientCode').value = item?.patientCode || '';
  document.getElementById('pharmaPatientName').textContent = item?.patientName || 'Chọn đơn thuốc đã thanh toán';
  document.getElementById('pharmacyPatientBadge').textContent = item ? `Mã đơn: ${item.prescriptionCode}` : 'Chưa chọn đơn';
  document.getElementById('pharmaDiagnosis').textContent = item?.diagnosis || '—';
  const body = document.getElementById('pharmacyItemsBody');
  body.replaceChildren();
  let enoughStock = Boolean(item?.lines.length);
  (item?.lines || []).forEach((line, index) => {
    const row = document.createElement('tr');
    const strength = line.strength && !line.medicineName.toLocaleLowerCase('vi').includes(line.strength.toLocaleLowerCase('vi')) ? ` ${line.strength}` : '';
    const cells = [String(index + 1), `${line.medicineName}${strength}${line.instruction ? `\n${line.instruction}` : ''}`, String(line.quantity), line.unit || '—', String(line.stock ?? 0)];
    cells.forEach((value, column) => {
      const cell = document.createElement('td');
      cell.textContent = value;
      if (column === 1) cell.style.whiteSpace = 'pre-line';
      if (column === 2 || column === 4) cell.className = 'text-center';
      row.appendChild(cell);
    });
    const statusCell = document.createElement('td');
    statusCell.className = 'text-center';
    const badge = document.createElement('span');
    const inStock = Number(line.stock) >= Number(line.quantity);
    enoughStock &&= inStock;
    badge.className = `badge ${inStock ? 'bg-success' : 'bg-danger'}`;
    badge.textContent = inStock ? 'Đủ hàng' : 'Thiếu hàng';
    statusCell.appendChild(badge);
    row.appendChild(statusCell);
    body.appendChild(row);
  });
  const checkbox = document.getElementById('checkDispense');
  checkbox.checked = false;
  selectedDispenseStockAvailable = enoughStock;
  document.getElementById('confirmDispenseButton').disabled = !item || !enoughStock || !checkbox.checked;
}

async function confirmDispense() {
  if (!selectedDispenseItem) return;
  if (!document.getElementById('checkDispense').checked) {
    workflowMessage('dispensingMessage', 'Vui lòng xác nhận đã kiểm tra thuốc trước khi cấp phát.', true);
    return;
  }
  const button = document.getElementById('confirmDispenseButton');
  button.disabled = true;
  try {
    await apiRequest(`/workflow/dispensing/${selectedDispenseItem.prescriptionId}/confirm`, { method: 'POST' });
    showToast(`Đã cấp thuốc cho ${selectedDispenseItem.patientName}.`);
    selectedDispenseItem = null;
    await loadWorkflowDispensing();
  } catch (error) {
    workflowMessage('dispensingMessage', error.message, true);
  } finally {
    if (selectedDispenseItem) button.disabled = false;
  }
}

// Helper Toast Notification
function showToast(msg) {
  const toastEl = document.getElementById('liveToast');
  const toastMessage = document.getElementById('toastMessage');
  if (!toastEl || !toastMessage) return;
  toastMessage.innerText = msg;
  const toast = new bootstrap.Toast(toastEl);
  toast.show();
}

const API_BASE = '/api';

function getAuthHeader() {
  return {};
}

async function apiRequest(path, options = {}) {
  const headers = { Accept: 'application/json', ...getAuthHeader(), ...(options.headers || {}) };
  if (options.body && !headers['Content-Type']) headers['Content-Type'] = 'application/json';
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
    credentials: 'same-origin',
  });
  if (response.status === 401) {
    sessionStorage.removeItem('hisCredentials');
    if (window.location.pathname !== '/login' && window.location.pathname !== '/') window.location.href = '/login';
    throw new Error('Phiên đăng nhập không hợp lệ');
  }
  if (response.status === 403) {
    throw new Error('Tài khoản không có quyền thực hiện thao tác này');
  }
  const payload = await response.json().catch(() => null);
  if (!response.ok) throw new Error(payload?.error || 'Không thể thực hiện yêu cầu');
  return payload;
}

function mapGender(value) {
  return value === 'Nữ' ? 'FEMALE' : value === 'Khác' ? 'OTHER' : 'MALE';
}

function parseDate(value) {
  if (!value) return null;
  if (/^\d{4}$/.test(value)) return `${value}-01-01`;
  const dateMatch = value.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
  if (dateMatch) {
    return `${dateMatch[3]}-${dateMatch[2].padStart(2, '0')}-${dateMatch[1].padStart(2, '0')}`;
  }
  return value;
}

async function submitLogin(event) {
  event.preventDefault();
  const form = document.getElementById('loginForm');
  if (typeof HisValidation !== 'undefined' && !HisValidation.validate(form)) {
    return;
  }
  const username = document.getElementById('username').value.trim();
  const password = document.getElementById('password').value;
  const message = document.getElementById('loginMessage');
  try {
    const result = await apiRequest('/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    });
    sessionStorage.setItem('hisUser', JSON.stringify(result));
    const destination = '/rooms';
    window.location.href = destination;
  } catch (error) {
    const isBadCredentials = error.message === 'Invalid username or password' || error.message.includes('401');
    const msg = isBadCredentials ? 'Tên đăng nhập hoặc mật khẩu không chính xác.' : error.message;
    message.textContent = msg;
    message.className = 'text-center text-danger small mt-3';
    if (typeof HisValidation !== 'undefined') {
      HisValidation.mark(document.getElementById('username'), 'Kiểm tra lại tên đăng nhập');
      HisValidation.mark(document.getElementById('password'), 'Kiểm tra lại mật khẩu');
    }
  }
}

async function loadCurrentUser() {
  const userNameElement = document.getElementById('currentUserName');
  if (!userNameElement) return;

  const storedUser = sessionStorage.getItem('hisUser');
  if (storedUser) {
    try {
      const user = JSON.parse(storedUser);
      userNameElement.textContent = user.fullName || user.username || 'Người dùng';
    } catch (error) {
      sessionStorage.removeItem('hisUser');
    }
  }

  try {
    const response = await fetch('/api/auth/me', {
      headers: { Accept: 'application/json' },
      credentials: 'same-origin',
    });
    if (!response.ok) return;
    const user = await response.json();
    sessionStorage.setItem('hisUser', JSON.stringify(user));
    userNameElement.textContent = user.fullName || user.username || 'Người dùng';
  } catch (error) {
    if (!storedUser) {
      userNameElement.textContent = 'Chưa đăng nhập';
    }
  }
}

async function logout() {
  try {
    await fetch('/api/auth/logout', {
      method: 'POST',
      credentials: 'same-origin',
    });
  } finally {
    sessionStorage.removeItem('hisUser');
    sessionStorage.removeItem('hisCredentials');
    localStorage.removeItem('hisRoomSelection');
    window.location.href = '/login';
  }
}

async function loadPatientsFromApi() {
  const patients = await apiRequest('/patients');
  patientRecords = Object.fromEntries(patients.map((patient) => [patient.patientCode, patient]));
  renderDailyReceptionQueue();
}

function renderDailyReceptionQueue() {
  const flow = getPatientFlow();
  const today = clinicDate();
  const patients = Object.values(patientRecords).filter((patient) =>
    (flowDate(flow[patient.patientCode]) || clinicDate(patient.createdAt || '')) === today
  );
  queueData = patients.map((patient, index) => ({
    stt: index + 1,
    id: patient.patientCode,
    name: patient.fullName,
    genderYear: `${patient.gender || 'OTHER'} / ${patient.dateOfBirth || '-'}`,
    room: flow[patient.patientCode]?.room || 'Chưa phân phòng',
    status: flow[patient.patientCode]?.status || (patient.status === 'ACTIVE' ? 'waiting' : 'done'),
  }));
  renderQueueTable();
}

async function submitInlinePatient() {
  const form = document.getElementById('inlineReceptionForm');
  if (typeof HisValidation !== 'undefined' && !HisValidation.validate(form)) {
    return;
  }
  const name = document.getElementById('recName')?.value.trim();
  const phone = document.getElementById('recPhone')?.value.trim();
  if (!name || !phone) return alert('Vui lòng nhập Họ tên và Số điện thoại!');
  const patientCode = document.getElementById('recPatientId')?.value.trim() || `BN${Date.now().toString().slice(-8)}`;
  const existing = Boolean(document.getElementById('recPatientId')?.value.trim());
  const flow = getFlowForPatient(patientCode);
  if (existing && flow && flow.status !== 'waiting') {
    alert('Không thể sửa bệnh nhân vì phòng khám đã bắt đầu xử lý hồ sơ.');
    return;
  }
  try {
    const endpoint = existing && patientRecords[patientCode]?.id ? `/patients/${patientRecords[patientCode].id}` : '/patients';
    await apiRequest(endpoint, {
      method: existing ? 'PUT' : 'POST',
      ...(existing
        ? {
            body: JSON.stringify({
              ...(patientRecords[patientCode] || {}),
              patientCode,
              fullName: name,
              phone,
              dateOfBirth: parseDate(document.getElementById('recDob')?.value.trim()),
              gender: mapGender(document.getElementById('recGender')?.value),
              identityNumber: document.getElementById('recIdentity')?.value.trim(),
              address: document.getElementById('recAddress')?.value.trim(),
              status: 'ACTIVE',
            }),
          }
        : {}),
      ...(!existing
        ? {
            body: JSON.stringify({
              patientCode,
              fullName: name,
              phone,
              dateOfBirth: parseDate(document.getElementById('recDob')?.value.trim()),
              gender: mapGender(document.getElementById('recGender')?.value),
              identityNumber: document.getElementById('recIdentity')?.value.trim(),
              address: document.getElementById('recAddress')?.value.trim(),
              status: 'ACTIVE',
            }),
          }
        : {}),
    });
    const updatedFlow = updatePatientFlow({ id: patientCode, name }, flow?.status || 'waiting', document.getElementById('recRoom')?.value);
    updatedFlow.reason = document.getElementById('recReason')?.value.trim() || '';
    const allFlows = getPatientFlow();
    allFlows[patientCode] = updatedFlow;
    savePatientFlow(allFlows);
    clearReceptionForm();
    await loadPatientsFromApi();
    showToast(existing ? `Đã cập nhật thông tin bệnh nhân ${name}` : `Đã tạo hồ sơ bệnh nhân ${name}`);
  } catch (error) {
    console.error('Không thể tiếp nhận bệnh nhân', error);
    if (typeof HisValidation !== 'undefined' && error.fieldErrors) {
      HisValidation.serverErrors(error, {
        fullName: 'recName',
        phone: 'recPhone',
        dateOfBirth: 'recDob',
        gender: 'recGender',
        identityNumber: 'recIdentity',
        address: 'recAddress'
      });
    }
    alert(`Không thể tiếp nhận bệnh nhân: ${error.message}`);
  }
}

async function loadRoomsFromApi() {
  const rooms = await apiRequest('/examination-rooms');
  const tbody = document.getElementById('roomTableBody');
  if (!tbody) return;
  tbody.innerHTML = rooms
    .map(
      (room) => `
    <tr><td>${room.code}</td><td>${room.name}</td><td>${room.department?.name || '-'}</td>
    <td>${room.floor || '-'}</td><td><span class="badge ${room.status === 'ACTIVE' ? 'bg-success' : 'bg-secondary'}">${room.status}</span></td>
    <td><button class="btn btn-sm btn-outline-secondary" onclick="editRoom(${room.id})">Sửa</button></td></tr>`
    )
    .join('');
  const count = document.getElementById('roomCount');
  if (count) count.textContent = `${rooms.length} phòng`;
}

async function loadDepartmentsForRoomForm() {
  const select = document.getElementById('roomDepartment');
  if (!select) return;
  const departments = await apiRequest('/departments');
  select.innerHTML = departments.map((department) => `<option value="${department.id}">${department.name}</option>`).join('');
}

async function addRoom() {
  const code = document.getElementById('roomCode')?.value.trim();
  const name = document.getElementById('roomName')?.value.trim();
  const departmentId = Number(document.getElementById('roomDepartment')?.value);
  const floor = document.getElementById('roomFloor')?.value.trim();
  if (!code || !name || !departmentId) return alert('Vui lòng nhập đủ thông tin phòng và khoa.');
  try {
    await apiRequest('/examination-rooms', {
      method: 'POST',
      body: JSON.stringify({ code, name, floor, status: 'ACTIVE', department: { id: departmentId } }),
    });
    document.getElementById('roomForm').reset();
    await loadRoomsFromApi();
    showToast('Đã tạo phòng khám');
  } catch (error) {
    alert(error.message);
  }
}

function setupKeyboardNavigation() {
  document.addEventListener('keydown', (event) => {
    if (event.defaultPrevented || event.key !== 'Enter' || event.altKey || event.ctrlKey || event.metaKey || event.shiftKey) return;
    const current = event.target;
    if (!(current instanceof HTMLElement) || current.matches('textarea, button, input[type="hidden"], input[type="checkbox"], input[type="radio"], input[type="file"], [readonly], [disabled], [data-enter-nav="off"]')) return;

    const form = current.closest('form');
    const scope = form || current.closest('main') || document.body;
    const fields = [...scope.querySelectorAll('input, select, textarea')].filter((field) => {
      if (field.matches('textarea, input[type="hidden"], input[type="checkbox"], input[type="radio"], input[type="file"], [readonly], [disabled], [data-enter-nav="off"]')) return false;
      if (field.offsetParent === null && field !== current) return false;
      return true;
    });
    const currentIndex = fields.indexOf(current);
    if (currentIndex < 0) return;
    event.preventDefault();
    const next = fields[currentIndex + 1];
    if (next) {
      next.focus();
      if (typeof next.select === 'function' && next.type === 'search') next.select();
    } else if (form) {
      form.requestSubmit();
    }
  });
}

document.addEventListener('DOMContentLoaded', async () => {
  setupKeyboardNavigation();
  document.querySelectorAll('.navbar-nav .nav-link').forEach((link) => {
    const path = window.location.pathname === '/medical-examination' ? '/examination' : window.location.pathname;
    link.classList.toggle('active', link.getAttribute('href') === path);
  });
  await loadCurrentUser();
  const loginForm = document.getElementById('loginForm');
  if (loginForm) loginForm.addEventListener('submit', submitLogin);
  if (document.getElementById('receptionTableBody') && sessionStorage.getItem('hisUser')) {
    try {
      await loadReceptionRoomOptions();
      await loadPatientsFromApi();
    } catch (error) {
      console.warn(error.message);
    }
  }
  if (document.getElementById('roomTableBody') && sessionStorage.getItem('hisUser')) {
    try {
      await loadDepartmentsForRoomForm();
      await loadRoomsFromApi();
    } catch (error) {
      console.warn(error.message);
    }
  }
  const examSearch = document.getElementById('examSearch');
  if (examSearch) examSearch.addEventListener('input', filterExaminationQueue);
  ['examFrom', 'examTo', 'examStatusFilter'].forEach((id) => document.getElementById(id)?.addEventListener('change', filterExaminationQueue));
  if (document.getElementById('cashierQueueList') && sessionStorage.getItem('hisUser')) {
    await loadWorkflowBilling();
  }
  if (document.getElementById('pharmacyQueueList') && sessionStorage.getItem('hisUser')) {
    document.getElementById('checkDispense').addEventListener('change', (event) => {
      document.getElementById('confirmDispenseButton').disabled = !selectedDispenseItem || !selectedDispenseStockAvailable || !event.target.checked;
    });
    await loadWorkflowDispensing();
  }
});

async function loadReceptionRoomOptions() {
  const rooms = (await apiRequest('/examination-rooms')).filter((room) => room.status === 'ACTIVE');
  for (const id of ['recRoom', 'newPtRoom']) {
    const select = document.getElementById(id);
    if (!select) continue;
    const previous = select.value;
    select.replaceChildren(new Option('-- Chọn phòng khám --', ''));
    rooms.forEach((room) => select.add(new Option(room.name, room.name)));
    if (rooms.some((room) => room.name === previous)) select.value = previous;
  }
}
