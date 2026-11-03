package com.dis.service;

import com.dis.dto.common.PageResponse;
import com.dis.dto.drug.CreateDrugRequest;
import com.dis.dto.drug.DrugDto;
import com.dis.dto.drug.UpdateDrugRequest;
import com.dis.exception.BusinessValidationException;
import com.dis.exception.ResourceNotFoundException;
import com.dis.model.DrugMaster;
import com.dis.repository.BatchRepository;
import com.dis.repository.DrugMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DrugService {

    private final DrugMasterRepository drugRepository;
    private final BatchRepository batchRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<DrugDto> getAllDrugs(String category, String query, Pageable pageable) {
        LocalDate today = LocalDate.now(clock);
        Page<DrugMaster> page = drugRepository.searchDrugs(
                category != null && !category.isBlank() ? category : null,
                query != null && !query.isBlank() ? query : null,
                pageable
        );

        return PageResponse.from(page.map(drug -> {
            DrugDto dto = DrugDto.from(drug);
            dto.setCurrentStock(batchRepository.getTotalUnexpiredStockByDrug(drug.getDrugId(), today));
            return dto;
        }));
    }

    @Transactional(readOnly = true)
    public DrugDto getDrugById(String drugId) {
        LocalDate today = LocalDate.now(clock);
        DrugMaster drug = drugRepository.findById(drugId)
                .orElseThrow(() -> new ResourceNotFoundException("Drug not found with ID: " + drugId));
        DrugDto dto = DrugDto.from(drug);
        dto.setCurrentStock(batchRepository.getTotalUnexpiredStockByDrug(drug.getDrugId(), today));
        return dto;
    }

    @Transactional
    public DrugDto createDrug(CreateDrugRequest request) {
        if (drugRepository.existsById(request.getDrugId())) {
            throw new BusinessValidationException("Drug already exists with ID: " + request.getDrugId());
        }

        DrugMaster drug = DrugMaster.builder()
                .drugId(request.getDrugId())
                .name(request.getName())
                .category(request.getCategory())
                .unit(request.getUnit())
                .unitCost(request.getUnitCost())
                .serviceLevel(request.getServiceLevel())
                .build();

        DrugMaster saved = drugRepository.save(drug);
        return DrugDto.from(saved);
    }

    @Transactional
    public DrugDto updateDrug(String drugId, UpdateDrugRequest request) {
        DrugMaster drug = drugRepository.findById(drugId)
                .orElseThrow(() -> new ResourceNotFoundException("Drug not found with ID: " + drugId));

        drug.setName(request.getName());
        drug.setCategory(request.getCategory());
        drug.setUnit(request.getUnit());
        drug.setUnitCost(request.getUnitCost());
        if (request.getServiceLevel() != null) {
            drug.setServiceLevel(request.getServiceLevel());
        }

        DrugMaster updated = drugRepository.save(drug);
        return DrugDto.from(updated);
    }

    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return drugRepository.findAllCategories();
    }
}
