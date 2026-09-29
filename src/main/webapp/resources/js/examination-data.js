// Catalog and completed examinations are shared through the database.
let examinationRooms = [];
let savingExamination = false;

async function loadExaminationData() {
  const [diagnoses, medicines, rooms] = await Promise.all([apiRequest('/diagnoses'), apiRequest('/medicines'), apiRequest('/examination-rooms')]);
  icdCatalog = Object.fromEntries(diagnoses.filter((item) => item.status === 'ACTIVE').map((item) => [item.icdCode, item.name]));
  medicineCatalog = medicines
    .filter((item) => item.status === 'ACTIVE')
    .map((item) => ({
      id: item.id,
      name: item.name,
      active: item.activeIngredient || '',
      unit: item.unit,
      hint: [item.strength, item.dosageForm].filter(Boolean).join(' · '),
    }));
  examinationRooms = rooms.filter((room) => room.status === 'ACTIVE');
}

async function persistExamination() {
  if (savingExamination) return;
  const patientCode = document.getElementById('examPatientCode').value;
  const flow = getFlowForPatient(patientCode);
  if (!flow || !['waiting', 'examining'].includes(flow.status)) return alert('Vui lòng chọn bệnh nhân trong hàng chờ.');
  const room = examinationRooms.find((item) => item.name === flow.room);
  if (!room) return alert('Phòng đã chọn không có trong danh mục. Vui lòng phân lại phòng từ trang tiếp nhận.');
  const icdCode = document.getElementById('docIcdCode').value.trim().toUpperCase();
  if (!icdCatalog[icdCode]) return alert('Vui lòng chọn mã ICD trong danh mục.');
  const lines = [...document.querySelectorAll('#selectedMedicineList [data-medicine-id]')];
  const medicines = lines.map((row) => ({
    medicineId: Number(row.dataset.medicineId),
    quantity: Number(row.querySelector('[data-quantity]').value),
    instruction: row.querySelector('[data-instruction]').value.trim(),
  }));
  if (medicines.some((item) => !Number.isInteger(item.quantity) || item.quantity < 1 || item.quantity > 10000 || !item.instruction)) {
    return alert('Mỗi thuốc cần số lượng nguyên từ 1 đến 10000 và cách dùng.');
  }
  if (!flow.examinationRequestId) {
    flow.examinationRequestId = crypto.randomUUID();
    const all = getPatientFlow();
    all[patientCode] = flow;
    savePatientFlow(all);
  }
  const button = document.getElementById('finishExaminationButton');
  const bloodPressure = document.getElementById('docBloodPressure').value.trim();
  const bloodPressureMatch = bloodPressure.match(/^(\d{1,3}(?:\.\d+)?)\s*\/\s*(\d{1,3}(?:\.\d+)?)$/);
  if (bloodPressure && !bloodPressureMatch) return alert('Huyết áp cần có dạng tâm thu/tâm trương, ví dụ 120/80.');
  const optionalNumber = (id) => (document.getElementById(id).value.trim() ? Number(document.getElementById(id).value) : null);
  const vitalSigns = {
    systolic: bloodPressureMatch ? Number(bloodPressureMatch[1]) : null,
    diastolic: bloodPressureMatch ? Number(bloodPressureMatch[2]) : null,
    pulse: optionalNumber('docPulse'),
    temperature: optionalNumber('docTemperature'),
    weight: optionalNumber('docWeight'),
  };
  if (
    (vitalSigns.pulse !== null && (!Number.isInteger(vitalSigns.pulse) || vitalSigns.pulse < 0 || vitalSigns.pulse > 300)) ||
    (vitalSigns.temperature !== null && (vitalSigns.temperature < 25 || vitalSigns.temperature > 45)) ||
    (vitalSigns.weight !== null && (vitalSigns.weight < 0.1 || vitalSigns.weight > 500))
  ) {
    return alert('Vui lòng kiểm tra lại mạch, nhiệt độ và cân nặng đã nhập.');
  }
  savingExamination = true;
  button.disabled = true;
  try {
    const result = await apiRequest('/examinations/complete', {
      method: 'POST',
      body: JSON.stringify({
        requestId: flow.examinationRequestId,
        patientCode,
        roomId: room.id,
        icdCode,
        medicines,
        symptoms: document.getElementById('docSymptoms').value.trim(),
        medicalHistory: document.getElementById('docMedicalHistory').value.trim(),
        clinicalNote: document.getElementById('docClinicalNote').value.trim(),
        advice: document.getElementById('docAdvice').value.trim(),
        vitalSigns: Object.values(vitalSigns).some((value) => value !== null) ? vitalSigns : null,
      }),
    });
    updatePatientFlow({ id: patientCode, name: result.visit.patientName }, 'awaiting_payment', flow.room);
    showToast('Đã lưu hồ sơ vào lịch sử khám và chuyển bệnh nhân sang hàng chờ thanh toán.');
    setTimeout(() => {
      window.location.reload();
    }, 900);
  } catch (error) {
    alert(`Chưa lưu được hồ sơ: ${error.message}`);
    savingExamination = false;
    button.disabled = false;
  }
}

document.addEventListener('DOMContentLoaded', async () => {
  const button = document.getElementById('finishExaminationButton');
  button.disabled = true;
  try {
    await loadExaminationData();
    button.disabled = false;
  } catch (error) {
    alert(`Không tải được danh mục khám: ${error.message}. Vui lòng tải lại trang.`);
  }
});
