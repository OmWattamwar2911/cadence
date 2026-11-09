package com.walletservice.service;

import com.walletservice.entity.Wallet;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;

@Component
public class TransferExecutionHook {

    private volatile BiConsumer<Wallet, Wallet> onAfterDebitAction = null;

    public void setOnAfterDebitAction(BiConsumer<Wallet, Wallet> action) {
        this.onAfterDebitAction = action;
    }

    public void clearHook() {
        this.onAfterDebitAction = null;
    }

    public void triggerOnAfterDebit(Wallet sender, Wallet receiver) {
        BiConsumer<Wallet, Wallet> action = this.onAfterDebitAction;
        if (action != null) {
            action.accept(sender, receiver);
        }
    }
}
