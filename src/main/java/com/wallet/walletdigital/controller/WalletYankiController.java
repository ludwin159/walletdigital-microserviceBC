package com.wallet.walletdigital.controller;

import com.wallet.walletdigital.model.WalletYanki;
import com.wallet.walletdigital.service.WalletYankiService;
import io.reactivex.rxjava3.core.Single;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/wallet")
public class WalletYankiController {
    private final WalletYankiService walletYankiService;
    public WalletYankiController(WalletYankiService walletYankiService) {
        this.walletYankiService = walletYankiService;
    }

    @GetMapping
    public Single<ResponseEntity<List<WalletYanki>>> getAll() {
        return walletYankiService.getAll()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    public Single<ResponseEntity<WalletYanki>> findById(@PathVariable String id) {
        return walletYankiService.findById(id)
                .map(wallet -> wallet != null ? ResponseEntity.ok(wallet) : ResponseEntity.notFound().build());
    }

    @PostMapping
    public Single<ResponseEntity<WalletYanki>> save(@Valid @RequestBody WalletYanki wallet) {
        return walletYankiService.save(wallet)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    public Single<ResponseEntity<Void>> delete(@PathVariable String id) {
        return walletYankiService.deleteById(id)
                .map(deleted -> deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build());
    }
    @PostMapping("/associate")
    public Single<ResponseEntity<WalletYanki>> associateToDebitCard(@RequestBody WalletYanki walletYanki) {
        return walletYankiService.assignDebitCard(walletYanki)
                .map(ResponseEntity::ok);
    }

}
