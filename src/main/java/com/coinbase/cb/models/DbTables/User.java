package com.coinbase.cb.models.DbTables;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.LocalDateTime;
import java.util.List;
import java.io.Serializable;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
@ToString(exclude = {"sentTransactions", "receivedTransactions", "wallet"})
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "user_id")
    @JsonProperty("userId")
    private Long user_id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name="first_name", nullable = false)
    @JsonProperty("firstName")
    private String firstName;

    @Column(name="last_name", nullable = false)
    @JsonProperty("lastName")
    private String lastName;

    @Column(name="created_at", nullable = false, updatable = false)
    @CreationTimestamp
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @Column(name="updated_at", nullable = false)
    @UpdateTimestamp
    @JsonProperty("updatedAt")
    private LocalDateTime updateAt;

    @JsonIgnore
    @OneToMany(mappedBy = "fromUser")
    private List<Transaction> sentTransactions;

    @JsonIgnore
    @OneToMany(mappedBy = "toUser")
    private List<Transaction> receivedTransactions;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Wallet wallet;
}

