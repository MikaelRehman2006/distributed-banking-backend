package com.mikaelrehman.banking.service;

import com.mikaelrehman.banking.dto.TransferRequest;
import com.mikaelrehman.banking.dto.TransferResponse;
import com.mikaelrehman.banking.entity.Account;
import com.mikaelrehman.banking.entity.Transfer;
import com.mikaelrehman.banking.entity.TransferStatus;
import com.mikaelrehman.banking.exception.BadRequestException;
import com.mikaelrehman.banking.exception.ForbiddenException;
import com.mikaelrehman.banking.exception.InsufficientFundsException;
import com.mikaelrehman.banking.exception.ResourceNotFoundException;
import com.mikaelrehman.banking.repository.AccountRepository;
import com.mikaelrehman.banking.repository.TransferRepository;
import com.mikaelrehman.banking.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final IdempotencyService idempotencyService;

    public TransferService(
            AccountRepository accountRepository,
            TransferRepository transferRepository,
            IdempotencyService idempotencyService) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.idempotencyService = idempotencyService;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ResponseEntity<TransferResponse> executeTransfer(TransferRequest request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("Idempotency-Key header is required");
        }

        var replay = idempotencyService.findReplay(idempotencyKey, request);
        if (replay.isPresent()) {
            return replay.get();
        }

        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new BadRequestException("Source and destination accounts must differ");
        }

        Long userId = SecurityUtils.currentUser().getUserId();
        List<Long> orderedIds = List.of(request.sourceAccountId(), request.destinationAccountId())
                .stream()
                .sorted()
                .toList();

        Account first = accountRepository.findByIdForUpdate(orderedIds.get(0))
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + orderedIds.get(0)));
        Account second = accountRepository.findByIdForUpdate(orderedIds.get(1))
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + orderedIds.get(1)));

        Account source = first.getId().equals(request.sourceAccountId()) ? first : second;
        Account destination = first.getId().equals(request.destinationAccountId()) ? first : second;

        if (!source.getUserId().equals(userId)) {
            throw new ForbiddenException("You do not own the source account");
        }

        if (!source.getCurrency().equals(destination.getCurrency())) {
            throw new BadRequestException("Currency mismatch between accounts");
        }

        if (source.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds in source account");
        }

        source.debit(request.amount());
        destination.credit(request.amount());

        accountRepository.save(source);
        accountRepository.save(destination);

        Transfer transfer = transferRepository.save(new Transfer(
                source.getId(),
                destination.getId(),
                request.amount(),
                TransferStatus.COMPLETED));

        TransferResponse body = toResponse(transfer);
        ResponseEntity<TransferResponse> response = ResponseEntity.status(HttpStatus.CREATED).body(body);
        idempotencyService.store(idempotencyKey, request, response);
        return response;
    }

    @Transactional(readOnly = true)
    public TransferResponse getTransfer(Long transferId) {
        Long userId = SecurityUtils.currentUser().getUserId();
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found: " + transferId));

        Account source = accountRepository.findById(transfer.getSourceAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found"));
        Account destination = accountRepository.findById(transfer.getDestinationAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found"));

        if (!source.getUserId().equals(userId) && !destination.getUserId().equals(userId)) {
            throw new ForbiddenException("You are not allowed to view this transfer");
        }

        return toResponse(transfer);
    }

    private TransferResponse toResponse(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getSourceAccountId(),
                transfer.getDestinationAccountId(),
                transfer.getAmount(),
                transfer.getStatus(),
                transfer.getCreatedAt());
    }
}
