package com.dis.service;

import com.dis.dto.common.PageResponse;
import com.dis.dto.supplier.CreateSupplierRequest;
import com.dis.dto.supplier.SupplierDto;
import com.dis.dto.supplier.UpdateSupplierRequest;
import com.dis.exception.BusinessValidationException;
import com.dis.exception.ResourceNotFoundException;
import com.dis.model.Supplier;
import com.dis.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;

    @Transactional(readOnly = true)
    public PageResponse<SupplierDto> getAllSuppliers(String query, Pageable pageable) {
        Page<Supplier> page = supplierRepository.searchSuppliers(
                query != null && !query.isBlank() ? query : null,
                pageable
        );
        return PageResponse.from(page.map(SupplierDto::from));
    }

    @Transactional(readOnly = true)
    public SupplierDto getSupplierById(String supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: " + supplierId));
        return SupplierDto.from(supplier);
    }

    @Transactional
    public SupplierDto createSupplier(CreateSupplierRequest request) {
        if (supplierRepository.existsById(request.getSupplierId())) {
            throw new BusinessValidationException("Supplier already exists with ID: " + request.getSupplierId());
        }

        Supplier supplier = Supplier.builder()
                .supplierId(request.getSupplierId())
                .name(request.getName())
                .contact(request.getContact())
                .leadTimeDays(request.getLeadTimeDays())
                .build();

        Supplier saved = supplierRepository.save(supplier);
        return SupplierDto.from(saved);
    }

    @Transactional
    public SupplierDto updateSupplier(String supplierId, UpdateSupplierRequest request) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: " + supplierId));

        supplier.setName(request.getName());
        supplier.setContact(request.getContact());
        supplier.setLeadTimeDays(request.getLeadTimeDays());

        Supplier updated = supplierRepository.save(supplier);
        return SupplierDto.from(updated);
    }
}
