package com.economy.finance.persistence;

import com.economy.finance.domain.UserAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    List<UserAccount> findByOwner_IdOrderByAccountTypeAscNameAsc(Long ownerId);

    Optional<UserAccount> findByIdAndOwner_Id(Long id, Long ownerId);

    Optional<UserAccount> findByOwner_IdAndPublicKey(Long ownerId, String publicKey);

    boolean existsByOwner_IdAndPublicKey(Long ownerId, String publicKey);
}
