package com.nestcart.repository;

import com.nestcart.entity.DynastyCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * @deprecated 请使用 {@link com.nestcart.modules.evolution_analyzer.repository.DynastyCartRepository} 替代
 */
@Deprecated
@Repository
public interface DynastyCartRepository extends JpaRepository<DynastyCart, UUID> {

    List<DynastyCart> findAllByOrderBySortOrderAsc();

    List<DynastyCart> findByDynastyNameContainingIgnoreCase(String dynastyName);
}
