package com.bikash.photo_porter.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "transfer_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "transfer_id", nullable = false)
    private Long transferId;
    @Column(nullable = false)
    private LocalDateTime timestamp;
    @Column(columnDefinition = "TEXT")
    private String message;
    @PrePersist
    protected void onLogCreated() {
        this.timestamp = LocalDateTime.now();
    }
}
