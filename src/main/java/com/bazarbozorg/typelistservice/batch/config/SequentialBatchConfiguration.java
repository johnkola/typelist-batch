package com.bazarbozorg.typelistservice.batch.config;

import com.bazarbozorg.typelistservice.batch.BatchUtils;
import com.bazarbozorg.typelistservice.batch.listener.PerformanceMonitoringListener;
import com.bazarbozorg.typelistservice.client.model.TypeItemDTO;
import com.bazarbozorg.typelistservice.client.model.TypeListDTO;
import com.bazarbozorg.typelistservice.client.model.TypeSetDTO;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Optional;

@Configuration
@Profile({"sequential"})
public class SequentialBatchConfiguration {

    @Autowired
    private BatchUtils batchUtils;

    @Autowired
    private int chunkSize;

    @Autowired
    private TaskExecutor taskExecutor;

    @Bean
    @Qualifier("typeItemStep")
    public Step typeItemStep(JobRepository jobRepository,
                             PlatformTransactionManager transactionManager,
                             @Qualifier("typeItemReader") ItemReader<TypeItemDTO> reader,
                             @Qualifier("typeItemProcessor") ItemProcessor<TypeItemDTO, TypeItemDTO> processor,
                             @Qualifier("compositeTypeItemWriter") ItemWriter<TypeItemDTO> writer,
                             PerformanceMonitoringListener performanceMonitoringListener) {

        // Create synchronized reader for thread safety if it's a stream reader
        ItemReader<TypeItemDTO> syncReader = reader;
        if (reader instanceof ItemStreamReader) {
            syncReader = batchUtils.synchronizedItemReader((ItemStreamReader<TypeItemDTO>) reader);
        }

        return new StepBuilder("typeItemStep", jobRepository)
                .<TypeItemDTO, TypeItemDTO>chunk(chunkSize, transactionManager)
                .reader(syncReader)
                .processor(processor)
                .writer(writer)
                .taskExecutor(taskExecutor)  // Use common task executor 
                .faultTolerant()
                .skipPolicy(batchUtils.fileVerificationSkipper())
                .listener(Optional.ofNullable(performanceMonitoringListener))
                .build();
    }

    @Bean
    @Qualifier("typeListStep")
    public Step typeListStep(JobRepository jobRepository,
                             PlatformTransactionManager transactionManager,
                             @Qualifier("typeListReader") ItemReader<TypeListDTO> reader,
                             @Qualifier("typeListProcessor") ItemProcessor<TypeListDTO, TypeListDTO> processor,
                             @Qualifier("compositeTypeListWriter") ItemWriter<TypeListDTO> writer,
                             PerformanceMonitoringListener performanceMonitoringListener) {

        // Create synchronized reader for thread safety if it's a stream reader
        ItemReader<TypeListDTO> syncReader = reader;
        if (reader instanceof ItemStreamReader) {
            syncReader = batchUtils.synchronizedItemReader((ItemStreamReader<TypeListDTO>) reader);
        }

        return new StepBuilder("typeListStep", jobRepository)
                .<TypeListDTO, TypeListDTO>chunk(chunkSize, transactionManager)
                .reader(syncReader)
                .processor(processor)
                .writer(writer)
                .taskExecutor(taskExecutor)
                .faultTolerant()
                .skipPolicy(batchUtils.fileVerificationSkipper())
                .listener(Optional.ofNullable(performanceMonitoringListener))
                .build();
    }

    @Bean
    @Qualifier("typeSetStep")
    public Step typeSetStep(JobRepository jobRepository,
                            PlatformTransactionManager transactionManager,
                            @Qualifier("typeSetReader") ItemReader<TypeSetDTO> reader,
                            @Qualifier("typeSetProcessor") ItemProcessor<TypeSetDTO, TypeSetDTO> processor,
                            @Qualifier("compositeTypeSetWriter") ItemWriter<TypeSetDTO> writer,
                            PerformanceMonitoringListener performanceMonitoringListener) {

        // Create synchronized reader for thread safety if it's a stream reader
        ItemReader<TypeSetDTO> syncReader = reader;
        if (reader instanceof ItemStreamReader) {
            syncReader = batchUtils.synchronizedItemReader((ItemStreamReader<TypeSetDTO>) reader);
        }

        return new StepBuilder("typeSetStep", jobRepository)
                .<TypeSetDTO, TypeSetDTO>chunk(chunkSize, transactionManager)
                .reader(syncReader)
                .processor(processor)
                .writer(writer)
                .taskExecutor(taskExecutor)
                .faultTolerant()
                .skipPolicy(batchUtils.fileVerificationSkipper())
                .listener(Optional.ofNullable(performanceMonitoringListener))
                .build();
    }
}