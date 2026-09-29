let queueData = [];
let patientRecords = {};

let icdCatalog = {};
let medicineCatalog = [];

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
  container.classList.toggle('d-none', !keyword);
  if (!keyword) return;
  const matches = medicineCatalog.filter((medicine) => keyword.split(/\s+/).every((term) => `${medicine.name} ${medicine.active} ${medicine.hint}`.toLowerCase().includes(term))).slice(0, 20);
  for (const medicine of matches) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'list-group-item list-group-item-action py-2';
    button.textContent = `${medicine.name} · ${medicine.active} · ${medicine.hint}`;
    button.addEventListener('click', () => selectMedicine(medicine.id));
    container.appendChild(button);
  }
  if (!matches.length) container.textContent = 'Không tìm thấy thuốc phù hợp.';
}

function selectMedicine(id) {
  const medicine = medicineCatalog.find((item) => item.id === id);
  if (!medicine) return;
  const container = document.getElementById('selectedMedicineList');
  if ([...container.children].some((row) => Number(row.dataset.medicineId) === id)) return;
  const row = document.createElement('div');
  row.className = 'input-group input-group-sm mb-2';
  row.dataset.medicineId = id;
  const label = document.createElement('span');
  label.className = 'input-group-text flex-grow-1';
  label.textContent = `${medicine.name} (${medicine.unit})`;
  const quantity = document.createElement('input');
  quantity.type = 'number';
  quantity.min = '1';
  quantity.max = '10000';
  quantity.value = '1';
  quantity.dataset.quantity = '';
  quantity.className = 'form-control';
  quantity.style.maxWidth = '90px';
  quantity.setAttribute('aria-label', 'Số lượng ' + medicine.name);
  const instruction = document.createElement('input');
  instruction.dataset.instruction = '';
  instruction.className = 'form-control';
  instruction.placeholder = 'Cách dùng';
  instruction.maxLength = 2000;
  instruction.setAttribute('aria-label', 'Cách dùng ' + medicine.name);
  const remove = document.createElement('button');
  remove.type = 'button';
  remove.className = 'btn btn-outline-danger';
  remove.textContent = 'Xóa';
  remove.addEventListener('click', () => row.remove());
  row.append(label, quantity, instruction, remove);
  container.appendChild(row);
  document.getElementById('docMedicineSearch').value = '';
  document.getElementById('medicineSuggestions').classList.add('d-none');
}

// Init App
document.addEventListener('DOMContentLoaded', () => {
  renderQueueTable();
});

// Fill form from old patient lookup selection
function fillPatientForm(id, name, phone, year, gender, address, identity) {
  const recPatientId = document.getElementById('recPatientId');
  if (recPatientId) recPatientId.value = id || '';

  const recName = document.getElementById('recName');
  if (recName) recName.value = name || '';

  const recPhone = document.getElementById('recPhone');
  if (recPhone) recPhone.value = phone || '';

  const recDob = document.getElementById('recDob');
  if (recDob) recDob.value = year || '';

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

// Handle direct inline registration submit
function submitInlinePatient() {
  const nameInput = document.getElementById('recName');
  const phoneInput = document.getElementById('recPhone');
  const roomInput = document.getElementById('recRoom');
  const dobInput = document.getElementById('recDob');
  const genderInput = document.getElementById('recGender');
  const patientIdInput = document.getElementById('recPatientId');

  const name = nameInput ? nameInput.value.trim() : '';
  const phone = phoneInput ? phoneInput.value.trim() : '';
  const room = roomInput ? roomInput.value : '';
  const year = dobInput && dobInput.value.trim() ? dobInput.value.trim() : '1995';
  const gender = genderInput ? genderInput.value : 'Nam';
  let patientId = patientIdInput ? patientIdInput.value.trim() : '';

  if (!name || !phone) {
    alert('Vui lòng nhập Họ tên và Số điện thoại!');
    return;
  }

  if (!patientId) {
    patientId = 'BN' + Math.floor(10000 + Math.random() * 90000);
  }

  const nextStt = 100 + queueData.length + 1;
  queueData.unshift({
    stt: nextStt,
    id: patientId,
    name: name,
    genderYear: `${gender} / ${year}`,
    room: room,
    status: 'waiting',
  });

  renderQueueTable();
  clearReceptionForm();
  showToast(`Thành công! Đã cấp số ${nextStt} cho bệnh nhân ${name}`);
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

function submitNewPatient() {
  const nameEl = document.getElementById('newPtName');
  const phoneEl = document.getElementById('newPtPhone');
  const roomEl = document.getElementById('newPtRoom');

  const name = nameEl ? nameEl.value : '';
  const phone = phoneEl ? phoneEl.value : '';
  const room = roomEl ? roomEl.value : '';

  if (!name || !phone) {
    alert('Vui lòng điền đầy đủ Họ tên và Số điện thoại!');
    return;
  }

  const newId = 'BN' + Math.floor(10000 + Math.random() * 90000);
  const nextStt = 100 + queueData.length + 1;

  queueData.unshift({
    stt: nextStt,
    id: newId,
    name: name,
    genderYear: 'Nam / 1995',
    room: room,
    status: 'waiting',
  });
  updatePatientFlow({ id: newId, name }, 'waiting', room || 'Phòng khám số 1');

  renderQueueTable();

  // Close modal
  const modalEl = document.getElementById('newPatientModal');
  if (modalEl) {
    const modal = bootstrap.Modal.getInstance(modalEl);
    if (modal) modal.hide();
  }

  showToast(`Thành công! Cấp số ${nextStt} cho BN ${name}`);
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
  const flow = Object.values(getPatientFlow()).filter((item) => flowDate(item) === today && ['waiting', 'examining'].includes(item.status) && (!room || item.room === room));
  table.replaceChildren();
  flow.forEach((item, index) => {
    const row = document.createElement('tr');
    row.dataset.patientCode = item.id;
    row.dataset.patientStatus = item.status === 'examining' ? 'EXAMINING' : 'WAITING';
    row.dataset.queueDate = flowDate(item);
    for (const value of [index + 1, item.id, item.name, item.room, item.status === 'examining' ? 'Đang khám' : 'Chờ khám']) {
      const cell = document.createElement('td');
      cell.textContent = value;
      row.appendChild(cell);
    }
    const action = document.createElement('td');
    const statusCell = row.children[4];
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'btn btn-sm btn-outline-primary';
    button.textContent = 'Mở hồ sơ';
    action.appendChild(button);
    row.appendChild(action);
    row.addEventListener('click', () => {
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
  if (flow.length && !(preserveSelection && document.getElementById('examPatientCode')?.value)) selectExaminationPatient(flow[0].id);
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
    ['docSymptoms', 'docMedicalHistory', 'docIcdCode', 'docDiagnosis', 'docClinicalNote', 'docAdvice', 'docMedicineSearch'].forEach((key) => {
      document.getElementById(key).value = '';
    });
    document.getElementById('docSymptoms').value = item.reason || '';
    document.getElementById('selectedMedicineList').replaceChildren();
    document.getElementById('icdSuggestion').textContent = '';
    ['icdSuggestions', 'medicineSuggestions'].forEach((key) => document.getElementById(key).classList.add('d-none'));
  }
  code.value = id;
  document.getElementById('docPatientName').textContent = item.name;
  document.getElementById('docPatientMeta').textContent = `Mã BN: ${id} | Đối tượng: Dịch vụ | Phòng: ${item.room}`;
  document.getElementById('examRoomLabel').textContent = item.room;
  document.querySelectorAll('#examinationQueueBody tr').forEach((row) => row.classList.toggle('table-primary', row.dataset.patientCode === id));
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
  document.getElementById('invPayMethod').textContent = document.getElementById('payCash').checked ? 'Tiền mặt' : 'Chuyển khoản ngân hàng';
  document.getElementById('invCashier').textContent = document.getElementById('billCashier').textContent;
  document.getElementById('invTotalAmount').textContent = `${workflowMoney(selectedBillingItem.totalAmount)} VNĐ`;
  const body = document.getElementById('invoiceItemsBody');
  body.replaceChildren();
  selectedBillingItem.lines.forEach((line, index) => {
    const row = document.createElement('tr');
    [index + 1, line.description, `${line.quantity} ${line.unit || ''}`.trim(), workflowMoney(line.unitPrice), workflowMoney(line.totalPrice)].forEach((value, column) => {
      const cell = document.createElement('td');
      cell.textContent = value;
      if (column > 0) cell.classList.toggle('text-end', column > 1);
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
    const method = document.getElementById('payCash').checked ? 'CASH' : 'BANK_TRANSFER';
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
  const year = value.match(/^\d{4}$/);
  return year ? `${value}-01-01` : value;
}

async function submitLogin(event) {
  event.preventDefault();
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
    message.textContent = error.message;
    message.className = 'text-center text-danger small mt-3';
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

document.addEventListener('DOMContentLoaded', async () => {
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
