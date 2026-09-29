(() => {
  const el = (id) => document.getElementById(id);
  const form = el('staffForm');
  let employees = [];
  let currentUsername = '';
  let editingId = null;
  let passwordEmployee = null;
  let busy = false;
  const permissions = {
    ADMIN: 'Admin: quản lý nhân viên, phân quyền và truy cập tất cả chức năng.',
    DOCTOR: 'Bác sĩ: thiết lập phòng, khám bệnh, kê đơn và xem lịch sử khám bệnh.',
    RECEPTION: 'Lễ tân: thiết lập phòng, tiếp nhận và xem lịch sử khám bệnh.',
    PHARMACIST: 'Dược sĩ: thiết lập phòng, cấp thuốc, thanh toán và xem lịch sử khám bệnh.'
  };

  function message(text, error = false) {
    el('staffMessage').textContent = text;
    el('staffMessage').className = `alert alert-${error ? 'danger' : 'success'}`;
  }

  async function request(path, options = {}) {
    return apiRequest(`/employees${path}`, {
      ...options,
      headers: { [el('staffCsrfHeader').value]: el('staffCsrfToken').value }
    });
  }

  const normalized = (value) => String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase();

  function render() {
    const query = normalized(el('staffSearch').value.trim());
    const role = el('staffRoleFilter').value;
    const status = el('staffStatusFilter').value;
    const visible = employees.filter((staff) => (!role || staff.roleCode === role)
      && (!status || staff.status === status)
      && normalized([staff.fullName, staff.employeeCode, staff.username, staff.phone, staff.email].join(' ')).includes(query));
    const body = el('staffRows');
    body.replaceChildren();
    el('staffCount').textContent = `${visible.length} / ${employees.length} nhân viên`;
    if (!visible.length) {
      const cell = document.createElement('td');
      cell.colSpan = 7;
      cell.textContent = 'Không có nhân viên phù hợp.';
      cell.className = 'text-center text-muted py-4';
      const row = document.createElement('tr');
      row.append(cell);
      body.append(row);
    }
    visible.forEach((staff) => {
      const row = document.createElement('tr');
      [staff.employeeCode, [staff.fullName, staff.jobTitle].filter(Boolean).join(' — '),
        [staff.phone, staff.email].filter(Boolean).join(' / '), staff.username, staff.roleName,
        staff.status === 'ACTIVE' ? 'Hoạt động' : 'Đã khóa'].forEach((value) => {
        const cell = document.createElement('td');
        cell.textContent = value || '—';
        row.append(cell);
      });
      const actions = document.createElement('td');
      actions.className = 'text-nowrap';
      [['Sửa', 'primary', () => edit(staff)], ['Đổi mật khẩu', 'secondary', () => editPassword(staff)],
        ['Xóa', 'danger', () => remove(staff)]].forEach(([label, color, handler]) => {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = `btn btn-sm btn-outline-${color} me-1`;
        button.textContent = label;
        button.disabled = busy || (label === 'Xóa' && staff.username === currentUsername);
        button.addEventListener('click', handler);
        actions.append(button);
      });
      row.append(actions);
      body.append(row);
    });
  }

  function closeEditors() {
    el('staffEditor').hidden = true;
    el('passwordEditor').hidden = true;
    form.reset();
    el('passwordForm').reset();
    editingId = null;
    passwordEmployee = null;
  }

  function edit(staff = null) {
    if (busy) return;
    closeEditors();
    editingId = staff?.id || null;
    el('staffFormError').textContent = '';
    el('staffEditorTitle').textContent = staff ? `Sửa nhân viên: ${staff.fullName}` : 'Thêm nhân viên và tạo tài khoản';
    for (const field of form.elements) {
      if (field.name) field.value = staff?.[field.name] ?? (field.name === 'status' ? 'ACTIVE' : '');
    }
    el('staffUsername').value = staff?.username || '';
    el('staffUsername').disabled = !!staff;
    const self = staff?.username === currentUsername;
    el('staffRole').disabled = self;
    el('staffStatus').disabled = self;
    el('initialPasswordFields').hidden = !!staff;
    ['staffPassword', 'staffPasswordConfirm'].forEach((id) => {
      el(id).required = !staff;
      el(id).disabled = !!staff;
    });
    roleHelp();
    el('staffEditor').hidden = false;
    el('staffEditorTitle').focus();
    el('staffEditor').scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  function roleHelp() { el('staffRoleHelp').textContent = permissions[el('staffRole').value] || 'Chọn nhóm quyền phù hợp với nhiệm vụ của nhân viên.'; }

  function editPassword(staff) {
    if (busy) return;
    closeEditors();
    passwordEmployee = staff;
    el('passwordError').textContent = '';
    el('passwordTitle').textContent = `Đổi mật khẩu: ${staff.fullName} (${staff.username})`;
    el('passwordEditor').hidden = false;
    el('passwordTitle').focus();
    el('passwordEditor').scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  function validatePassword(password, confirmation) {
    if (password !== confirmation) throw new Error('Mật khẩu nhập lại chưa khớp.');
    if (!password.trim() || password.length < 8 || new TextEncoder().encode(password).length > 72)
      throw new Error('Mật khẩu cần ít nhất 8 ký tự và tối đa 72 byte UTF-8.');
  }

  async function reload() { employees = await request(''); render(); }

  function setBusy(value) {
    busy = value;
    el('addEmployee').disabled = value;
    document.querySelectorAll('#staffForm button, #passwordForm button').forEach((button) => { button.disabled = value; });
    render();
  }

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    if (busy) return;
    el('staffFormError').textContent = '';
    try {
      const profile = {};
      for (const field of form.elements) {
        if (field.name) profile[field.name] = field.value.trim() || null;
      }
      const id = editingId;
      let body = profile;
      if (!id) {
        validatePassword(el('staffPassword').value, el('staffPasswordConfirm').value);
        body = { username: el('staffUsername').value.trim(), password: el('staffPassword').value, profile };
      }
      setBusy(true);
      const saved = await request(id ? `/${id}` : '', { method: id ? 'PUT' : 'POST', body: JSON.stringify(body) });
      employees = employees.filter((staff) => staff.id !== saved.id).concat(saved)
        .sort((a, b) => a.fullName.localeCompare(b.fullName, 'vi'));
      closeEditors();
      message(id ? 'Đã cập nhật thông tin và quyền của nhân viên.' : 'Đã thêm nhân viên và tạo tài khoản.');
    } catch (error) { el('staffFormError').textContent = error.message; }
    finally { setBusy(false); }
  });

  el('passwordForm').addEventListener('submit', async (event) => {
    event.preventDefault();
    if (busy || !passwordEmployee) return;
    el('passwordError').textContent = '';
    try {
      const password = el('newStaffPassword').value;
      validatePassword(password, el('confirmStaffPassword').value);
      const staff = passwordEmployee;
      setBusy(true);
      await request(`/${staff.id}/password`, { method: 'PUT', body: JSON.stringify({ password }) });
      closeEditors();
      if (staff.username === currentUsername) {
        sessionStorage.removeItem('hisUser');
        window.location.href = '/login';
        return;
      }
      message('Đã đổi mật khẩu. Nhân viên cần đăng nhập lại bằng mật khẩu mới.');
    } catch (error) { el('passwordError').textContent = error.message; }
    finally { setBusy(false); }
  });

  async function remove(staff) {
    if (busy || !confirm(`Xóa nhân viên ${staff.fullName} (${staff.username})? Tài khoản sẽ ngừng hoạt động và được ẩn khỏi danh sách. Lịch sử nghiệp vụ được giữ lại.`)) return;
    try {
      setBusy(true);
      await request(`/${staff.id}`, { method: 'DELETE' });
      employees = employees.filter((item) => item.id !== staff.id);
      if (editingId === staff.id || passwordEmployee?.id === staff.id) closeEditors();
      message('Đã xóa nhân viên khỏi danh sách và ngừng tài khoản.');
    } catch (error) { message(error.message, true); }
    finally { setBusy(false); }
  }

  el('addEmployee').addEventListener('click', () => edit());
  el('cancelEmployee').addEventListener('click', closeEditors);
  el('cancelPassword').addEventListener('click', closeEditors);
  el('staffRole').addEventListener('change', roleHelp);
  ['staffSearch', 'staffRoleFilter', 'staffStatusFilter'].forEach((id) => el(id).addEventListener('input', render));

  async function init() {
    try {
      const [roles, me] = await Promise.all([request('/roles'), apiRequest('/auth/me')]);
      currentUsername = me.username;
      el('staffRole').replaceChildren(new Option('-- Chọn nhóm quyền --', ''));
      roles.forEach((role) => {
        el('staffRole').add(new Option(role.name, role.code));
        el('staffRoleFilter').add(new Option(role.name, role.code));
      });
      const today = new Date();
      el('staffBirth').max = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
      await reload();
      el('addEmployee').disabled = false;
    } catch (error) {
      el('staffCount').textContent = 'Chưa tải được danh sách.';
      message(error.message, true);
    }
  }
  init();
})();
