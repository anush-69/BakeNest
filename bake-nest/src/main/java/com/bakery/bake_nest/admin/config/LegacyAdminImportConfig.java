package com.bakery.bake_nest.admin.config;

import com.bakery.bake_nest.admin.service.LegacyAdminImporter;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "bakenest.import-legacy-admins", havingValue = "true")
public class LegacyAdminImportConfig {
    @Bean
    public ApplicationRunner importLegacyAdmins(LegacyAdminImporter importer) {
        return args -> importer.importAdmins();
    }
}
