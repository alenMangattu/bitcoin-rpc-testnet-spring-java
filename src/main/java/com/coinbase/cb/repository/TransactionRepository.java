package com.coinbase.cb.repository;

import com.coinbase.cb.models.DbTables.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
