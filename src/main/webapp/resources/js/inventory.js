(function () {
  const el = id => document.getElementById(id);
  const money = value => new Intl.NumberFormat('vi-VN').format(value || 0);
  const message = (text, error = false) => { const box = el('inventoryMessage'); box.textContent = text; box.className = `alert ${error ? 'alert-danger' : 'alert-success'}`; };
  let medicines = [];
  function render() {
    const body = el('inventoryBody');
    if (!medicines.length) { body.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-4">Không có hàng hóa phù hợp.</td></tr>'; return; }
    body.innerHTML = medicines.map(item => { const nearest = item.nearestExpiry || 'Không áp dụng'; const low = item.lowStock; const warehouse = item.inventoryType === 'SUPPLY' ? 'Kho vật tư' : 'Kho thuốc'; return `<tr><td class="ps-3"><span class="badge ${item.inventoryType === 'SUPPLY' ? 'bg-info text-dark' : 'bg-primary'}">${warehouse}</span></td><td class="fw-semibold">${item.code}</td><td>${item.name}${item.strength ? `<div class="small text-muted">${item.strength}</div>` : ''}</td><td>${item.unit}</td><td class="text-end ${low ? 'text-danger fw-bold' : ''}">${Number(item.availableStock).toLocaleString('vi-VN')}</td><td>${nearest}</td><td><span class="badge ${low ? 'bg-warning text-dark' : 'bg-success'}">${low ? 'Sắp hết' : 'Đủ hàng'}</span></td><td class="text-end pe-3"><button class="btn btn-outline-primary btn-sm" data-add="${item.id}"><i class="fa-solid fa-plus"></i> Nhập lô</button></td></tr>`; }).join('');
    body.querySelectorAll('[data-add]').forEach(btn => { btn.onclick = () => { el('batchMedicine').value = btn.dataset.add; bootstrap.Modal.getOrCreateInstance(el('batchModal')).show(); }; });
  }
  async function load() { const params = new URLSearchParams({ keyword: el('inventoryKeyword').value, activeOnly: el('inventoryActive').value }); if (el('inventoryWarehouse').value) params.set('warehouse', el('inventoryWarehouse').value); try { medicines = await apiRequest(`/inventory?${params}`); const select = el('batchMedicine'); select.innerHTML = medicines.map(m => `<option value="${m.id}">${m.code} - ${m.name}</option>`).join(''); render(); } catch (e) { el('inventoryBody').innerHTML = `<tr><td colspan="8" class="text-center text-danger py-4">${e.message}</td></tr>`; } }
  el('inventoryFilter').onsubmit = e => { e.preventDefault(); load(); };
  el('batchForm').onsubmit = async e => { e.preventDefault(); const payload = { medicineId: Number(el('batchMedicine').value), batchNumber: el('batchNumber').value.trim(), quantity: Number(el('batchQuantity').value), manufactureDate: el('batchManufacture').value || null, expiryDate: el('batchExpiry').value || null, unitPrice: el('batchPrice').value ? Number(el('batchPrice').value) : null }; try { await apiRequest('/inventory/batches', { method: 'POST', body: JSON.stringify(payload) }); bootstrap.Modal.getOrCreateInstance(el('batchModal')).hide(); e.target.reset(); message('Đã nhập lô thuốc thành công.'); load(); } catch (error) { message(error.message, true); } };
  load();
})();
