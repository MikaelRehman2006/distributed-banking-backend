package com.mikaelrehman.banking.repository;

import com.mikaelrehman.banking.entity.IdempotentRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotentRequestRepository extends JpaRepository<IdempotentRequest, Long> {

    Optional<IdempotentRequest> findByIdempotencyKey(String idempotencyKey);
}
