package com.dis.repository;

import com.dis.model.DrugMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DrugMasterRepository extends JpaRepository<DrugMaster, String> {

    @Query("SELECT d FROM DrugMaster d WHERE " +
           "(:category IS NULL OR LOWER(d.category) = LOWER(:category)) AND " +
           "(:query IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(d.drugId) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<DrugMaster> searchDrugs(@Param("category") String category,
                                 @Param("query") String query,
                                 Pageable pageable);

    @Query("SELECT DISTINCT d.category FROM DrugMaster d ORDER BY d.category ASC")
    List<String> findAllCategories();
}
