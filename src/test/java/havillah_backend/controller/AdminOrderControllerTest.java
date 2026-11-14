package havillah_backend.controller;

import havillah_backend.dto.OrderItemResponse;
import havillah_backend.dto.OrderRequest;
import havillah_backend.dto.OrderResponse;
import havillah_backend.entity.OrderStatus;
import havillah_backend.security.CustomUserDetailsService;
import havillah_backend.security.JwtService;
import havillah_backend.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private UsernamePasswordAuthenticationToken adminPrincipal() {
        return new UsernamePasswordAuthenticationToken(
                "admin@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    private OrderResponse sampleOrder(OrderStatus status) {
        return new OrderResponse(
                10L,
                status,
                new BigDecimal("50000.00"),
                "12 Marina Road, Lagos",
                LocalDateTime.of(2026, 11, 14, 9, 0),
                List.of(new OrderItemResponse(
                        3L, "Wireless Headphones",
                        new BigDecimal("25000.00"), 2,
                        new BigDecimal("50000.00")
                ))
        );
    }

    @Test
    void getAllOrdersReturnsOrders() throws Exception {
        when(orderService.getAllOrders()).thenReturn(List.of(sampleOrder(OrderStatus.PENDING)));

        mockMvc.perform(get("/api/admin/orders").principal(adminPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].totalAmount").value(50000.00));

        verify(orderService).getAllOrders();
    }

    @Test
    void updateOrderStatusReturnsUpdatedOrder() throws Exception {
        when(orderService.updateStatus(eq(10L), any(OrderRequest.UpdateStatusRequest.class)))
                .thenReturn(sampleOrder(OrderStatus.SHIPPED));

        mockMvc.perform(patch("/api/admin/orders/10/status")
                        .principal(adminPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "SHIPPED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("SHIPPED"));

        verify(orderService).updateStatus(eq(10L), any(OrderRequest.UpdateStatusRequest.class));
    }

    @Test
    void updateOrderStatusReturnsBadRequestWhenStatusIsMissing() throws Exception {
        mockMvc.perform(patch("/api/admin/orders/10/status")
                        .principal(adminPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).updateStatus(anyLong(), any(OrderRequest.UpdateStatusRequest.class));
    }
}
