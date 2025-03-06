package com.wallet.walletdigital.controller;

import com.wallet.walletdigital.model.MovementWallet;
import com.wallet.walletdigital.service.MovementWalletService;
import io.reactivex.rxjava3.core.Single;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/movement-wallet")
public class MovementWalletController {
    private final MovementWalletService movementWalletService;
    public MovementWalletController(MovementWalletService movementWalletService) {
        this.movementWalletService = movementWalletService;
    }

    @GetMapping
    public Single<ResponseEntity<List<MovementWallet>>> getAll() {
        return movementWalletService.getAll()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    public Single<ResponseEntity<MovementWallet>> findById(@PathVariable String id) {
        return movementWalletService.findById(id)
                .map(ResponseEntity::ok);
    }

    @PostMapping
    public Single<ResponseEntity<MovementWallet>> save(@Valid @RequestBody MovementWallet movementWallet) {
        return movementWalletService.save(movementWallet)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    public Single<ResponseEntity<Void>> delete(@PathVariable String id) {
        return movementWalletService.deleteById(id)
                .map(deleted -> deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build());
    }

}
