package com.walletservice.service;

import com.walletservice.entity.Transfer;
import com.walletservice.entity.TransferStatus;
import com.walletservice.entity.Wallet;
import com.walletservice.repository.TransferRepository;
import com.walletservice.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferRejectionService {

    private static final Logger log = LoggerFactory.getLogger(TransferRejectionService.class);

    private final TransferRepository transferRepository;
    private final WalletRepository walletRepository;

    public TransferRejectionService(TransferRepository transferRepository, WalletRepository walletRepository) {
        this.transferRepository = transferRepository;
        this.walletRepository = walletRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRejectedTransfer(Long senderWalletId, Long receiverWalletId, Long amount, String reference) {
        Wallet senderWallet = walletRepository.findById(senderWalletId).orElse(null);
        Wallet receiverWallet = walletRepository.findById(receiverWalletId).orElse(null);

        if (senderWallet != null && receiverWallet != null) {
            Transfer rejected = new Transfer(senderWallet, receiverWallet, amount, TransferStatus.REJECTED, reference);
            transferRepository.save(rejected);
            log.info("Persisted REJECTED transfer audit record: fromWallet={}, toWallet={}, amount={}, ref={}",
                    senderWalletId, receiverWalletId, amount, reference);
        } else {
            log.warn("Could not persist REJECTED transfer: senderWalletId={} or receiverWalletId={} not found",
                    senderWalletId, receiverWalletId);
        }
    }
}
