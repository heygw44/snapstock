package com.snapstock;

import org.springframework.boot.SpringApplication;

public class TestSnapstockApplication {

	public static void main(String[] args) {
		SpringApplication.from(SnapstockApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
