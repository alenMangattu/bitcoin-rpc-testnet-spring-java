package com.coinbase.cb.repository;

import com.coinbase.cb.models.DbTables.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByAddress(String address);
    
    List<Wallet> findAllByAddressIn(List<String> addresses);
    
    List<Wallet> findAllByWalletNameIn(List<String> walletNames);
    
    @Query(value = "SELECT * FROM wallets w WHERE MOD(w.wallet_id, :totalPartitions) = :partitionId", 
           nativeQuery = true)
    Page<Wallet> findAllByPartition(
        @Param("partitionId") int partitionId, 
        @Param("totalPartitions") int totalPartitions, 
        Pageable pageable);
}
