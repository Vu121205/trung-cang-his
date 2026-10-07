package com.trungcang.trung_cang_his.controller;

import com.trungcang.trung_cang_his.domain.Medicine;
import com.trungcang.trung_cang_his.domain.MedicineBatch;
import com.trungcang.trung_cang_his.repository.MedicineBatchRepository;
import com.trungcang.trung_cang_his.repository.MedicineRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final MedicineRepository medicines;
    private final MedicineBatchRepository batches;

    public InventoryController(MedicineRepository medicines, MedicineBatchRepository batches) {
        this.medicines = medicines;
        this.batches = batches;
    }

    @GetMapping
    public List<InventoryItem> list(@RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) Boolean activeOnly,
                                    @RequestParam(required = false) Medicine.InventoryType warehouse) {
        String search = keyword == null ? "" : keyword.trim().toLowerCase();
        return medicines.findAll().stream()
                .filter(m -> !Boolean.TRUE.equals(activeOnly) || m.getStatus() == Medicine.Status.ACTIVE)
                .filter(m -> warehouse == null || inventoryTypeOf(m) == warehouse)
                .filter(m -> search.isBlank() || m.getCode().toLowerCase().contains(search)
                        || m.getName().toLowerCase().contains(search)
                        || (m.getActiveIngredient() != null && m.getActiveIngredient().toLowerCase().contains(search)))
                .map(m -> InventoryItem.from(m, inventoryTypeOf(m),
                        batches.findAllByMedicine_IdOrderByExpiryDateAscIdAsc(m.getId()).stream().map(BatchView::new).toList()))
                .toList();
    }

    private Medicine.InventoryType inventoryTypeOf(Medicine medicine) {
        return medicine.getInventoryType() == null ? Medicine.InventoryType.MEDICINE : medicine.getInventoryType();
    }

    @PostMapping("/medicines")
    public Medicine createMedicine(@RequestBody Medicine medicine) {
        medicine.setId(null);
        medicine.setCreatedAt(java.time.LocalDateTime.now());
        return medicines.save(medicine);
    }

    @PutMapping("/medicines/{id}")
    public Medicine updateMedicine(@PathVariable Long id, @RequestBody Medicine input) {
        Medicine medicine = medicines.findById(id).orElseThrow();
        medicine.setCode(input.getCode());
        medicine.setName(input.getName());
        medicine.setActiveIngredient(input.getActiveIngredient());
        medicine.setStrength(input.getStrength());
        medicine.setDosageForm(input.getDosageForm());
        medicine.setUnit(input.getUnit());
        medicine.setManufacturer(input.getManufacturer());
        medicine.setRegistrationNumber(input.getRegistrationNumber());
        medicine.setPrice(input.getPrice());
        medicine.setMinStock(input.getMinStock());
        medicine.setStatus(input.getStatus());
        return medicines.save(medicine);
    }

    @PostMapping("/batches")
    public MedicineBatch createBatch(@RequestBody BatchRequest request) {
        Medicine medicine = medicines.findById(request.medicineId()).orElseThrow();
        MedicineBatch batch = new MedicineBatch();
        batch.setMedicine(medicine);
        batch.setBatchNumber(request.batchNumber());
        batch.setManufactureDate(request.manufactureDate());
        batch.setExpiryDate(request.expiryDate());
        batch.setQuantity(request.quantity());
        batch.setUnitPrice(request.unitPrice() == null ? medicine.getPrice() : request.unitPrice());
        batch.setCreatedAt(java.time.LocalDateTime.now());
        return batches.save(batch);
    }

    @PutMapping("/batches/{id}")
    public MedicineBatch updateBatch(@PathVariable Long id, @RequestBody BatchRequest request) {
        MedicineBatch batch = batches.findById(id).orElseThrow();
        batch.setBatchNumber(request.batchNumber());
        batch.setManufactureDate(request.manufactureDate());
        batch.setExpiryDate(request.expiryDate());
        batch.setQuantity(request.quantity());
        if (request.unitPrice() != null) batch.setUnitPrice(request.unitPrice());
        return batches.save(batch);
    }

    @DeleteMapping("/batches/{id}")
    public ResponseEntity<Void> deleteBatch(@PathVariable Long id) {
        batches.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    public record BatchRequest(Long medicineId, String batchNumber, LocalDate manufactureDate,
                               LocalDate expiryDate, Integer quantity, java.math.BigDecimal unitPrice) {}

    public record InventoryItem(Long id, String code, String name, String activeIngredient, String strength,
                                String unit, java.math.BigDecimal price, Integer minStock, Medicine.Status status,
                                Medicine.InventoryType inventoryType, long availableStock, LocalDate nearestExpiry,
                                boolean lowStock) {
        static InventoryItem from(Medicine medicine, Medicine.InventoryType inventoryType, List<BatchView> batches) {
            List<BatchView> available = batches.stream().filter(InventoryItem::usable).toList();
            long availableStock = available.stream().mapToLong(BatchView::quantity).sum();
            LocalDate nearestExpiry = available.stream().map(BatchView::expiryDate).filter(java.util.Objects::nonNull)
                    .min(Comparator.naturalOrder()).orElse(null);
            int minimum = medicine.getMinStock() == null ? 0 : medicine.getMinStock();
            String unit = medicine.getUnit() == null || medicine.getUnit().isBlank() ? "—" : medicine.getUnit().trim();
            return new InventoryItem(medicine.getId(), medicine.getCode(), medicine.getName(), medicine.getActiveIngredient(),
                    medicine.getStrength(), unit, medicine.getPrice(), medicine.getMinStock(), medicine.getStatus(), inventoryType,
                    availableStock, nearestExpiry, availableStock <= minimum);
        }

        private static boolean usable(BatchView batch) {
            return batch.quantity() != null && batch.quantity() > 0
                    && (batch.expiryDate() == null || !batch.expiryDate().isBefore(LocalDate.now()));
        }
    }

    public record BatchView(Long id, String batchNumber, LocalDate manufactureDate, LocalDate expiryDate,
                            Integer quantity, java.math.BigDecimal unitPrice) {
        BatchView(MedicineBatch batch) { this(batch.getId(), batch.getBatchNumber(), batch.getManufactureDate(),
                batch.getExpiryDate(), batch.getQuantity(), batch.getUnitPrice()); }
    }
}
