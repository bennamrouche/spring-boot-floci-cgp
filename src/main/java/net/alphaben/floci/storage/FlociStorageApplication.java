package net.alphaben.floci.storage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.AccessToken;

@SpringBootApplication
public class FlociStorageApplication {
    @Bean
    CredentialsProvider googleCredentials() {
        return () -> GoogleCredentials.create(new AccessToken("floci", null));
    }

    @Bean
    @ConditionalOnMissingBean(Storage.class)
    Storage flociStorage(CredentialsProvider googleCredentials,
                         @org.springframework.beans.factory.annotation.Value("${spring.cloud.gcp.project-id}") String projectId,
                         @org.springframework.beans.factory.annotation.Value("${spring.cloud.gcp.storage.host}") String host) throws java.io.IOException {
        return StorageOptions.newBuilder()
                .setProjectId(projectId)
                .setHost(host)
                .setCredentials(googleCredentials.getCredentials())
                .build()
                .getService();
    }

    public static void main(String[] args) {
        SpringApplication.run(FlociStorageApplication.class, args);
    }
}
