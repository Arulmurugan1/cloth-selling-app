package com.clothapp.inventory.batch;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/batch/products")
public class ProductBatchController {

    private static final String TEMPLATE = "sku,name,description,category,size,color,price,imageUrl,stockQty,defectiveQty\n"
            + "SKU-1001,Classic Cotton Shirt,Regular fit cotton shirt,Men Shirts,L,Blue,1299.00,https://picsum.photos/seed/sku1001/400,200,0\n"
            + "SKU-1002,Premium Linen Kurti,Soft breathable linen kurti,Women Kurtis,M,Green,1499.00,https://picsum.photos/seed/sku1002/400,180,0\n";

    private final ProductCsvBatchConfig batch;
    private final JobLauncher jobLauncher;
    private final Job productCsvImportJob;

    public ProductBatchController(ProductCsvBatchConfig batch,
                                  JobLauncher jobLauncher,
                                  Job productCsvImportJob) {
        this.batch = batch;
        this.jobLauncher = jobLauncher;
        this.productCsvImportJob = productCsvImportJob;
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=product-upload-template.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(TEMPLATE.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BatchImportResult upload(@RequestPart("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty");
        }
        Path tmp = Files.createTempFile("inventory-products-", ".csv");
        file.transferTo(tmp);
        return batch.runImport(jobLauncher, productCsvImportJob, tmp.toFile());
    }
}
