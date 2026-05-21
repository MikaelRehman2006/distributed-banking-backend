package com.mikaelrehman.banking.repository;

import com.mikaelrehman.banking.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
}
