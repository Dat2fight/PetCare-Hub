package com.petcarehub;

import com.petcarehub.entity.*;
import com.petcarehub.service.InventoryService;
import com.petcarehub.service.impl.ProductServiceImpl;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties="spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@Import(ProductServiceImpl.class)
class ProductQueryTest {
    @Autowired TestEntityManager em;
    @Autowired ProductServiceImpl products;
    @MockBean InventoryService stock;
    @Test void threeStateFilterSearchAndBoundedQueryCount() {
        ProductCategory category=new ProductCategory(); category.setName("Test"); em.persist(category);
        for(int i=0;i<25;i++) {
            Product p=Product.builder().name(i==24?"Needle":"Product "+i).sku("query-"+i).category(category).price(BigDecimal.ONE).active(i!=24).build();
            em.persist(p); em.persist(Inventory.builder().product(p).quantity(2).build());
        }
        em.flush(); em.clear();
        var stats=em.getEntityManager().getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics(); stats.clear();
        var all=products.searchProducts(null,null,null,PageRequest.of(0,20));
        assertEquals(25,all.getTotalElements()); assertEquals(20,all.getContent().size());
        assertTrue(all.getContent().stream().allMatch(p->p.getStockQuantity()==2));
        assertTrue(stats.getPrepareStatementCount()<=3,"Expected page + count + batch stock, got "+stats.getPrepareStatementCount());
        assertEquals(24,products.searchProducts(null,true,null,PageRequest.of(0,20)).getTotalElements());
        assertEquals(1,products.searchProducts(null,false,null,PageRequest.of(0,20)).getTotalElements());
        assertEquals("Needle",products.searchProducts(category.getId(),null,"needle",PageRequest.of(0,20)).getContent().get(0).getName());
        assertTrue(products.searchProducts(null,true,"needle",PageRequest.of(0,20)).isEmpty());
    }
}
