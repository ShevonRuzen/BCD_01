package com.techmart.config;

import com.techmart.model.Product;
import com.techmart.user.User;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.jms.JMSConnectionFactoryDefinition;
import jakarta.jms.JMSDestinationDefinition;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.logging.Logger;

@DataSourceDefinition(
    name = "java:app/jdbc/TechMartDS",
    className = "com.mysql.cj.jdbc.MysqlXADataSource",
    url = "jdbc:mysql://localhost:3306/techmart_db?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC",
    user = "root",
    password = "Shehan@2002",
    properties = {
        "pinGlobalTxToPhysicalConnection=true"
    }
)
@JMSConnectionFactoryDefinition(
    name = "java:app/jms/ConnectionFactory",
    interfaceName = "jakarta.jms.ConnectionFactory"
)
@JMSDestinationDefinition(
    name = "java:app/jms/OrderQueue",
    interfaceName = "jakarta.jms.Queue",
    destinationName = "OrderQueue"
)
@JMSDestinationDefinition(
    name = "java:app/jms/NotificationQueue",
    interfaceName = "jakarta.jms.Queue",
    destinationName = "NotificationQueue"
)
@Singleton
@Startup
public class DatabaseConfig {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConfig.class.getName());

    @PersistenceContext(unitName = "TechMartPU")
    private EntityManager em;

    /**
     * Seeds default admin account and sample products after the schema has been
     * created/recreated by the JPA provider. This ensures the application is
     * immediately usable after deployment without any manual SQL script.
     */
    @PostConstruct
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void seedData() {
        try {
            seedAdmin();
            seedProducts();
            LOGGER.info("TechMart data seeding completed successfully.");
        } catch (Exception e) {
            LOGGER.warning("Data seeding skipped (data may already exist): " + e.getMessage());
        }
    }

    private void seedAdmin() {
        long adminCount = em.createQuery(
            "SELECT COUNT(u) FROM User u WHERE u.role = 'ADMIN'", Long.class
        ).getSingleResult();

        if (adminCount == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@techmart.com");
            admin.setPasswordHash("admin123");
            admin.setRole("ADMIN");
            admin.setActive(true);
            em.persist(admin);
            LOGGER.info("Default admin user created: admin@techmart.com / admin123");
        }

        // Also seed a sample customer so you can test customer pages
        long customerCount = em.createQuery(
            "SELECT COUNT(u) FROM User u WHERE u.email = 'customer@techmart.com'", Long.class
        ).getSingleResult();
        if (customerCount == 0) {
            User customer = new User();
            customer.setUsername("customer");
            customer.setEmail("customer@techmart.com");
            customer.setPasswordHash("customer123");
            customer.setRole("CUSTOMER");
            customer.setActive(true);
            em.persist(customer);
            LOGGER.info("Sample customer created: customer@techmart.com / customer123");
        }
    }

    private void seedProducts() {
        if (em.find(Product.class, "PROD_001") == null) {
            Product p1 = new Product();
            p1.setId("PROD_001");
            p1.setName("Enterprise SSD 1TB");
            p1.setPrice(199.99);
            p1.setStock(50000);
            em.persist(p1);
        }
        if (em.find(Product.class, "PROD_002") == null) {
            Product p2 = new Product();
            p2.setId("PROD_002");
            p2.setName("Mechanical Keyboard");
            p2.setPrice(89.99);
            p2.setStock(250);
            em.persist(p2);
        }
    }
}
