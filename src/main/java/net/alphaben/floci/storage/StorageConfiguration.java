package net.alphaben.floci.storage;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfiguration {
    @Bean
    ApplicationRunner createBucketIfMissing(Storage storage, @Value("${app.storage.bucket}") String bucketName) {
        return args -> {
            Bucket bucket = storage.get(bucketName);
            if (bucket == null) {
                storage.create(BucketInfo.of(bucketName));
            }
        };
    }
}
