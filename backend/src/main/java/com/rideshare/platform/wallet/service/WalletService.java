package com.rideshare.platform.wallet.service;

import com.rideshare.platform.common.exception.ApiException;
import com.rideshare.platform.wallet.entity.Wallet;
import com.rideshare.platform.wallet.entity.WalletTransaction;
import com.rideshare.platform.wallet.repository.WalletRepository;
import com.rideshare.platform.wallet.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Backs refunds (Section 9 Cancellation), coupon credits, driver payouts. */
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Transactional
    public Wallet credit(Long userId, BigDecimal amount, String reason, String reference) {
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId).orElseGet(() -> {
            Wallet w = new Wallet();
            w.setUserId(userId);
            return walletRepository.save(w);
        });
        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);
        recordTransaction(wallet.getId(), amount, "CREDIT", reason, reference);
        return wallet;
    }

    @Transactional
    public Wallet debit(Long userId, BigDecimal amount, String reason, String reference) {
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> ApiException.notFound("WALLET_001", "Wallet not found."));
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw ApiException.businessRule("WALLET_002", "Insufficient wallet balance.");
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);
        recordTransaction(wallet.getId(), amount, "DEBIT", reason, reference);
        return wallet;
    }

    private void recordTransaction(Long walletId, BigDecimal amount, String type, String reason, String reference) {
        WalletTransaction tx = new WalletTransaction();
        tx.setWalletId(walletId);
        tx.setAmount(amount);
        tx.setType(type);
        tx.setReason(reason);
        tx.setReference(reference);
        walletTransactionRepository.save(tx);
    }
}
