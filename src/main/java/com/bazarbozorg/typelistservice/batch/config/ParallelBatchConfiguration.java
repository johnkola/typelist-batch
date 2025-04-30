package com.bazarbozorg.typelistservice.batch.config;

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
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.MultiResourcePartitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.File;
import java.io.IOException;

/**
 * Configuration for parallel batch processing
 * - Supports processing multiple files in parallel
 * - Supports partitioning a single large file
 */
@Configuration
@Profile("parallel")
public class ParallelBatchConfiguration {
    private static final Logger log = LoggerFactory.getLogger(ParallelBatchConfiguration.class);

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private FileReaderFactory fileReaderFactory;

    @Autowired
    private TypeItemProcessor typeItemProcessor;

    @Autowired
    private TypeListProcessor typeListProcessor;

    @Autowired
    private TypeSetProcessor typeSetProcessor;

    @Autowired
    @Qualifier("compositeTypeItemWriter")
    private ItemWriter<TypeItemDTO> typeItemWriter;

    @Autowired
    @Qualifier("compositeTypeListWriter")
    private ItemWriter<TypeListDTO> typeListWriter;

    @Autowired
    @Qualifier("compositeTypeSetWriter")
    private ItemWriter<TypeSetDTO> typeSetWriter;

    @Autowired
    private TaskExecutor taskExecutor;

    @Autowired
    private int chunkSize;

    /**
     * Configure a job for processing multiple files in parallel
     */
    @Bean
    public Job parallelTypeListJob(JobCompletionNotificationListener listener,
                                   PerformanceMonitoringListener performanceListener) {
        return new JobBuilder("parallelTypeListJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .listener(performanceListener)
                .start(splitFlow())
                .build()
                .build();
    }

    /**
     * Configure parallel flow for processing multiple files
     */
    @Bean
    public Flow splitFlow() {
        return new FlowBuilder<SimpleFlow>("splitFlow")
                .split(taskExecutor)
                .add(typeListFlow(), typeItemFlow(), typeSetFlow())
                .build();
    }

    /**
     * Define TypeList parallel flow
     */
    @Bean
    public Flow typeListFlow() {
        return new FlowBuilder<SimpleFlow>("typeListFlow")
                .start(typeListFileStep())
                .build();
    }

    /**
     * Define TypeItem parallel flow
     */
    @Bean
    public Flow typeItemFlow() {
        return new FlowBuilder<SimpleFlow>("typeItemFlow")
                .start(typeItemFileStep())
                .build();
    }

    /**
     * Define TypeSet parallel flow
     */
    @Bean
    public Flow typeSetFlow() {
        return new FlowBuilder<SimpleFlow>("typeSetFlow")
                .start(typeSetFileStep())
                .build();
    }

    /**
     * Configure a step for processing TypeList files
     */
    @Bean
    public Step typeListFileStep() {
        return new StepBuilder("typeListFileStep", jobRepository)
                .<TypeListDTO, TypeListDTO>chunk(chunkSize, transactionManager)
                .reader(synchronizedTypeListReader())
                .processor(typeListProcessor)
                .writer(typeListWriter)
                .taskExecutor(taskExecutor)
                .faultTolerant()
                .skipLimit(10)
                .skip(Exception.class)
                .build();
    }

    /**
     * Configure a step for processing TypeItem files
     */
    @Bean
    public Step typeItemFileStep() {
        return new StepBuilder("typeItemFileStep", jobRepository)
                .<TypeItemDTO, TypeItemDTO>chunk(chunkSize, transactionManager)
                .reader(synchronizedTypeItemReader())
                .processor(typeItemProcessor)
                .writer(typeItemWriter)
                .taskExecutor(taskExecutor)
                .faultTolerant()
                .skipLimit(10)
                .skip(Exception.class)
                .build();
    }

    /**
     * Configure a step for processing TypeSet files
     */
    @Bean
    public Step typeSetFileStep() {
        return new StepBuilder("typeSetFileStep", jobRepository)
                .<TypeSetDTO, TypeSetDTO>chunk(chunkSize, transactionManager)
                .reader(synchronizedTypeSetReader())
                .processor(typeSetProcessor)
                .writer(typeSetWriter)
                .taskExecutor(taskExecutor)
                .faultTolerant()
                .skipLimit(10)
                .skip(Exception.class)
                .build();
    }

    /**
     * Create synchronized readers for each type
     */
    @Bean
    @StepScope
    public ItemReader<TypeListDTO> synchronizedTypeListReader() {
        try {
            String filePath = System.getProperty("typelist.file.path", "input/typelists.json");
            ItemReader<TypeListDTO> reader = fileReaderFactory.createTypeListReader(new File(filePath));
            return reader;
        } catch (Exception e) {
            log.error("Error creating TypeList reader", e);
            return null;
        }
    }

    @Bean
    @StepScope
    public ItemReader<TypeItemDTO> synchronizedTypeItemReader() {
        try {
            String filePath = System.getProperty("typeitem.file.path", "input/typeitems.json");
            ItemReader<TypeItemDTO> reader = fileReaderFactory.createTypeItemReader(new File(filePath));
            return reader;
        } catch (Exception e) {
            log.error("Error creating TypeItem reader", e);
            return null;
        }
    }

    @Bean
    @StepScope
    public ItemReader<TypeSetDTO> synchronizedTypeSetReader() {
        try {
            String filePath = System.getProperty("typeset.file.path", "input/typesets.json");
            ItemReader<TypeSetDTO> reader = fileReaderFactory.createTypeSetReader(new File(filePath));
            return reader;
        } catch (Exception e) {
            log.error("Error creating TypeSet reader", e);
            return null;
        }
    }

    /**
     * Configure job for partitioned processing of files in a directory
     */
    @Bean
    public Job partitionedTypeItemJob(JobCompletionNotificationListener listener,
                                      PerformanceMonitoringListener performanceListener) {
        return new JobBuilder("partitionedTypeItemJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .listener(performanceListener)
                .start(partitionTypeItemStep())
                .build();
    }

    /**
     * Configure the master step for partitioning TypeItem processing
     */
    @Bean
    public Step partitionTypeItemStep() {
        return new StepBuilder("partitionTypeItemStep", jobRepository)
                .partitioner("typeItemSlaveStep", typeItemPartitioner())
                .partitionHandler(typeItemPartitionHandler())
                .build();
    }

    /**
     * Configure the partitioner for TypeItem files
     */
    @Bean
    @StepScope
    public MultiResourcePartitioner typeItemPartitioner() {
        MultiResourcePartitioner partitioner = new MultiResourcePartitioner();

        try {
            // Find all files matching pattern
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("file:input/typeitems_*.json");
            partitioner.setResources(resources);
            partitioner.setKeyName("typeItemFileName");

            log.info("Partitioning TypeItem job across {} files", resources.length);
        } catch (IOException e) {
            log.error("Error finding TypeItem partition resources", e);
        }

        return partitioner;
    }

    /**
     * Configure the partition handler for TypeItem processing
     */
    @Bean
    public PartitionHandler typeItemPartitionHandler() {
        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();
        handler.setTaskExecutor(taskExecutor);
        handler.setStep(typeItemSlaveStep());
        handler.setGridSize(10); // Set appropriate grid size
        return handler;
    }

    /**
     * Configure the slave step for TypeItem partitioning
     */
    @Bean
    public Step typeItemSlaveStep() {
        return new StepBuilder("typeItemSlaveStep", jobRepository)
                .<TypeItemDTO, TypeItemDTO>chunk(chunkSize, transactionManager)
                .reader(partitionTypeItemReader(null))
                .processor(typeItemProcessor)
                .writer(typeItemWriter)
                .faultTolerant()
                .skipLimit(10)
                .skip(Exception.class)
                .build();
    }

    /**
     * Create reader for a TypeItem partition
     */
    @Bean
    @StepScope
    public ItemReader<TypeItemDTO> partitionTypeItemReader(
            @Value("#{stepExecutionContext[typeItemFileName]}") String fileName) {
        try {
            if (fileName == null) {
                log.warn("No filename provided for partition reader");
                return null;
            }
            return fileReaderFactory.createTypeItemReader(new File(fileName));
        } catch (Exception e) {
            log.error("Error creating reader for TypeItem partition: {}", fileName, e);
            return null;
        }
    }
}