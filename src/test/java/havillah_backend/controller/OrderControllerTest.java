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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private OrderResponse sampleOrder() {
        return new OrderResponse(
                10L,
                OrderStatus.PENDING,
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
    void checkoutReturnsCreatedOrder() throws Exception {
        when(orderService.checkout(eq("customer@example.com"), any(OrderRequest.CheckoutRequest.class)))
                .thenReturn(sampleOrder());

        mockMvc.perform(post("/api/orders")
                        .principal(new UsernamePasswordAuthenticationToken("customer@example.com", null, List.of()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shippingAddress": "12 Marina Road, Lagos"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(50000.00));

        verify(orderService).checkout(eq("customer@example.com"), any(OrderRequest.CheckoutRequest.class));
    }

    @Test
    void checkoutReturnsBadRequestWhenShippingAddressIsBlank() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .principal(new UsernamePasswordAuthenticationToken("customer@example.com", null, List.of()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shippingAddress": ""
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).checkout(anyString(), any(OrderRequest.CheckoutRequest.class));
    }

    @Test
    void getMyOrdersReturnsCurrentUsersOrders() throws Exception {
        when(orderService.getMyOrders("customer@example.com")).thenReturn(List.of(sampleOrder()));

        mockMvc.perform(get("/api/orders")
                        .principal(new UsernamePasswordAuthenticationToken("customer@example.com", null, List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].shippingAddress").value("12 Marina Road, Lagos"));

        verify(orderService).getMyOrders("customer@example.com");
    }

    @Test
    void getMyOrderReturnsRequestedOrder() throws Exception {
        when(orderService.getMyOrder("customer@example.com", 10L)).thenReturn(sampleOrder());

        mockMvc.perform(get("/api/orders/10")
                        .principal(new UsernamePasswordAuthenticationToken("customer@example.com", null, List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.items[0].productName").value("Wireless Headphones"));

        verify(orderService).getMyOrder("customer@example.com", 10L);
    }

    @Test
    void cancelOrderReturnsCancelledOrder() throws Exception {
        OrderResponse cancelledOrder = new OrderResponse(
                10L, OrderStatus.CANCELLED, new BigDecimal("50000.00"),
                "12 Marina Road, Lagos",
                LocalDateTime.of(2026, 11, 14, 9, 0),
                sampleOrder().items()
        );
        when(orderService.cancelMyOrder("customer@example.com", 10L)).thenReturn(cancelledOrder);

        mockMvc.perform(put("/api/orders/10/cancel")
                        .principal(new UsernamePasswordAuthenticationToken("customer@example.com", null, List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(orderService).cancelMyOrder("customer@example.com", 10L);
    }
}
