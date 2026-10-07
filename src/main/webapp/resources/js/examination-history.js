(() => {
  const element = (id) => document.getElementById(id);
  let page = 0;
  let totalPages = 0;
  let listRequest = 0;
  let detailRequest = 0;
  let currentPrescriptionId = null;
  const displayDate = (value) => (value ? new Date(value).toLocaleString('vi-VN') : '—');
  function message(value) {
    element('historyMessage').textContent = value;
    element('historyMessage').classList.toggle('d-none', !value);
  }
  function cell(row, value) {
    const td = document.createElement('td');
    td.textContent = value || '—';
    row.appendChild(td);
    return td;
  }
  async function load() {
    const request = ++listRequest;
    const from = element('historyFrom').value;
    const to = element('historyTo').value;
    element('historyPrevious').disabled = true;
    element('historyNext').disabled = true;
    message('');
    if (from && to && from > to) return message('Từ ngày phải trước hoặc bằng đến ngày.');
    const params = new URLSearchParams({ q: element('historyQuery').value.trim(), page, size: 20 });
    if (from) params.set('from', from);
    if (to) params.set('to', to);
    try {
      const result = await apiRequest(`/examination-history?${params}`);
      if (request !== listRequest) return;
      totalPages = result.totalPages;
      const body = element('historyRows');
      body.replaceChildren();
      for (const item of result.items) {
        const row = document.createElement('tr');
        [
          displayDate(item.examinedAt || item.visitDate),
          item.patientCode,
          item.patientName,
          item.phone,
          item.roomName,
          item.doctorName,
          item.result,
          item.status === 'COMPLETED' ? 'Đã khám' : 'Bản nháp',
        ].forEach((value) => cell(row, value));
        const action = cell(row, '');
        action.textContent = '';
        const button = document.createElement('button');
        button.className = 'btn btn-outline-primary btn-sm';
        button.textContent = 'Xem';
        button.addEventListener('click', () => showDetail(item.id));
        action.appendChild(button);
        body.appendChild(row);
      }
      if (!result.items.length) {
        const row = document.createElement('tr');
        const empty = cell(row, 'Chưa có hồ sơ khám phù hợp. Hồ sơ xuất hiện sau khi hoàn tất khám và lưu thành công.');
        empty.colSpan = 9;
        empty.className = 'text-center text-muted py-4';
        body.appendChild(row);
      }
      element('historyCount').textContent = `${result.total} hồ sơ`;
      element('historyPage').textContent = totalPages ? `Trang ${page + 1} / ${totalPages}` : 'Trang 0 / 0';
      element('historyPrevious').disabled = page === 0;
      element('historyNext').disabled = page + 1 >= totalPages;
    } catch (error) {
      if (request === listRequest) message(error.message);
    }
  }
  async function showDetail(id) {
    const request = ++detailRequest;
    message('');
    try {
      const detail = await apiRequest(`/examination-history/${id}`);
      if (request !== detailRequest) return;
      const body = element('historyDetailBody');
      body.replaceChildren();
      currentPrescriptionId = detail.prescriptionId || null;
      element('historyPrintPrescription').classList.toggle('d-none', !currentPrescriptionId);
      element('historyDetailTitle').textContent = `${detail.visit.patientName} — ${detail.visit.visitCode}`;
      const fields = [
        ['Ngày khám', displayDate(detail.visit.examinedAt)],
        ['Phòng / Người khám', `${detail.visit.roomName} / ${detail.visit.doctorName}`],
        [
          'Dấu hiệu sinh tồn',
          detail.vitalSigns
            ? `Huyết áp ${detail.vitalSigns.systolic ?? '—'}/${detail.vitalSigns.diastolic ?? '—'} mmHg · Mạch ${detail.vitalSigns.pulse ?? '—'} lần/phút · Nhiệt độ ${detail.vitalSigns.temperature ?? '—'} °C · Cân nặng ${detail.vitalSigns.weight ?? '—'} kg`
            : 'Chưa ghi nhận',
        ],
        ['Triệu chứng', detail.symptoms],
        ['Tiền sử bệnh', detail.medicalHistory],
        ['Ghi chú / Chỉ định cận lâm sàng', detail.clinicalNote],
        ['Chẩn đoán', detail.diagnoses.map((item) => `${item.code} — ${item.name}`).join('\n') || detail.result],
        ['Dặn dò', detail.advice],
      ];
      for (const [title, value] of fields) {
        const label = document.createElement('strong');
        label.textContent = title;
        const text = document.createElement('p');
        text.style.whiteSpace = 'pre-wrap';
        text.textContent = value || '—';
        body.append(label, text);
      }
      const title = document.createElement('h6');
      title.textContent = 'Đơn thuốc';
      body.appendChild(title);
      const list = document.createElement('ul');
      for (const item of detail.medicines) {
        const line = document.createElement('li');
        line.textContent = `${item.name}: ${item.quantity} ${item.unit || ''}. ${item.instruction || ''}`;
        list.appendChild(line);
      }
      if (!detail.medicines.length) list.textContent = 'Không có thuốc được kê.';
      body.appendChild(list);
      element('historyDetail').classList.remove('d-none');
      element('historyDetail').focus();
    } catch (error) {
      if (request === detailRequest) message(error.message);
    }
  }
  function closeDetail() {
    detailRequest++;
    element('historyDetail').classList.add('d-none');
    currentPrescriptionId = null;
    element('historyPrintPrescription').classList.add('d-none');
  }
  async function printPrescription() {
    if (!currentPrescriptionId) return;
    try {
      const data = await apiRequest(`/prescriptions/${currentPrescriptionId}/print`);
      const lines = data.lines.map((line, index) => `<tr><td>${index + 1}</td><td>${escapeHtml([line.medicineName, line.activeIngredient, line.strength, line.dosageForm].filter(Boolean).join(' · '))}</td><td>${escapeHtml(line.dosage || '—')}</td><td>${escapeHtml(line.frequency || '—')}</td><td>${escapeHtml(line.route || '—')}</td><td>${escapeHtml(line.duration || '—')}</td><td>${escapeHtml(line.quantity)} ${escapeHtml(line.unit || '')}</td><td>${escapeHtml(line.instruction || '—')}</td></tr>`).join('');
      const popup = window.open('', '_blank', 'width=1100,height=800');
      if (!popup) throw new Error('Trình duyệt đã chặn cửa sổ in.');
      popup.document.write(`<!doctype html><html lang="vi"><head><meta charset="utf-8"><title>Đơn thuốc ${escapeHtml(data.prescriptionCode)}</title><style>body{font-family:Arial,sans-serif;margin:28px;color:#111}h2,h3{text-align:center;margin:4px}table{border-collapse:collapse;width:100%;margin-top:18px}th,td{border:1px solid #555;padding:6px;vertical-align:top;font-size:12px}th{background:#eee}.meta{line-height:1.7}.sign{display:flex;justify-content:space-around;text-align:center;margin-top:55px}.sign span{display:block;margin-top:45px}.note{margin-top:18px;white-space:pre-wrap}</style></head><body><h2>${escapeHtml(data.facilityName || 'Phòng khám Trung Cang')}</h2><div style="text-align:center">Địa chỉ: ${escapeHtml(data.facilityAddress || '—')} · Điện thoại: ${escapeHtml(data.facilityPhone || '—')}</div><h3>ĐƠN THUỐC / BẢNG KÊ THUỐC</h3><div class="meta"><b>Mã đơn:</b> ${escapeHtml(data.prescriptionCode)}<br><b>Họ tên:</b> ${escapeHtml(data.patientName)} &nbsp; <b>Mã BN:</b> ${escapeHtml(data.patientCode)}<br><b>Ngày sinh:</b> ${escapeHtml(data.dateOfBirth || '—')} &nbsp; <b>Giới tính:</b> ${escapeHtml(data.gender || '—')} &nbsp; <b>Cân nặng:</b> ${escapeHtml(data.weight || '—')}<br><b>Số định danh:</b> ${escapeHtml(data.identityNumber || '—')} &nbsp; <b>BHYT:</b> ${escapeHtml(data.healthInsuranceNumber || '—')}<br><b>Nơi cư trú:</b> ${escapeHtml(data.address || '—')} &nbsp; <b>Điện thoại:</b> ${escapeHtml(data.phone || '—')}<br><b>Chẩn đoán:</b> ${escapeHtml(data.diagnosis || '—')}</div><table><thead><tr><th>STT</th><th>Thuốc, hoạt chất, hàm lượng/dạng dùng</th><th>Liều/lần</th><th>Số lần/ngày</th><th>Đường dùng</th><th>Số ngày</th><th>Số lượng</th><th>Cách dùng/thời điểm</th></tr></thead><tbody>${lines || '<tr><td colspan="8">Không có thuốc</td></tr>'}</tbody></table><div class="note"><b>Lời dặn:</b> ${escapeHtml(data.advice || '—')}</div><div class="sign"><div>Bệnh nhân/người đại diện<br><span>(Ký, ghi rõ họ tên)</span></div><div>Bác sĩ/Y sĩ khám bệnh<br><span>(Ký, ghi rõ họ tên)<br>${escapeHtml(data.doctorName || '—')}</span></div></div><script>window.onload=()=>window.print()<\/script></body></html>`);
      popup.document.close();
    } catch (error) { message(error.message); }
  }
  function escapeHtml(value) { return String(value ?? '').replace(/[&<>"']/g, (char) => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char])); }
  element('historyFilters').addEventListener('submit', (event) => {
    event.preventDefault();
    page = 0;
    closeDetail();
    load();
  });
  element('historyFilters').addEventListener('reset', () => {
    page = 0;
    closeDetail();
    setTimeout(load, 0);
  });
  element('historyPrevious').addEventListener('click', () => {
    if (page > 0) {
      page--;
      load();
    }
  });
  element('historyNext').addEventListener('click', () => {
    if (page + 1 < totalPages) {
      page++;
      load();
    }
  });
  element('historyClose').addEventListener('click', closeDetail);
  element('historyPrintPrescription').addEventListener('click', printPrescription);
  load();
})();
