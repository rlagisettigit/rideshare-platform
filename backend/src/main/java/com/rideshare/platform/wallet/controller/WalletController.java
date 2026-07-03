package com.rideshare.platform.wallet.controller;

import com.rideshare.platform.common.ApiResponse;
import com.rideshare.platform.wallet.entity.Wallet;
import com.rideshare.platform.wallet.entity.WalletTransaction;
import com.rideshare.platform.wallet.repository.WalletRepository;
import com.rideshare.platform.wallet.repository.WalletTransactionRepository;
import com.rideshare.platform.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
@Tag(name = "Wallet")
public class WalletController {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;

    @GetMapping("/me")
    public ApiResponse<Wallet> myWallet(@AuthenticationPrincipal String userPublicId) {
        Long userId = userRepository.findByPublicId(userPublicId).orElseThrow().getId();
        return ApiResponse.ok(walletRepository.findByUserId(userId).orElse(null));
    }

    @GetMapping("/transactions")
    public ApiResponse<List<WalletTransaction>> myTransactions(@AuthenticationPrincipal String userPublicId) {
        Long userId = userRepository.findByPublicId(userPublicId).orElseThrow().getId();
        Wallet wallet = walletRepository.findByUserId(userId).orElse(null);
        if (wallet == null) {
            return ApiResponse.ok(List.of());
        }
        return ApiResponse.ok(walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId()));
    }
}
