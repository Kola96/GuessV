package com.guessv.config;

import com.guessv.service.DataImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataImportRunner implements ApplicationRunner {

    private final DataImportService dataImportService;
    private final DevDataSeeder devDataSeeder;
    private final PoolInitializer poolInitializer;

    @Override
    public void run(ApplicationArguments args) {
        dataImportService.importIfEmpty();
        devDataSeeder.seedIfEnabled();
        poolInitializer.initIfEmpty();
    }
}
