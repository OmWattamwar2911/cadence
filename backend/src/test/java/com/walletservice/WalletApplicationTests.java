package com.walletservice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WalletApplicationTests extends BaseIntegrationTest {

    @Test
    @DisplayName("Context loads cleanly and connects to Testcontainers MySQL with Flyway migration")
    void contextLoads() {
        assertThat(MYSQL_CONTAINER.isRunning()).isTrue();
    }
}
