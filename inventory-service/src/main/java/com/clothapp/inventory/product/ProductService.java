package com.clothapp.inventory.product;

import com.clothapp.inventory.common.ConflictException;
import com.clothapp.inventory.common.NotFoundException;
import com.clothapp.inventory.product.ProductDtos.ProductDto;
import com.clothapp.inventory.product.ProductDtos.ProductRequest;
import com.clothapp.inventory.product.ProductDtos.ReserveResult;
import com.clothapp.inventory.product.ProductDtos.ReservedLine;
import com.clothapp.inventory.product.ProductDtos.StockLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    public static final String CACHE = "products";

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    @Cacheable(cacheNames = CACHE, key = "'list:' + (#search == null ? '' : #search.trim().toLowerCase())")
    @Transactional(readOnly = true)
    public ArrayList<ProductDto> list(String search) {
        List<Product> found = (search == null || search.isBlank())
                ? repository.findAllByOrderByNameAsc()
                : repository.search("%" + escapeLike(search.trim().toLowerCase(Locale.ROOT)) + "%");
        log.info("Loaded {} products from database (search='{}')", found.size(), search);
        return found.stream().map(ProductDto::from).collect(Collectors.toCollection(ArrayList::new));
    }

    @Cacheable(cacheNames = CACHE, key = "'sku:' + #sku")
    @Transactional(readOnly = true)
    public ProductDto get(String sku) {
        return ProductDto.from(find(sku));
    }

    @CacheEvict(cacheNames = CACHE, allEntries = true)
    @Transactional
    public ProductDto create(ProductRequest req) {
        if (repository.existsBySku(req.sku())) {
            throw new ConflictException("SKU already exists: " + req.sku());
        }
        Product p = new Product(req.sku());
        p.applyDetails(req.name(), req.description(), req.category(), req.size(), req.color(), req.price(),
                req.imageUrl());
        p.setStock(req.stockQty() == null ? 0 : req.stockQty(), null);
        log.info("Created SKU {}", req.sku());
        return ProductDto.from(repository.save(p));
    }

    @CacheEvict(cacheNames = CACHE, allEntries = true)
    @Transactional
    public ProductDto update(String sku, ProductRequest req) {
        Product p = find(sku);
        p.applyDetails(req.name(), req.description(), req.category(), req.size(), req.color(), req.price(),
                req.imageUrl());
        if (req.stockQty() != null) {
            p.setStock(req.stockQty(), null);
        }
        log.info("Updated SKU {}", sku);
        return ProductDto.from(p);
    }

    @CacheEvict(cacheNames = CACHE, allEntries = true)
    @Transactional
    public ProductDto updateInventory(String sku, int stockQty, Integer defectiveQty) {
        Product p = find(sku);
        if (stockQty < p.getReservedQty()) {
            throw new ConflictException("Stock cannot be lower than reserved quantity (" + p.getReservedQty() + ")");
        }
        p.setStock(stockQty, defectiveQty);
        log.info("Inventory of SKU {} set to stock={} defective={}", sku, stockQty, p.getDefectiveQty());
        return ProductDto.from(p);
    }

    @CacheEvict(cacheNames = CACHE, allEntries = true)
    @Transactional
    public ProductDto setActive(String sku, boolean active) {
        Product p = find(sku);
        p.setActive(active);
        log.info("SKU {} active={}", sku, active);
        return ProductDto.from(p);
    }

    @CacheEvict(cacheNames = CACHE, allEntries = true)
    @Transactional
    public ProductDto setDefective(String sku, boolean defective) {
        Product p = find(sku);
        p.setDefective(defective);
        log.info("SKU {} defective={}", sku, defective);
        return ProductDto.from(p);
    }

    @CacheEvict(cacheNames = CACHE, allEntries = true)
    @Transactional
    public ReserveResult reserve(List<StockLine> items) {
        Map<String, Integer> wanted = merge(items);
        Map<String, Product> products = lock(wanted);
        List<String> problems = new ArrayList<>();
        List<ReservedLine> lines = new ArrayList<>();
        wanted.forEach((sku, qty) -> {
            Product p = products.get(sku);
            if (p == null) {
                problems.add(sku + ": not found");
            } else if (!p.isActive() || p.isDefective()) {
                problems.add(sku + ": not available for sale");
            } else if (p.availableQty() < qty) {
                problems.add(sku + ": only " + p.availableQty() + " in stock");
            } else {
                lines.add(new ReservedLine(sku, p.getName(), p.getPrice(), qty));
            }
        });
        if (!problems.isEmpty()) {
            log.warn("Reservation rejected: {}", problems);
            return new ReserveResult(false, problems, List.of());
        }
        lines.forEach(l -> products.get(l.sku()).reserve(l.qty()));
        log.info("Reserved stock for {} line(s)", lines.size());
        return new ReserveResult(true, List.of(), lines);
    }

    @CacheEvict(cacheNames = CACHE, allEntries = true)
    @Transactional
    public void release(List<StockLine> items) {
        Map<String, Integer> wanted = merge(items);
        lock(wanted).forEach((sku, p) -> p.release(wanted.get(sku)));
        log.info("Released stock for {} line(s)", wanted.size());
    }

    @CacheEvict(cacheNames = CACHE, allEntries = true)
    @Transactional
    public void commit(List<StockLine> items) {
        Map<String, Integer> wanted = merge(items);
        lock(wanted).forEach((sku, p) -> p.commit(wanted.get(sku)));
        log.info("Committed stock for {} line(s)", wanted.size());
    }

    private Map<String, Product> lock(Map<String, Integer> wanted) {
        return repository.lockBySkus(wanted.keySet()).stream()
                .collect(Collectors.toMap(Product::getSku, p -> p, (a, b) -> a, LinkedHashMap::new));
    }

    private Map<String, Integer> merge(List<StockLine> items) {
        Map<String, Integer> merged = new LinkedHashMap<>();
        items.forEach(i -> merged.merge(i.sku(), i.qty(), Integer::sum));
        return merged;
    }

    private Product find(String sku) {
        return repository.findBySku(sku).orElseThrow(() -> new NotFoundException("SKU not found: " + sku));
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
