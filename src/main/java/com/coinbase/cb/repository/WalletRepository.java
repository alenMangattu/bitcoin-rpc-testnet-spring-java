package com.coinbase.cb.repository;

import com.coinbase.cb.models.DbTables.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
}
