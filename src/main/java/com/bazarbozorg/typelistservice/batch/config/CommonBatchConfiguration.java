package com.bazarbozorg.typelistservice.batch.config;

import com.bazarbozorg.typelistservice.batch.BatchUtils;
import com.bazarbozorg.typelistservice.batch.ProgressTrackingWriter;
import com.bazarbozorg.typelistservice.batch.listener.JobCompletionNotificationListener;
import com.bazarbozorg.typelistservice.batch.listener.PerformanceMonitoringListener;
import com.bazarbozorg.typelistservice.batch.processor.TypeItemProcessor;
import com.bazarbozorg.typelistservice.batch.processor.TypeListProcessor;
import com.bazarbozorg.typelistservice.batch.processor.TypeSetProcessor;
import com.bazarbozorg.typelistservice.batch.reader.FileReaderFactory;
import com.bazarbozorg.typelistservice.client.model.TypeItemDTO;
import com.bazarbozorg.typelistservice.client.model.TypeListDTO;
import com.bazarbozorg.typelistservice.client.model.TypeSetDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Arrays;

/**
 * Common configuration for batch processing shared between sequential and parallel implementations
 */
@Configuration
@EnableBatchProcessing
@Import(com.bazarbozorg.ApiClientConfiguration.class)
public class CommonBatchConfiguration {
    private static final Logger log = LoggerFactory.getLogger(CommonBatchConfiguration.class);

    @Value("${batch.chunk-size:1000}")
    private int chunkSize;

    @Value("${batch.max-threads:8}")
    private int maxThreads;

    @Autowired
    private BatchUtils batchUtils;

    @Autowired
    private FileReaderFactory fileReaderFactory;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /*
     * Processor beans are shared across configurations
     */
    @Bean
    @Qualifier("typeItemProcessor")
    public TypeItemProcessor typeItemProcessor() {
        return new TypeItemProcessor();
    }

    @Bean
    @Qualifier("typeListProcessor")
    public TypeListProcessor typeListProcessor() {
        return new TypeListProcessor();
    }

    @Bean
    @Qualifier("typeSetProcessor")
    public TypeSetProcessor typeSetProcessor() {
        return new TypeSetProcessor();
    }

    /**
     * Common task executor configuration for all batch processing
     */
    @Bean
    @Primary
    public TaskExecutor taskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("batch_executor_");
        executor.setConcurrencyLimit(maxThreads);
        return executor;
    }

    /**
     * Returns the configured chunk size
     */
    @Bean
    public int chunkSize() {
        return chunkSize;
    }

    /**
     * Creates a synchronized reader wrapper for thread safety
     */
    public <T> ItemReader<T> synchronizedReader(ItemReader<T> reader) {
        if (reader instanceof ItemStreamReader) {
            return batchUtils.synchronizedItemReader((ItemStreamReader<T>) reader);
        }
        return reader;
    }

    /**
     * Creates a progress tracking writer for TypeItem
     */
    @Bean
    @Qualifier("progressTrackingTypeItemWriter")
    public ItemWriter<TypeItemDTO> progressTrackingTypeItemWriter(
            @Qualifier("typeItemWriter") ItemWriter<TypeItemDTO> typeItemWriter) {
        return new ProgressTrackingWriter<>(typeItemWriter);
    }

    /**
     * Creates a progress tracking writer for TypeList
     */
    @Bean
    @Qualifier("progressTrackingTypeListWriter")
    public ItemWriter<TypeListDTO> progressTrackingTypeListWriter(
            @Qualifier("typeListWriter") ItemWriter<TypeListDTO> typeListWriter) {
        return new ProgressTrackingWriter<>(typeListWriter);
    }

    /**
     * Creates a progress tracking writer for TypeSet
     */
    @Bean
    @Qualifier("progressTrackingTypeSetWriter")
    public ItemWriter<TypeSetDTO> progressTrackingTypeSetWriter(
            @Qualifier("typeSetWriter") ItemWriter<TypeSetDTO> typeSetWriter) {
        return new ProgressTrackingWriter<>(typeSetWriter);
    }

    /**
     * Creates a composite writer for TypeItem that includes progress tracking
     */
    @Bean
    @Qualifier("compositeTypeItemWriter")
    public ItemWriter<TypeItemDTO> compositeTypeItemWriter(
            @Qualifier("progressTrackingTypeItemWriter") ItemWriter<TypeItemDTO> progressWriter,
            @Qualifier("typeItemWriter") ItemWriter<TypeItemDTO> typeItemWriter) {
        CompositeItemWriter<TypeItemDTO> compositeWriter = new CompositeItemWriter<>();
        compositeWriter.setDelegates(Arrays.asList(progressWriter, typeItemWriter));
        return compositeWriter;
    }

    /**
     * Creates a composite writer for TypeList that includes progress tracking
     */
    @Bean
    @Qualifier("compositeTypeListWriter")
    public ItemWriter<TypeListDTO> compositeTypeListWriter(
            @Qualifier("progressTrackingTypeListWriter") ItemWriter<TypeListDTO> progressWriter,
            @Qualifier("typeListWriter") ItemWriter<TypeListDTO> typeListWriter) {
        CompositeItemWriter<TypeListDTO> compositeWriter = new CompositeItemWriter<>();
        compositeWriter.setDelegates(Arrays.asList(progressWriter, typeListWriter));
        return compositeWriter;
    }

    /**
     * Creates a composite writer for TypeSet that includes progress tracking
     */
    @Bean
    @Qualifier("compositeTypeSetWriter")
    public ItemWriter<TypeSetDTO> compositeTypeSetWriter(
            @Qualifier("progressTrackingTypeSetWriter") ItemWriter<TypeSetDTO> progressWriter,
            @Qualifier("typeSetWriter") ItemWriter<TypeSetDTO> typeSetWriter) {
        CompositeItemWriter<TypeSetDTO> compositeWriter = new CompositeItemWriter<>();
        compositeWriter.setDelegates(Arrays.asList(progressWriter, typeSetWriter));
        return compositeWriter;
    }

    /**
     * Common step definition for TypeItem processing
     */
    @Bean
    @Qualifier("baseTypeItemStep")
    public Step baseTypeItemStep(
            @Qualifier("typeItemReader") ItemReader<TypeItemDTO> reader,
            @Qualifier("typeItemProcessor") TypeItemProcessor processor,
            @Qualifier("compositeTypeItemWriter") ItemWriter<TypeItemDTO> writer) {

        ItemReader<TypeItemDTO> syncReader = synchronizedReader(reader);

        return new StepBuilder("typeItemStep", jobRepository)
                .<TypeItemDTO, TypeItemDTO>chunk(chunkSize, transactionManager)
                .reader(syncReader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skipPolicy(batchUtils.fileVerificationSkipper())
                .build();
    }

    /**
     * Common step definition for TypeList processing
     */
    @Bean
    @Qualifier("baseTypeListStep")
    public Step baseTypeListStep(
            @Qualifier("typeListReader") ItemReader<TypeListDTO> reader,
            @Qualifier("typeListProcessor") TypeListProcessor processor,
            @Qualifier("compositeTypeListWriter") ItemWriter<TypeListDTO> writer) {

        ItemReader<TypeListDTO> syncReader = synchronizedReader(reader);

        return new StepBuilder("typeListStep", jobRepository)
                .<TypeListDTO, TypeListDTO>chunk(chunkSize, transactionManager)
                .reader(syncReader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skipPolicy(batchUtils.fileVerificationSkipper())
                .build();
    }

    /**
     * Common step definition for TypeSet processing
     */
    @Bean
    @Qualifier("baseTypeSetStep")
    public Step baseTypeSetStep(
            @Qualifier("typeSetReader") ItemReader<TypeSetDTO> reader,
            @Qualifier("typeSetProcessor") TypeSetProcessor processor,
            @Qualifier("compositeTypeSetWriter") ItemWriter<TypeSetDTO> writer) {

        ItemReader<TypeSetDTO> syncReader = synchronizedReader(reader);

        return new StepBuilder("typeSetStep", jobRepository)
                .<TypeSetDTO, TypeSetDTO>chunk(chunkSize, transactionManager)
                .reader(syncReader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skipPolicy(batchUtils.fileVerificationSkipper())
                .build();
    }

    @Bean
    public Job importDataJob(JobRepository jobRepository,
                             @Qualifier("typeItemStep") Step typeItemStep,
                             @Qualifier("typeListStep") Step typeListStep,
                             @Qualifier("typeSetStep") Step typeSetStep,
                             JobCompletionNotificationListener jobCompletionNotificationListener,
                             PerformanceMonitoringListener performanceMonitoringListener) {
        return new JobBuilder("importDataJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jobCompletionNotificationListener)
                .listener(performanceMonitoringListener)
                .start(typeListStep)  // First process TypeLists
                .next(typeItemStep)   // Then process TypeItems
                .next(typeSetStep)    // Finally process TypeSets (which may reference TypeItems)
                .build();
    }


}