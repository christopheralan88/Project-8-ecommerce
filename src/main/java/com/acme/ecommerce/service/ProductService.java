package com.acme.ecommerce.service;

import com.acme.ecommerce.domain.Product;
import com.acme.ecommerce.exceptions.ProductNotFoundException;
import com.acme.ecommerce.exceptions.QuantityException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

	public Iterable<Product> findAll();
	
	public Page<Product> findAll(Pageable pageable);
	
	public Product findById(Long id) throws ProductNotFoundException;

	public Product findById(Long id, int quantity) throws QuantityException;
}
