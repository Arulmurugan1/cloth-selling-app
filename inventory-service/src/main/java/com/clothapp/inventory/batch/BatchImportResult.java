package com.clothapp.inventory.batch;

public record BatchImportResult(long readCount, long writeCount, long skipCount, String status) {
}
