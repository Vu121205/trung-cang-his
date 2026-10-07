package com.trungcang.trung_cang_his.controller;

import com.trungcang.trung_cang_his.domain.*;
import com.trungcang.trung_cang_his.repository.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final VisitRepository visits;
    private final PrescriptionRepository prescriptions;
    private final PrescriptionDetailRepository details;
    private final InvoiceRepository invoices;
    private final MedicineRepository medicines;
    private final MedicineBatchRepository batches;

    public ReportController(VisitRepository visits, PrescriptionRepository prescriptions,
                            PrescriptionDetailRepository details, InvoiceRepository invoices,
                            MedicineRepository medicines, MedicineBatchRepository batches) {
        this.visits = visits;
        this.prescriptions = prescriptions;
        this.details = details;
        this.invoices = invoices;
        this.medicines = medicines;
        this.batches = batches;
    }

    @GetMapping
    public ReportData report(@RequestParam(required = false) LocalDate from,
                             @RequestParam(required = false) LocalDate to,
                             @RequestParam(required = false) Long roomId,
                             @RequestParam(required = false) Visit.Status status,
                             @RequestParam(required = false) Visit.PaymentType paymentType,
                             @RequestParam(required = false) Long medicineId,
                             @RequestParam(defaultValue = "3") int expiryMonths,
                             @RequestParam(required = false) Integer stockThreshold) {
        LocalDate start = from == null ? LocalDate.now().withDayOfMonth(1) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        if (end.isBefore(start)) throw new IllegalArgumentException("Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu.");

        List<Visit> filteredVisits = visits.findAll().stream()
                .filter(v -> inRange(v.getVisitDate(), start, end))
                .filter(v -> roomId == null || (v.getRoom() != null && roomId.equals(v.getRoom().getId())))
                .filter(v -> status == null || v.getStatus() == status)
                .filter(v -> paymentType == null || v.getPaymentType() == paymentType)
                .toList();
        Set<Long> visitIds = filteredVisits.stream().map(Visit::getId).collect(Collectors.toSet());

        Revenue revenue = invoices.findAll().stream()
                .filter(invoice -> invoice.getStatus() == Invoice.Status.PAID)
                .filter(invoice -> inRange(invoiceDate(invoice), start, end))
                .filter(invoice -> invoice.getVisit() != null && (roomId == null || (invoice.getVisit().getRoom() != null && roomId.equals(invoice.getVisit().getRoom().getId()))))
                .filter(invoice -> paymentType == null || invoice.getVisit().getPaymentType() == paymentType)
                .collect(Collectors.collectingAndThen(Collectors.toList(), paid -> new Revenue(
                        paid.size(), paid.stream().map(Invoice::getTotalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add))));

        VisitActivity activity = new VisitActivity(
                filteredVisits.size(),
                filteredVisits.stream().filter(v -> v.getVisitType() == Visit.VisitType.NEW).count(),
                filteredVisits.stream().filter(v -> v.getVisitType() != Visit.VisitType.NEW).count(),
                filteredVisits.stream().filter(v -> v.getStatus() == Visit.Status.COMPLETED).count());

        List<MedicineUsage> usage = details.findAll().stream()
                .filter(d -> d.getPrescription() != null && d.getPrescription().getVisit() != null && visitIds.contains(d.getPrescription().getVisit().getId()))
                .filter(d -> medicineId == null || (d.getMedicine() != null && medicineId.equals(d.getMedicine().getId())))
                .collect(Collectors.groupingBy(d -> d.getMedicine().getId(), LinkedHashMap::new, Collectors.toList())).values().stream()
                .map(items -> new MedicineUsage(items.get(0).getMedicine().getId(), items.get(0).getMedicine().getCode(),
                        items.get(0).getMedicine().getName(), items.get(0).getUnit(),
                        items.stream().mapToLong(d -> d.getQuantity() == null ? 0 : d.getQuantity()).sum(),
                        items.stream().map(PrescriptionDetail::getTotalPrice).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add)))
                .toList();

        LocalDate expiryLimit = LocalDate.now().plusMonths(Math.max(0, expiryMonths));
        List<InventoryRow> inventory = medicines.findAll().stream()
                .filter(m -> activeMedicine(m) && medicineType(m) == Medicine.InventoryType.MEDICINE)
                .filter(m -> medicineId == null || medicineId.equals(m.getId()))
                .map(m -> inventoryRow(m, start, end, expiryLimit, stockThreshold))
                .toList();
        InventorySummary pharmacy = new InventorySummary(
                inventory.stream().mapToLong(InventoryRow::imported).sum(),
                inventory.stream().mapToLong(InventoryRow::exported).sum(),
                inventory.stream().mapToLong(InventoryRow::stock).sum(),
                expiryMonths,
                stockThreshold,
                inventory.stream().filter(InventoryRow::lowStock).toList(),
                inventory.stream().filter(InventoryRow::expiringSoon).toList(),
                inventory);
        return new ReportData(start, end, revenue, activity, usage, pharmacy);
    }

    private InventoryRow inventoryRow(Medicine medicine, LocalDate start, LocalDate end, LocalDate expiryLimit, Integer stockThreshold) {
        List<MedicineBatch> medicineBatches = batches.findAllByMedicine_IdOrderByExpiryDateAscIdAsc(medicine.getId());
        long stock = medicineBatches.stream().filter(this::availableBatch).mapToLong(b -> value(b.getQuantity())).sum();
        long imported = medicineBatches.stream().filter(b -> b.getCreatedAt() != null && inRange(b.getCreatedAt().toLocalDate(), start, end))
                .mapToLong(b -> value(b.getQuantity())).sum();
        long exported = details.findAll().stream()
                .filter(d -> d.getMedicine() != null && medicine.getId().equals(d.getMedicine().getId()))
                .filter(d -> d.getPrescription() != null && d.getPrescription().getStatus() == Prescription.Status.DISPENSED)
                .filter(d -> d.getPrescription().getPrescriptionDate() != null && inRange(d.getPrescription().getPrescriptionDate().toLocalDate(), start, end))
                .mapToLong(d -> value(d.getQuantity())).sum();
        LocalDate nearestExpiry = medicineBatches.stream().filter(this::availableBatch).map(MedicineBatch::getExpiryDate)
                .filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
        int threshold = stockThreshold == null ? value(medicine.getMinStock()) : stockThreshold;
        boolean lowStock = threshold > 0 && stock < threshold;
        boolean expiringSoon = nearestExpiry != null && !nearestExpiry.isBefore(LocalDate.now()) && !nearestExpiry.isAfter(expiryLimit);
        return new InventoryRow(medicine.getId(), medicine.getCode(), medicine.getName(), medicine.getUnit(), imported, exported,
                stock, threshold, nearestExpiry, lowStock, expiringSoon);
    }

    private boolean activeMedicine(Medicine medicine) { return medicine.getStatus() == Medicine.Status.ACTIVE; }
    private Medicine.InventoryType medicineType(Medicine medicine) { return medicine.getInventoryType() == null ? Medicine.InventoryType.MEDICINE : medicine.getInventoryType(); }
    private boolean availableBatch(MedicineBatch batch) { return value(batch.getQuantity()) > 0 && (batch.getExpiryDate() == null || !batch.getExpiryDate().isBefore(LocalDate.now())); }
    private int value(Integer number) { return number == null ? 0 : number; }
    private long value(Long number) { return number == null ? 0 : number; }
    private LocalDate invoiceDate(Invoice invoice) { return invoice.getInvoiceDate() == null ? null : invoice.getInvoiceDate().toLocalDate(); }
    private boolean inRange(LocalDate value, LocalDate start, LocalDate end) { return value != null && !value.isBefore(start) && !value.isAfter(end); }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "csv") String format,
                                         @RequestParam(required = false) LocalDate from,
                                         @RequestParam(required = false) LocalDate to,
                                         @RequestParam(required = false) Long roomId,
                                         @RequestParam(required = false) Visit.Status status,
                                         @RequestParam(required = false) Visit.PaymentType paymentType,
                                         @RequestParam(required = false) Long medicineId,
                                         @RequestParam(defaultValue = "3") int expiryMonths,
                                         @RequestParam(required = false) Integer stockThreshold) {
        ReportData data = report(from, to, roomId, status, paymentType, medicineId, expiryMonths, stockThreshold);
        String normalized = format.toLowerCase(Locale.ROOT);
        String body = "json".equals(normalized) ? toJson(data) : "html".equals(normalized) ? toHtml(data) : toCsv(data);
        MediaType type = "json".equals(normalized) ? MediaType.APPLICATION_JSON : "html".equals(normalized) ? MediaType.TEXT_HTML : MediaType.parseMediaType("text/csv;charset=UTF-8");
        String extension = "json".equals(normalized) ? "json" : "html".equals(normalized) ? "html" : "csv";
        return ResponseEntity.ok().contentType(type).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bao-cao-" + data.from() + "." + extension).body(body.getBytes(StandardCharsets.UTF_8));
    }

    private String toCsv(ReportData d) {
        StringBuilder out = new StringBuilder("\uFEFFBáo cáo Trung Cang HIS\r\n");
        out.append("DOANH THU\r\nTừ ngày,Đến ngày,Số hóa đơn,Tổng doanh thu\r\n").append(d.from()).append(',').append(d.to()).append(',').append(d.revenue().paidInvoices()).append(',').append(d.revenue().total()).append("\r\n\r\n");
        out.append("HOẠT ĐỘNG KHÁM BỆNH\r\nTổng lượt khám,Bệnh nhân mới,Tái khám,Đã hoàn tất\r\n").append(d.activity().totalVisits()).append(',').append(d.activity().newPatients()).append(',').append(d.activity().followUps()).append(',').append(d.activity().completedVisits()).append("\r\n\r\n");
        out.append("KHO DƯỢC\r\nMã thuốc,Tên thuốc,Nhập,Xuất,Tồn,Ngưỡng tồn,Hạn gần nhất,Cảnh báo\r\n");
        d.pharmacy().inventory().forEach(i -> out.append(csv(i.code())).append(',').append(csv(i.name())).append(',').append(i.imported()).append(',').append(i.exported()).append(',').append(i.stock()).append(',').append(i.threshold()).append(',').append(csv(i.nearestExpiry())).append(',').append(csv(alertText(i))).append("\r\n"));
        return out.toString();
    }

    private String toJson(ReportData d) {
        return "{\"from\":\"" + d.from() + "\",\"to\":\"" + d.to() + "\",\"revenue\":{" +
                "\"paidInvoices\":" + d.revenue().paidInvoices() + ",\"total\":" + d.revenue().total() + "}," +
                "\"activity\":{" + "\"totalVisits\":" + d.activity().totalVisits() + ",\"newPatients\":" + d.activity().newPatients() + ",\"followUps\":" + d.activity().followUps() + ",\"completedVisits\":" + d.activity().completedVisits() + "}," +
                "\"pharmacy\":{" + "\"imported\":" + d.pharmacy().imported() + ",\"exported\":" + d.pharmacy().exported() + ",\"stock\":" + d.pharmacy().stock() + ",\"expiryMonths\":" + d.pharmacy().expiryMonths() + ",\"inventory\":" + inventoryJson(d.pharmacy().inventory()) + ",\"lowStock\":" + inventoryJson(d.pharmacy().lowStock()) + ",\"expiringSoon\":" + inventoryJson(d.pharmacy().expiringSoon()) + "}," +
                "\"medicineUsage\":" + d.medicineUsage().stream().map(u -> "{\"code\":\"" + json(u.code()) + "\",\"name\":\"" + json(u.name()) + "\",\"unit\":\"" + json(u.unit()) + "\",\"quantity\":" + u.quantity() + ",\"amount\":" + u.amount() + "}").collect(Collectors.joining(",", "[", "]")) + "}";
    }

    private String inventoryJson(List<InventoryRow> rows) { return rows.stream().map(i -> "{\"code\":\"" + json(i.code()) + "\",\"name\":\"" + json(i.name()) + "\",\"unit\":\"" + json(i.unit()) + "\",\"imported\":" + i.imported() + ",\"exported\":" + i.exported() + ",\"stock\":" + i.stock() + ",\"threshold\":" + i.threshold() + ",\"nearestExpiry\":\"" + json(i.nearestExpiry()) + "\",\"lowStock\":" + i.lowStock() + ",\"expiringSoon\":" + i.expiringSoon() + "}").collect(Collectors.joining(",", "[", "]")); }
    private String toHtml(ReportData d) { return "<!doctype html><meta charset=\"utf-8\"><title>Báo cáo Trung Cang HIS</title><h1>Báo cáo Trung Cang HIS</h1><p>Khoảng ngày: " + d.from() + " - " + d.to() + "</p><h2>Doanh thu</h2><p>" + d.revenue().total() + " VNĐ / " + d.revenue().paidInvoices() + " hóa đơn</p><h2>Hoạt động khám bệnh</h2><p>Mới: " + d.activity().newPatients() + "; Tái khám: " + d.activity().followUps() + "; Hoàn tất: " + d.activity().completedVisits() + "</p><h2>Kho dược</h2><table border=1><tr><th>Mã</th><th>Tên thuốc</th><th>Nhập</th><th>Xuất</th><th>Tồn</th><th>Cảnh báo</th></tr>" + d.pharmacy().inventory().stream().map(i -> "<tr><td>" + html(i.code()) + "</td><td>" + html(i.name()) + "</td><td>" + i.imported() + "</td><td>" + i.exported() + "</td><td>" + i.stock() + "</td><td>" + html(alertText(i)) + "</td></tr>").collect(Collectors.joining()) + "</table>"; }
    private String alertText(InventoryRow row) { return row.lowStock() && row.expiringSoon() ? "Sắp hết hàng; Sắp hết hạn" : row.lowStock() ? "Sắp hết hàng" : row.expiringSoon() ? "Sắp hết hạn" : ""; }
    private String csv(Object value) { return "\"" + String.valueOf(value == null ? "" : value).replace("\"", "\"\"") + "\""; }
    private String json(Object value) { return String.valueOf(value == null ? "" : value).replace("\\", "\\\\").replace("\"", "\\\""); }
    private String html(Object value) { return String.valueOf(value == null ? "" : value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }

    public record ReportData(LocalDate from, LocalDate to, Revenue revenue, VisitActivity activity,
                             List<MedicineUsage> medicineUsage, InventorySummary pharmacy) {}
    public record Revenue(long paidInvoices, BigDecimal total) {}
    public record VisitActivity(long totalVisits, long newPatients, long followUps, long completedVisits) {}
    public record MedicineUsage(Long medicineId, String code, String name, String unit, long quantity, BigDecimal amount) {}
    public record InventorySummary(long imported, long exported, long stock, int expiryMonths, Integer stockThreshold,
                                   List<InventoryRow> lowStock, List<InventoryRow> expiringSoon, List<InventoryRow> inventory) {}
    public record InventoryRow(Long medicineId, String code, String name, String unit, long imported, long exported,
                               long stock, int threshold, LocalDate nearestExpiry, boolean lowStock, boolean expiringSoon) {}
}
