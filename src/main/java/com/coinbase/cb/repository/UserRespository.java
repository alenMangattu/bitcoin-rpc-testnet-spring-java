package com.coinbase.cb.repository;

import com.coinbase.cb.models.DbTables.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.List;

public interface UserRespository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.wallet w WHERE u.email = :email")
    Optional<User> findByEmailWithWallet(@Param("email") String email);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.wallet w WHERE u.user_id = :userId")
    Optional<User> findByIdWithWallet(@Param("userId") Long userId);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.wallet w WHERE w.address = :address")
    Optional<User> findByWalletAddress(@Param("address") String address);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.wallet WHERE u.user_id IN :userIds")
    List<User> findAllByUserIdsWithWallet(@Param("userIds") List<Long> userIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.wallet WHERE u.email = :email")
    Optional<User> findByEmailWithWalletForUpdate(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.wallet WHERE u.user_id = :id")
    Optional<User> findByIdWithWalletForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.wallet w WHERE w.address = :address")
    Optional<User> findByWalletAddressForUpdate(@Param("address") String address);
}

