package com.nestcart.modules.evolution_analyzer.repository;

import com.nestcart.modules.evolution_analyzer.entity.DynastyCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DynastyCartRepository extends JpaRepository<DynastyCart, UUID> {

    List<DynastyCart> findAllByOrderBySortOrderAsc();

    List<DynastyCart> findByDynastyNameContainingIgnoreCase(String dynastyName);
}
