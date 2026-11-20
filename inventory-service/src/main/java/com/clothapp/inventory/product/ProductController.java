package com.clothapp.inventory.product;

import com.clothapp.inventory.product.ProductDtos.ActiveUpdate;
import com.clothapp.inventory.product.ProductDtos.DefectiveUpdate;
import com.clothapp.inventory.product.ProductDtos.InventoryUpdate;
import com.clothapp.inventory.product.ProductDtos.ProductDto;
import com.clothapp.inventory.product.ProductDtos.ProductRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProductDto> list(@RequestParam(required = false) String search) {
        return service.list(search);
    }

    @GetMapping("/{sku}")
    public ProductDto get(@PathVariable String sku) {
        return service.get(sku);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto create(@Valid @RequestBody ProductRequest request) {
        return service.create(request);
    }

    @PutMapping("/{sku}")
    public ProductDto update(@PathVariable String sku, @Valid @RequestBody ProductRequest request) {
        return service.update(sku, request);
    }

    @PatchMapping("/{sku}/inventory")
    public ProductDto inventory(@PathVariable String sku, @Valid @RequestBody InventoryUpdate body) {
        return service.updateInventory(sku, body.stockQty(), body.defectiveQty());
    }

    @PatchMapping("/{sku}/active")
    public ProductDto active(@PathVariable String sku, @Valid @RequestBody ActiveUpdate body) {
        return service.setActive(sku, body.active());
    }

    @PatchMapping("/{sku}/defective")
    public ProductDto defective(@PathVariable String sku, @Valid @RequestBody DefectiveUpdate body) {
        return service.setDefective(sku, body.defective());
    }
}
