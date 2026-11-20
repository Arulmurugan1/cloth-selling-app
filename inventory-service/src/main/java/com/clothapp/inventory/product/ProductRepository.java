package com.clothapp.inventory.product;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    List<Product> findAllByOrderByNameAsc();

    @Query("""
            select p from Product p
            where lower(p.name) like :q escape '\\' or lower(p.sku) like :q escape '\\'
               or lower(coalesce(p.category, '')) like :q escape '\\'
               or lower(coalesce(p.color, '')) like :q escape '\\'
            order by p.name asc
            """)
    List<Product> search(@Param("q") String q);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.sku in :skus order by p.sku asc")
    List<Product> lockBySkus(@Param("skus") Collection<String> skus);
}
