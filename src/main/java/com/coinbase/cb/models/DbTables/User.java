package com.coinbase.cb.models.DbTables;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
@ToString
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long user_id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name="first_name", nullable = false)
    private String firstName;


    @Column(name="last_name", nullable = false)
    private String lastName;


    @Column(name="created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createAt;


    @Column(name="updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updateAt;


    @OneToMany(mappedBy = "fromUser")
    private List<Transaction> sentTransactions;

    @OneToMany(mappedBy = "toUser")
    private List<Transaction> receivedTransactions;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Wallet wallet;


}
