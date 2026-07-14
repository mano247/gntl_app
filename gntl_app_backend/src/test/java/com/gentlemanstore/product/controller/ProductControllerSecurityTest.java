package com.gentlemanstore.product.controller;

import com.gentlemanstore.discount.controller.DiscountController;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Role su deklarativne (@PreAuthorize) — ovi testovi garantuju da mutirajuci
 * product/discount/promotion endpointi ne dozvoljavaju CUSTOMER rolu,
 * a da su dostupni EMPLOYEE roli tamo gde je to poslovno pravilo.
 */
public class ProductControllerSecurityTest {

    private String preAuthorizeOf(Class<?> controller, String methodName) {
        Method method = Arrays.stream(controller.getDeclaredMethods())
                .filter(m -> m.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Method not found: " + methodName));
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation, methodName + " must be protected with @PreAuthorize");
        return annotation.value();
    }

    @Test
    void customerCannotAccessProductCrudEndpoints() {
        for (String method : new String[]{"createProduct", "updateProduct", "deleteProduct", "restoreProduct"}) {
            String rules = preAuthorizeOf(ProductController.class, method);
            assertFalse(rules.contains("CUSTOMER"), method + " must not allow CUSTOMER");
        }
    }

    @Test
    void employeeHasFullProductCrud() {
        for (String method : new String[]{"createProduct", "updateProduct", "deleteProduct", "restoreProduct"}) {
            String rules = preAuthorizeOf(ProductController.class, method);
            assertTrue(rules.contains("EMPLOYEE"), method + " must allow EMPLOYEE");
        }
    }

    @Test
    void ticketArchiveAndStaffUnreadSummaryAreStaffOnly() {
        for (String method : new String[]{"archiveTicket", "getStaffUnreadSummary"}) {
            String rules = preAuthorizeOf(com.gentlemanstore.support.controller.SupportController.class, method);
            assertFalse(rules.contains("CUSTOMER"), method + " must not allow CUSTOMER");
            assertTrue(rules.contains("EMPLOYEE"), method + " must allow EMPLOYEE");
        }
    }

    @Test
    void customerCannotManageDiscountsOrPromotions() {
        for (String method : new String[]{"createDiscount", "deleteDiscount", "createPromotion", "deletePromotion"}) {
            String rules = preAuthorizeOf(DiscountController.class, method);
            assertFalse(rules.contains("CUSTOMER"), method + " must not allow CUSTOMER");
        }
    }

    @Test
    void customerCanValidatePromoCode() {
        String rules = preAuthorizeOf(DiscountController.class, "validatePromoCode");
        assertTrue(rules.contains("CUSTOMER"));
    }
}
