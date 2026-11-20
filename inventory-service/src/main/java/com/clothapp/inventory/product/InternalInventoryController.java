package com.clothapp.inventory.product;

import com.clothapp.inventory.product.ProductDtos.ReserveResult;
import com.clothapp.inventory.product.ProductDtos.StockRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/inventory")
public class InternalInventoryController {

    private final ProductService service;

    public InternalInventoryController(ProductService service) {
        this.service = service;
    }

    @PostMapping("/reserve")
    public ReserveResult reserve(@Valid @RequestBody StockRequest request) {
        return service.reserve(request.items());
    }

    @PostMapping("/release")
    public void release(@Valid @RequestBody StockRequest request) {
        service.release(request.items());
    }

    @PostMapping("/commit")
    public void commit(@Valid @RequestBody StockRequest request) {
        service.commit(request.items());
    }
}
