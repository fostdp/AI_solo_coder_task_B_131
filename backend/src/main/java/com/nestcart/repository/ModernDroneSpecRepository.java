package com.nestcart.repository;

import com.nestcart.entity.ModernDroneSpec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ModernDroneSpecRepository extends JpaRepository<ModernDroneSpec, UUID> {

    List<ModernDroneSpec> findAllByOrderBySortOrderAsc();

    List<ModernDroneSpec> findByCategory(String category);
}
