(function () {
  const el = (id) => document.getElementById(id);
  const money = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 });
  const number = new Intl.NumberFormat('vi-VN');
  const escapeHtml = (value) => String(value ?? '').replace(/[&<>"']/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char]));
  const params = () => {
    const query = new URLSearchParams();
    [['from', 'reportFrom'], ['to', 'reportTo'], ['status', 'reportStatus'], ['paymentType', 'reportPayment'], ['expiryMonths', 'reportExpiryMonths'], ['stockThreshold', 'reportStockThreshold']].forEach(([key, id]) => {
      const value = el(id).value;
      if (value !== '') query.set(key, value);
    });
    return query;
  };
  const alertLabels = (row) => [row.lowStock ? 'Sắp hết hàng' : '', row.expiringSoon ? 'Sắp hết hạn' : ''].filter(Boolean);
  const renderEmpty = (id, colspan, message) => { el(id).innerHTML = `<tr><td colspan="${colspan}" class="text-center text-muted py-4">${message}</td></tr>`; };

  const render = (data) => {
    el('reportRevenuePeriod').textContent = `(${data.from} - ${data.to})`;
    el('reportRevenueTotal').textContent = money.format(data.revenue.total || 0);
    el('reportPaidInvoices').textContent = number.format(data.revenue.paidInvoices || 0);
    el('reportTotalVisits').textContent = number.format(data.activity.totalVisits || 0);
    el('reportNewPatients').textContent = number.format(data.activity.newPatients || 0);
    el('reportFollowUps').textContent = number.format(data.activity.followUps || 0);
    el('reportCompleted').textContent = number.format(data.activity.completedVisits || 0);
    el('reportStockSummary').textContent = `Nhập: ${number.format(data.pharmacy.imported)} · Xuất: ${number.format(data.pharmacy.exported)} · Tồn: ${number.format(data.pharmacy.stock)}`;

    const inventory = data.pharmacy.inventory || [];
    el('reportInventoryBody').innerHTML = inventory.length ? inventory.map((row) => {
      const alerts = alertLabels(row);
      return `<tr><td class="ps-3">${escapeHtml(row.code)}</td><td class="fw-semibold">${escapeHtml(row.name)}<div class="small text-muted">${escapeHtml(row.unit || '—')}</div></td><td class="text-end">${number.format(row.imported)}</td><td class="text-end">${number.format(row.exported)}</td><td class="text-end ${row.lowStock ? 'text-danger fw-bold' : ''}">${number.format(row.stock)}</td><td class="text-end">${number.format(row.threshold)}</td><td>${escapeHtml(row.nearestExpiry || '—')}</td><td>${alerts.map((label) => `<span class="badge ${label === 'Sắp hết hàng' ? 'bg-danger' : 'bg-warning text-dark'} me-1">${label}</span>`).join('') || '<span class="text-muted">Bình thường</span>'}</td></tr>`;
    }).join('') : '<tr><td colspan="8" class="text-center text-muted py-4">Chưa có dữ liệu kho thuốc.</td></tr>';

    const expiring = data.pharmacy.expiringSoon || [];
    el('reportExpiringBody').innerHTML = expiring.length ? expiring.map((row) => `<tr><td class="ps-3">${escapeHtml(row.name)}<div class="small text-muted">${escapeHtml(row.code)}</div></td><td class="text-danger fw-semibold">${escapeHtml(row.nearestExpiry)}</td><td class="text-end pe-3">${number.format(row.stock)}</td></tr>`).join('') : '<tr><td colspan="3" class="text-center text-muted py-4">Không có thuốc sắp hết hạn.</td></tr>';
    const lowStock = data.pharmacy.lowStock || [];
    el('reportLowStockBody').innerHTML = lowStock.length ? lowStock.map((row) => `<tr><td class="ps-3">${escapeHtml(row.name)}<div class="small text-muted">${escapeHtml(row.code)}</div></td><td class="text-end text-danger fw-bold">${number.format(row.stock)}</td><td class="text-end pe-3">${number.format(row.threshold)}</td></tr>`).join('') : '<tr><td colspan="3" class="text-center text-muted py-4">Không có thuốc dưới ngưỡng tồn.</td></tr>';
  };

  const load = async () => {
    const box = el('reportMessage');
    box.className = 'alert d-none';
    try { render(await apiRequest(`/reports?${params()}`)); }
    catch (error) { box.textContent = error.message; box.className = 'alert alert-danger'; }
  };
  el('reportFilter').addEventListener('submit', (event) => { event.preventDefault(); load(); });
  el('exportReport').addEventListener('click', () => { window.location.href = `/api/reports/export?${params()}&format=${encodeURIComponent(el('reportFormat').value)}`; });
  load();
})();
