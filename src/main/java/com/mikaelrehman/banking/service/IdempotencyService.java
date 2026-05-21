package com.mikaelrehman.banking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mikaelrehman.banking.dto.TransferRequest;
import com.mikaelrehman.banking.dto.TransferResponse;
import com.mikaelrehman.banking.entity.IdempotentRequest;
import com.mikaelrehman.banking.exception.IdempotencyConflictException;
import com.mikaelrehman.banking.repository.IdempotentRequestRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class IdempotencyService {

    private final IdempotentRequestRepository idempotentRequestRepository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotentRequestRepository idempotentRequestRepository, ObjectMapper objectMapper) {
        this.idempotentRequestRepository = idempotentRequestRepository;
        this.objectMapper = objectMapper;
    }

    public Optional<ResponseEntity<TransferResponse>> findReplay(String idempotencyKey, TransferRequest request) {
        return idempotentRequestRepository.findByIdempotencyKey(idempotencyKey)
                .map(stored -> {
                    String requestHash = hashRequest(request);
                    if (!stored.getRequestHash().equals(requestHash)) {
                        throw new IdempotencyConflictException(
                                "Idempotency key reused with a different request body");
                    }
                    try {
                        TransferResponse body = objectMapper.readValue(stored.getResponseBody(), TransferResponse.class);
                        return ResponseEntity.status(stored.getHttpStatus()).body(body);
                    } catch (JsonProcessingException e) {
                        throw new IllegalStateException("Stored idempotent response is invalid JSON", e);
                    }
                });
    }

    public void store(String idempotencyKey, TransferRequest request, ResponseEntity<TransferResponse> response) {
        try {
            String responseBody = objectMapper.writeValueAsString(response.getBody());
            IdempotentRequest record = new IdempotentRequest(
                    idempotencyKey,
                    hashRequest(request),
                    response.getStatusCode().value(),
                    responseBody);
            idempotentRequestRepository.save(record);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize transfer response", e);
        }
    }

    public String hashRequest(TransferRequest request) {
        String payload = request.sourceAccountId() + "|" + request.destinationAccountId() + "|" + request.amount();
        return sha256(payload);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
