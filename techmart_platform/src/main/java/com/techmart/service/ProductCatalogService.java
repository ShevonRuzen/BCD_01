package com.techmart.service;

import com.techmart.model.Product;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class ProductCatalogService {

    @PersistenceContext(unitName = "TechMartPU")
    private EntityManager em;

    public List<Product> getAllProducts() {
        return em.createQuery("SELECT p FROM Product p ORDER BY p.id", Product.class)
                .getResultList();
    }

    public Product getProductById(String id) {
        return em.find(Product.class, id);
    }

    public Product createProduct(String id, String name, double price, int stock) {
        if (id == null || id.isBlank() || getProductById(id) != null) {
            return null;
        }
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(price);
        product.setStock(stock);
        em.persist(product);
        return product;
    }

    public Product updateProduct(String id, String name, Double price, Integer stock) {
        Product product = getProductById(id);
        if (product == null) {
            return null;
        }
        if (name != null && !name.isBlank()) {
            product.setName(name);
        }
        if (price != null) {
            product.setPrice(price);
        }
        if (stock != null) {
            product.setStock(stock);
        }
        return em.merge(product);
    }

    public boolean deleteProduct(String id) {
        Product product = getProductById(id);
        if (product == null) {
            return false;
        }
        em.remove(em.contains(product) ? product : em.merge(product));
        return true;
    }
}