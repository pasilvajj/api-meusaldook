package com.economy.finance.persistence;

import com.economy.finance.domain.Category;
import com.economy.finance.domain.MoneyKind;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByIdAndOwner_Id(Long id, Long ownerId);

    @EntityGraph(attributePaths = {"parent"})
    List<Category> findByOwner_IdOrderByNameAsc(Long ownerId);

    List<Category> findByOwner_IdAndKindOrderByNameAsc(Long ownerId, MoneyKind kind);

    long countByOwner_Id(Long ownerId);
}
