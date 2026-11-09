-- -----------------------------------------------------------------------------
-- V2__advanced_fintech_features.sql
-- 1. Cryptographic Merkle Hash Chain on ledger_entries
-- 2. Scheduled & Recurring Transfers
-- 3. Payment Requests & Split the Bill
-- -----------------------------------------------------------------------------

-- 1. Cryptographic ledger hash chain columns
ALTER TABLE ledger_entries ADD COLUMN prev_hash VARCHAR(64) NULL;
ALTER TABLE ledger_entries ADD COLUMN entry_hash VARCHAR(64) NULL;

-- 2. Scheduled Transfers Table
CREATE TABLE scheduled_transfers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    amount BIGINT NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    next_run_at TIMESTAMP NOT NULL,
    last_run_at TIMESTAMP NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_scheduled_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_scheduled_amount CHECK (amount > 0),
    CONSTRAINT chk_scheduled_frequency CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY')),
    CONSTRAINT chk_scheduled_status CHECK (status IN ('ACTIVE', 'PAUSED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_scheduled_status_next_run ON scheduled_transfers (status, next_run_at);
CREATE INDEX idx_scheduled_user_status ON scheduled_transfers (user_id, status);

-- 3. Payment Requests & Split Bill Table
CREATE TABLE payment_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    requester_user_id BIGINT NOT NULL,
    payer_email VARCHAR(255) NOT NULL,
    amount BIGINT NOT NULL,
    note VARCHAR(255) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    transfer_id BIGINT NULL,
    paid_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payreq_requester FOREIGN KEY (requester_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_payreq_transfer FOREIGN KEY (transfer_id) REFERENCES transfers (id) ON DELETE RESTRICT,
    CONSTRAINT chk_payreq_amount CHECK (amount > 0),
    CONSTRAINT chk_payreq_status CHECK (status IN ('PENDING', 'PAID', 'DECLINED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_payreq_payer_status ON payment_requests (payer_email, status);
CREATE INDEX idx_payreq_requester_status ON payment_requests (requester_user_id, status);
