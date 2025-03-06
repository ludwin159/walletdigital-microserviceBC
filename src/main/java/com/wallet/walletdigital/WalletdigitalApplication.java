package com.wallet.walletdigital;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class WalletdigitalApplication {

	public static void main(String[] args) {
		SpringApplication.run(WalletdigitalApplication.class, args);
	}

}
