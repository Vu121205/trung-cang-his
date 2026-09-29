(() => {
  const element = (id) => document.getElementById(id);
  let page = 0;
  let totalPages = 0;
  let listRequest = 0;
  let detailRequest = 0;
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
  }
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
  load();
})();
