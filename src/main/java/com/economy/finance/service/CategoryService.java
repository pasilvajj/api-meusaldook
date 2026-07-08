package com.economy.finance.service;

import com.economy.finance.api.dto.CategoryRequest;
import com.economy.finance.api.dto.CategoryResponse;
import com.economy.finance.api.exception.ResourceNotFoundException;
import com.economy.finance.domain.AppUser;
import com.economy.finance.domain.Category;
import com.economy.finance.domain.MoneyKind;
import com.economy.finance.config.UserCacheEvictor;
import com.economy.finance.persistence.AppUserRepository;
import com.economy.finance.persistence.CategoryRepository;
import com.economy.finance.persistence.FinanceTransactionRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    public static final String CARD_PAYMENT_CATEGORY_NAME = "Pagamento de cartão";

    private final CategoryRepository categoryRepository;
    private final FinanceTransactionRepository transactionRepository;
    private final AppUserRepository appUserRepository;
    private final CurrentUserService currentUserService;
    private final UserCacheEvictor userCacheEvictor;

    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "@cacheKeyHelper.userCategories()")
    public List<CategoryResponse> listAll() {
        Long userId = currentUserService.requireUserId();
        return categoryRepository.findByOwner_IdOrderByNameAsc(userId).stream()
                .filter(c -> !c.isSystemHidden())
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional
    public CategoryResponse ensureCardPaymentCategory() {
        Long userId = currentUserService.requireUserId();
        Category category = ensureCardPaymentCategoryEntity(userId);
        return CategoryResponse.from(category);
    }

    @Transactional
    public Category ensureCardPaymentCategoryEntity(Long userId) {
        var existing =
                categoryRepository.findByOwner_IdAndNameAndKind(userId, CARD_PAYMENT_CATEGORY_NAME, MoneyKind.EXPENSE);
        if (existing.isPresent()) {
            Category category = existing.get();
            if (!category.isSystemHidden()) {
                category.setSystemHidden(true);
                categoryRepository.save(category);
                userCacheEvictor.evictUser(userId);
            }
            return category;
        }
        AppUser owner = appUserRepository.getReferenceById(userId);
        Category category =
                categoryRepository.save(
                        Category.builder()
                                .owner(owner)
                                .name(CARD_PAYMENT_CATEGORY_NAME)
                                .kind(MoneyKind.EXPENSE)
                                .systemHidden(true)
                                .createdAt(Instant.now())
                                .build());
        transactionRepository.reassignInvoicePaymentsToCategory(userId, category);
        userCacheEvictor.evictUser(userId);
        return category;
    }

    @Transactional
    public void seedDefaultsIfEmpty(Long userId) {
        if (categoryRepository.countByOwner_Id(userId) > 0) {
            return;
        }
        AppUser owner = appUserRepository.getReferenceById(userId);
        Instant now = Instant.now();

        Category moradia =
                Category.builder()
                        .owner(owner)
                        .name("Moradia")
                        .kind(MoneyKind.EXPENSE)
                        .createdAt(now)
                        .build();
        Category savedMoradia = categoryRepository.save(moradia);

        List<Category> defaults = new ArrayList<>();
        defaults.add(Category.builder().owner(owner).name("Alimentação").kind(MoneyKind.EXPENSE).createdAt(now).build());
        defaults.add(Category.builder().owner(owner).name("Academia").kind(MoneyKind.EXPENSE).createdAt(now).build());
        defaults.add(Category.builder().owner(owner).name("Bem Estar").kind(MoneyKind.EXPENSE).createdAt(now).build());
        defaults.add(Category.builder().owner(owner).name("Aluguel").kind(MoneyKind.EXPENSE).createdAt(now).build());
        defaults.add(Category.builder().owner(owner).name("Supermercado").kind(MoneyKind.EXPENSE).createdAt(now).build());
        defaults.add(Category.builder().owner(owner).name("Gás").kind(MoneyKind.EXPENSE).createdAt(now).build());
       
        defaults.add(Category.builder().owner(owner).name("Restaurante").kind(MoneyKind.EXPENSE).createdAt(now).build());
        defaults.add(Category.builder().owner(owner).name("Férias").kind(MoneyKind.EXPENSE).createdAt(now).build());
        defaults.add(Category.builder().owner(owner).name("Viagens").kind(MoneyKind.EXPENSE).createdAt(now).build());
        defaults.add(Category.builder().owner(owner).name("Academia").kind(MoneyKind.EXPENSE).createdAt(now).build());
        defaults.add(Category.builder().owner(owner).name("Presente").kind(MoneyKind.EXPENSE).createdAt(now).build());
        defaults.add(Category.builder().owner(owner).name("Entreterimento").kind(MoneyKind.EXPENSE).createdAt(now).build());
        
        defaults.add(Category.builder().owner(owner).name("Cartão").kind(MoneyKind.EXPENSE).createdAt(now).build());
        defaults.add(Category.builder().owner(owner).name("Educação").kind(MoneyKind.EXPENSE).createdAt(now).build());
        defaults.add(Category.builder().owner(owner).name("Lazer").kind(MoneyKind.EXPENSE).createdAt(now).build());
        defaults.add(Category.builder().owner(owner).name("Outras Despesas").kind(MoneyKind.EXPENSE).createdAt(now).build());
        defaults.add(Category.builder().owner(owner).name("Saúde").kind(MoneyKind.EXPENSE).createdAt(now).build());
        defaults.add(Category.builder().owner(owner).name("Seguro").kind(MoneyKind.EXPENSE).createdAt(now).build());
        defaults.add(Category.builder().owner(owner).name("Transporte").kind(MoneyKind.EXPENSE).createdAt(now).build());
        defaults.add(Category.builder().owner(owner).name("Vestuário").kind(MoneyKind.EXPENSE).createdAt(now).build());
        defaults.add(Category.builder()
                .owner(owner)
                .name("Água")
                .kind(MoneyKind.EXPENSE)
                .parent(savedMoradia)
                .createdAt(now)
                .build());
        defaults.add(Category.builder()
                .owner(owner)
                .name("Luz")
                .kind(MoneyKind.EXPENSE)
                .parent(savedMoradia)
                .createdAt(now)
                .build());
        defaults.add(Category.builder().owner(owner).name("Salário").kind(MoneyKind.INCOME).createdAt(now).build());
        defaults.add(Category.builder().owner(owner).name("Rendimentos").kind(MoneyKind.INCOME).createdAt(now).build());
        defaults.add(Category.builder().owner(owner).name("Outras Receitas").kind(MoneyKind.INCOME).createdAt(now).build());
        defaults.add(Category.builder()
                .owner(owner)
                .name(CARD_PAYMENT_CATEGORY_NAME)
                .kind(MoneyKind.EXPENSE)
                .systemHidden(true)
                .createdAt(now)
                .build());
        categoryRepository.saveAll(defaults);
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(Long id) {
        Long userId = currentUserService.requireUserId();
        Category c =
                categoryRepository
                        .findByIdAndOwner_Id(id, userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));
        return CategoryResponse.from(c);
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        Long userId = currentUserService.requireUserId();
        AppUser owner = appUserRepository.getReferenceById(userId);
        Category parent = null;
        if (request.getParentId() != null) {
            parent =
                    categoryRepository
                            .findByIdAndOwner_Id(request.getParentId(), userId)
                            .orElseThrow(() -> new ResourceNotFoundException("Categoria pai não encontrada"));
            if (parent.getKind() != request.getKind()) {
                throw new IllegalArgumentException("A categoria pai deve ser do mesmo tipo (receita/despesa)");
            }
        }
        Category entity =
                Category.builder()
                        .owner(owner)
                        .name(request.getName().trim())
                        .kind(request.getKind())
                        .parent(parent)
                        .createdAt(Instant.now())
                        .build();
        CategoryResponse created = CategoryResponse.from(categoryRepository.save(entity));
        userCacheEvictor.evictUser(userId);
        return created;
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Long userId = currentUserService.requireUserId();
        Category entity =
                categoryRepository
                        .findByIdAndOwner_Id(id, userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));
        assertNotSystemCategory(entity);
        Category parent = null;
        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new IllegalArgumentException("A categoria não pode ser pai de si própria");
            }
            parent =
                    categoryRepository
                            .findByIdAndOwner_Id(request.getParentId(), userId)
                            .orElseThrow(() -> new ResourceNotFoundException("Categoria pai não encontrada"));
            if (parent.getKind() != request.getKind()) {
                throw new IllegalArgumentException("A categoria pai deve ser do mesmo tipo (receita/despesa)");
            }
        }
        entity.setName(request.getName().trim());
        entity.setKind(request.getKind());
        entity.setParent(parent);
        CategoryResponse updated = CategoryResponse.from(entity);
        userCacheEvictor.evictUser(userId);
        return updated;
    }

    @Transactional
    public void delete(Long id) {
        Long userId = currentUserService.requireUserId();
        Category entity =
                categoryRepository
                        .findByIdAndOwner_Id(id, userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));
        assertNotSystemCategory(entity);
        categoryRepository.delete(entity);
        userCacheEvictor.evictUser(userId);
    }

    private static void assertNotSystemCategory(Category entity) {
        if (entity.isSystemHidden()) {
            throw new IllegalArgumentException("Esta categoria do sistema não pode ser alterada.");
        }
    }
}
