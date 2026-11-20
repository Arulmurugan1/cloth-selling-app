package com.clothapp.inventory.batch;

import com.clothapp.inventory.product.Product;
import com.clothapp.inventory.product.ProductRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.File;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Configuration
public class ProductCsvBatchConfig {

    public static final String JOB_NAME = "productCsvImportJob";

    @Bean
    public Job productCsvImportJob(JobRepository jobRepository,
                                   @Qualifier("productCsvImportStep") Step step) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(step)
                .build();
    }

    @Bean(name = "productCsvImportStep")
    public Step productCsvImportStep(JobRepository jobRepository,
                                     PlatformTransactionManager tx,
                                     ProductRepository repository,
                                     FlatFileItemReader<CsvProductRow> productReader) {
        return new StepBuilder("productCsvImportStep", jobRepository)
                .<CsvProductRow, CsvProductRow>chunk(100, tx)
                .reader(productReader)
                .processor(row -> {
                    if (row.getSku() == null || row.getSku().isBlank()) {
                        return null;
                    }
                    if (row.getPrice() == null) {
                        row.setPrice(BigDecimal.ZERO);
                    }
                    if (row.getStockQty() == null) {
                        row.setStockQty(0);
                    }
                    if (row.getDefectiveQty() == null) {
                        row.setDefectiveQty(0);
                    }
                    return row;
                })
                .writer(items -> {
                    List<Product> upsert = new ArrayList<>();
                    for (CsvProductRow row : items) {
                        Product p = repository.findBySku(row.getSku()).orElseGet(() -> new Product(row.getSku()));
                        p.applyDetails(row.getName(), row.getDescription(), row.getCategory(), row.getSize(),
                                row.getColor(), row.getPrice(), row.getImageUrl());
                        p.setStock(row.getStockQty(), row.getDefectiveQty());
                        p.setDefective(row.getDefectiveQty() != null && row.getDefectiveQty() > 0);
                        upsert.add(p);
                    }
                    repository.saveAll(upsert);
                })
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(100)
                .build();
    }

    @Bean
    @org.springframework.batch.core.configuration.annotation.StepScope
    public FlatFileItemReader<CsvProductRow> productReader(
            @org.springframework.beans.factory.annotation.Value("#{jobParameters['csvPath']}") String csvPath) {
        FlatFileItemReader<CsvProductRow> reader = new FlatFileItemReaderBuilder<CsvProductRow>()
                .name("productCsvReader")
                .resource(new FileSystemResource(csvPath == null ? "" : csvPath))
                .linesToSkip(1)
                .build();

        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("sku", "name", "description", "category", "size", "color", "price", "imageUrl",
                "stockQty", "defectiveQty");
        tokenizer.setStrict(false);

        BeanWrapperFieldSetMapper<CsvProductRow> mapper = new BeanWrapperFieldSetMapper<>();
        mapper.setTargetType(CsvProductRow.class);

        DefaultLineMapper<CsvProductRow> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(mapper);
        reader.setLineMapper(lineMapper);
        return reader;
    }

    public BatchImportResult runImport(JobLauncher launcher, Job job, File file) throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addString("csvPath", file.getAbsolutePath())
                .addString("requestId", UUID.randomUUID().toString())
                .addLong("requestedAt", Instant.now().toEpochMilli())
                .toJobParameters();
        JobExecution execution = launcher.run(job, params);
        var step = execution.getStepExecutions().stream().findFirst().orElseThrow();
        return new BatchImportResult(step.getReadCount(), step.getWriteCount(), step.getSkipCount(),
                execution.getStatus().name());
    }
}
