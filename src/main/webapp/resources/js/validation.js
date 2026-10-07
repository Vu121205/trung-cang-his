/* Shared input validation rules for Trung Cang HIS. */
const HisValidation = (() => {
  function today() {
    const parts = new Intl.DateTimeFormat('en-CA', {
      timeZone: 'Asia/Ho_Chi_Minh',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    }).formatToParts(new Date());
    const part = (type) => parts.find((item) => item.type === type).value;
    return `${part('year')}-${part('month')}-${part('day')}`;
  }

  function birthDate(value, currentDate = today()) {
    const text = String(value ?? '').trim();
    if (!text) return null;
    let year, month, day;
    if (/^\d{4}$/.test(text)) {
      [year, month, day] = [Number(text), 1, 1];
    } else if (/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/.test(text)) {
      const parts = text.split('/').map(Number);
      [day, month, year] = parts;
    } else if (/^(\d{4})-(\d{1,2})-(\d{1,2})$/.test(text)) {
      const parts = text.split('-').map(Number);
      [year, month, day] = parts;
    } else {
      throw new Error('Nhập năm sinh đủ 4 chữ số hoặc ngày sinh dạng DD/MM/YYYY.');
    }
    const date = new Date(Date.UTC(year, month - 1, day));
    if (
      date.getUTCFullYear() !== year ||
      date.getUTCMonth() !== month - 1 ||
      date.getUTCDate() !== day
    ) {
      throw new Error('Ngày sinh không tồn tại. Kiểm tra lại ngày, tháng và năm.');
    }
    const iso = `${String(year).padStart(4, '0')}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    if (year < 1900 || iso > currentDate) {
      throw new Error('Ngày sinh phải từ năm 1900 đến ngày hiện tại.');
    }
    return iso;
  }

  function personName(value) {
    const text = String(value ?? '').trim();
    if (!text) return null;
    if (text.length < 2) {
      throw new Error('Họ và tên cần ít nhất 2 ký tự.');
    }
    if (text.length > 150) {
      throw new Error('Họ và tên tối đa 150 ký tự.');
    }
    if (/\d/.test(text)) {
      throw new Error('Họ và tên không được chứa chữ số.');
    }
    if (!/^[\p{L}\s'.-]+$/u.test(text)) {
      throw new Error('Họ và tên chỉ chứa chữ cái và dấu cách, không chứa ký tự đặc biệt.');
    }
    if (!/\p{L}/u.test(text)) {
      throw new Error('Họ và tên phải chứa ít nhất một chữ cái.');
    }
    return text;
  }

  function phone(value) {
    const text = String(value ?? '').trim();
    if (!text) return null;
    if (text.length > 20 || !/^\+?[0-9() .-]+$/.test(text)) {
      throw new Error('Số điện thoại chỉ được chứa chữ số và các ký tự +, -, (), dấu cách.');
    }
    const digits = text.replace(/[^0-9]/g, '');
    if (digits.length < 8 || digits.length > 15) {
      throw new Error('Số điện thoại cần từ 8 đến 15 chữ số (Ví dụ: 0912345678).');
    }
    if (text.startsWith('0') && digits.length !== 10 && digits.length !== 11) {
      throw new Error('Số điện thoại Việt Nam thông thường gồm 10 chữ số (Ví dụ: 0912345678).');
    }
    return text;
  }

  function identityNumber(value) {
    const text = String(value ?? '').trim();
    if (!text) return null;
    if (!/^[0-9]{9}$|^[0-9]{12}$/.test(text)) {
      throw new Error('Số CCCD/CMND phải gồm đúng 9 hoặc 12 chữ số.');
    }
    return text;
  }

  function email(value) {
    const text = String(value ?? '').trim();
    if (!text) return null;
    if (text.length > 150) {
      throw new Error('Email tối đa 150 ký tự.');
    }
    if (!/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(text)) {
      throw new Error('Email không đúng định dạng (Ví dụ: name@example.com).');
    }
    return text;
  }

  function bloodPressure(value) {
    const text = String(value ?? '').trim();
    if (!text) return null;
    const match = text.match(/^(\d{2,3})\s*\/\s*(\d{2,3})$/);
    if (!match) {
      throw new Error('Huyết áp phải có định dạng Tâm thu/Tâm trương (Ví dụ: 120/80).');
    }
    const sys = Number(match[1]);
    const dia = Number(match[2]);
    if (sys < 50 || sys > 260 || dia < 30 || dia > 160) {
      throw new Error('Huyết áp ngoài khoảng cho phép (Tâm thu: 50–260, Tâm trương: 30–160 mmHg).');
    }
    if (sys <= dia) {
      throw new Error('Huyết áp tâm thu phải lớn hơn huyết áp tâm trương.');
    }
    return { systolic: sys, diastolic: dia };
  }

  function username(value) {
    const text = String(value ?? '').trim();
    if (!text) return null;
    if (text.length < 3 || text.length > 50) {
      throw new Error('Tên đăng nhập cần từ 3 đến 50 ký tự.');
    }
    if (!/^[a-zA-Z0-9._-]+$/.test(text)) {
      throw new Error('Tên đăng nhập chỉ chứa chữ không dấu, số, dấu chấm, gạch dưới hoặc gạch ngang.');
    }
    return text;
  }

  function number(value, { min, max, integer = false, label = 'Giá trị' } = {}) {
    const text = String(value ?? '').trim();
    if (!text) return null;
    const parsed = Number(text);
    if (
      !/^[+-]?\d+(?:\.\d+)?$/.test(text) ||
      !Number.isFinite(parsed) ||
      (integer && !Number.isInteger(parsed)) ||
      (min !== undefined && parsed < min) ||
      (max !== undefined && parsed > max)
    ) {
      const boundsDesc =
        min !== undefined && max !== undefined
          ? `từ ${min} đến ${max}`
          : min !== undefined
            ? `lớn hơn hoặc bằng ${min}`
            : max !== undefined
              ? `nhỏ hơn hoặc bằng ${max}`
              : '';
      throw new Error(`${label} phải là ${integer ? 'số nguyên' : 'số'} ${boundsDesc}.`.trim());
    }
    return parsed;
  }

  function errorFor(field) {
    if (!field || field.disabled || field.readOnly) return '';
    const value = String(field.value ?? '').trim();

    // 1. Kiểm tra trường bắt buộc
    if (field.required && !value) {
      if (field.tagName === 'SELECT') {
        return 'Vui lòng chọn thông tin này.';
      }
      return 'Vui lòng nhập thông tin này.';
    }

    // Nếu không bắt buộc và để trống thì không lỗi định dạng
    if (!value) return '';

    // 2. Độ dài tối đa / tối thiểu theo HTML
    if (field.maxLength > 0 && value.length > field.maxLength) {
      return `Nhập tối đa ${field.maxLength} ký tự.`;
    }
    if (field.minLength > 0 && value.length < field.minLength) {
      return `Nhập ít nhất ${field.minLength} ký tự.`;
    }

    // 3. Quy tắc riêng dựa theo dataset hoặc ID/Type
    const validationType = field.dataset.validation || '';

    // Họ và tên
    if (
      validationType === 'person-name' ||
      field.id === 'recName' ||
      field.id === 'newPtName' ||
      field.id === 'staffFullName'
    ) {
      try {
        personName(value);
      } catch (error) {
        return error.message;
      }
    }

    // Ngày sinh / Năm sinh
    if (
      validationType === 'birth-date' ||
      field.id === 'recDob' ||
      field.id === 'newPtDob'
    ) {
      try {
        birthDate(value);
      } catch (error) {
        return error.message;
      }
    }

    // Ngày sinh dạng input type="date"
    if (field.type === 'date' && field.id === 'staffBirth') {
      try {
        const currentDate = today();
        if (value > currentDate) {
          return 'Ngày sinh không được sau ngày hiện tại.';
        }
        if (value < '1900-01-01') {
          return 'Ngày sinh phải từ năm 1900 trở lại đây.';
        }
      } catch (error) {
        return 'Ngày sinh không hợp lệ.';
      }
    }

    // Số điện thoại
    if (
      field.type === 'tel' ||
      validationType === 'phone' ||
      field.id === 'recPhone' ||
      field.id === 'newPtPhone' ||
      field.id === 'staffPhone'
    ) {
      try {
        phone(value);
      } catch (error) {
        return error.message;
      }
    }

    // Số CCCD / CMND
    if (
      validationType === 'identity' ||
      field.id === 'recIdentity' ||
      field.id === 'newPtIdentity' ||
      field.id === 'staffIdentity'
    ) {
      try {
        identityNumber(value);
      } catch (error) {
        return error.message;
      }
    }

    // Email
    if (field.type === 'email' || validationType === 'email' || field.id === 'staffEmail') {
      try {
        email(value);
      } catch (error) {
        return error.message;
      }
    }

    // Huyết áp
    if (validationType === 'blood-pressure' || field.id === 'docBloodPressure') {
      try {
        bloodPressure(value);
      } catch (error) {
        return error.message;
      }
    }

    // Tên đăng nhập
    if (
      validationType === 'username' ||
      field.id === 'username' ||
      field.id === 'staffUsername'
    ) {
      try {
        username(value);
      } catch (error) {
        return error.message;
      }
    }

    // Mật khẩu xác nhận khớp
    if (field.dataset.matches) {
      const target = document.getElementById(field.dataset.matches);
      if (target && target.value !== field.value) {
        return 'Mật khẩu xác nhận không khớp.';
      }
    }
    if (field.id === 'staffPasswordConfirm') {
      const target = document.getElementById('staffPassword');
      if (target && target.value !== field.value) {
        return 'Mật khẩu xác nhận không khớp.';
      }
    }
    if (field.id === 'confirmStaffPassword') {
      const target = document.getElementById('newStaffPassword');
      if (target && target.value !== field.value) {
        return 'Mật khẩu xác nhận không khớp.';
      }
    }

    // Chỉ số sinh tồn
    if (field.id === 'docPulse') {
      try {
        number(value, { min: 30, max: 250, integer: true, label: 'Mạch' });
      } catch (error) {
        return error.message;
      }
    }
    if (field.id === 'docTemperature') {
      try {
        number(value, { min: 34.0, max: 43.0, label: 'Nhiệt độ' });
      } catch (error) {
        return error.message;
      }
    }
    if (field.id === 'docWeight') {
      try {
        number(value, { min: 0.5, max: 300.0, label: 'Cân nặng' });
      } catch (error) {
        return error.message;
      }
    }

    // Số lượng thuốc kê đơn
    if (field.hasAttribute('data-quantity')) {
      try {
        number(value, { min: 1, max: 10000, integer: true, label: 'Số lượng' });
      } catch (error) {
        return error.message;
      }
    }

    // Khoảng ngày lọc
    if (field.id === 'examTo' || field.id === 'historyTo') {
      const fromId = field.id === 'examTo' ? 'examFrom' : 'historyFrom';
      const fromField = document.getElementById(fromId);
      if (fromField && fromField.value && value && fromField.value > value) {
        return 'Đến ngày không được trước Từ ngày.';
      }
    }

    // 4. HTML5 standard validity
    if (field.validity?.badInput) return 'Dữ liệu nhập không đúng định dạng.';
    if (field.validity?.typeMismatch) return 'Thông tin không đúng định dạng.';
    if (field.validity?.patternMismatch) {
      return field.title || 'Thông tin không đúng định dạng cho phép.';
    }
    if (field.validity?.rangeUnderflow) {
      return `Giá trị tối thiểu là ${field.min}.`;
    }
    if (field.validity?.rangeOverflow) {
      return `Giá trị tối đa là ${field.max}.`;
    }
    if (field.validity?.stepMismatch) {
      return 'Bước nhảy giá trị không hợp lệ.';
    }

    return '';
  }

  function mark(field, message) {
    if (!field) return;
    const isInvalid = Boolean(message);
    field.setCustomValidity(message || '');
    field.classList.toggle('is-invalid', isInvalid);
    field.setAttribute('aria-invalid', String(isInvalid));

    // Tìm feedback hiện có
    let feedbackId = field.id ? `${field.id}Feedback` : null;
    let feedback = feedbackId ? document.getElementById(feedbackId) : null;

    const container = field.closest('.input-group') || field;
    if (!feedback && container.parentElement) {
      const attr = field.id ? `[data-for="${field.id}"]` : field.name ? `[data-for="${field.name}"]` : null;
      if (attr) {
        feedback = container.parentElement.querySelector(`:scope > .invalid-feedback${attr}`);
      }
    }

    if (!feedback && isInvalid) {
      feedback = document.createElement('div');
      if (feedbackId) feedback.id = feedbackId;
      feedback.className = 'invalid-feedback';
      if (field.id || field.name) feedback.dataset.for = field.id || field.name;
      container.insertAdjacentElement('afterend', feedback);
      if (feedbackId) {
        field.setAttribute(
          'aria-describedby',
          [field.getAttribute('aria-describedby'), feedbackId].filter(Boolean).join(' ')
        );
      }
    }

    if (feedback) {
      feedback.textContent = message || '';
      feedback.style.display = isInvalid ? 'block' : 'none';
    }
  }

  function validate(container) {
    if (!container) return true;
    let firstInvalid = null;

    container.querySelectorAll('input, select, textarea').forEach((field) => {
      if (
        field.disabled ||
        field.readOnly ||
        ['hidden', 'submit', 'button', 'reset'].includes(field.type)
      ) {
        return;
      }
      field.setCustomValidity('');
      const message = errorFor(field);
      mark(field, message);
      if (message && !firstInvalid) {
        firstInvalid = field;
      }
    });

    if (firstInvalid) {
      firstInvalid.focus();
      if (typeof firstInvalid.scrollIntoView === 'function') {
        firstInvalid.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
      if (typeof showToast === 'function') {
        showToast('Vui lòng kiểm tra lại: một số thông tin nhập chưa đúng định dạng hoặc còn thiếu!', 'danger');
      }
      return false;
    }
    return true;
  }

  function clear(container) {
    if (!container) return;
    container.querySelectorAll('input, select, textarea').forEach((field) => mark(field, ''));
  }

  function serverErrors(error, fields = {}) {
    let first;
    Object.entries(error.fieldErrors || {}).forEach(([name, message]) => {
      const targetId = fields[name] || fields[name.replace(/^profile\./, '')] || name;
      const field = document.getElementById(targetId) || document.querySelector(`[name="${name}"]`);
      if (field) {
        mark(field, message);
        first ||= field;
      }
    });
    first?.focus();
  }

  if (typeof document !== 'undefined') {
    document.addEventListener('DOMContentLoaded', () => {
      document.querySelectorAll('form').forEach((form) => {
        form.noValidate = true;
        form.addEventListener(
          'submit',
          (event) => {
            if (!validate(form)) {
              event.preventDefault();
              event.stopImmediatePropagation();
            }
          },
          true
        );
        form.addEventListener('reset', () => clear(form));
      });

      // Kiểm tra tức thì khi người dùng sửa lỗi
      document.addEventListener('input', (event) => {
        const target = event.target;
        if (target.matches?.('input:not([type=hidden]), select, textarea')) {
          if (target.classList.contains('is-invalid')) {
            mark(target, errorFor(target));
          }
        }
      });

      // Kiểm tra khi thay đổi lựa chọn (select / date / radio / checkbox)
      document.addEventListener('change', (event) => {
        const target = event.target;
        if (target.matches?.('input:not([type=hidden]), select, textarea')) {
          mark(target, errorFor(target));
        }
      });

      // Kiểm tra khi rời khỏi ô nhập liệu (focusout / blur)
      document.addEventListener('focusout', (event) => {
        const target = event.target;
        if (
          target.matches?.('input:not([type=hidden]), select, textarea') &&
          !target.disabled &&
          !target.readOnly
        ) {
          if (target.value.trim() || target.required) {
            mark(target, errorFor(target));
          }
        }
      });
    });
  }

  return {
    today,
    birthDate,
    personName,
    phone,
    identityNumber,
    email,
    bloodPressure,
    username,
    number,
    errorFor,
    mark,
    validate,
    clear,
    serverErrors,
  };
})();

if (typeof module !== 'undefined') {
  module.exports = HisValidation;
}
