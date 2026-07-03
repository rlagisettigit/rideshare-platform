package com.rideshare.platform.wallet.repository;

import com.rideshare.platform.wallet.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId);
}
