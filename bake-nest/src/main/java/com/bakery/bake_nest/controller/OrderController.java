package com.bakery.bake_nest.controller;

import com.bakery.bake_nest.form.OrderForm;
import com.bakery.bake_nest.model.BakeryOrder;
import com.bakery.bake_nest.model.Product;
import com.bakery.bake_nest.service.OrderService;
import com.bakery.bake_nest.service.ProductService;
import java.util.List;
import java.util.ArrayList;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;
    private final ProductService productService;

    public OrderController(OrderService orderService, ProductService productService) {
        this.orderService = orderService;
        this.productService = productService;
    }

    @GetMapping("/new")
    public String showForm(Model model) {
        List<Product> products = productService.getAvailableProducts();
        OrderForm form = new OrderForm();
        for (Product product : products) {
            OrderForm.Item item = new OrderForm.Item();
            item.setProductId(product.getId());
            form.getItems().add(item);
        }
        model.addAttribute("orderForm", form);
        model.addAttribute("products", products);
        return "orders/orderCreate";
    }

    @PostMapping
    public String placeOrder(@ModelAttribute("orderForm") OrderForm form, BindingResult errors,
                             Model model, RedirectAttributes redirect, HttpSession session) {
        if (!errors.hasErrors()) {
            try {
                BakeryOrder order = orderService.createOrder(form);
                // Remember which orders belong to this guest browser.
                List<Long> orderIds = getGuestOrderIds(session);
                orderIds.add(order.getId());
                session.setAttribute("guestOrderIds", orderIds);
                // Redirect prevents a page refresh from submitting the order again.
                redirect.addFlashAttribute("orderNumber", order.getId());
                redirect.addFlashAttribute("total", order.getTotal());
                redirect.addFlashAttribute("paymentMethod", order.getPaymentMethod());
                return "redirect:/orders/created";
            } catch (IllegalArgumentException ex) {
                errors.reject("order.invalid", ex.getMessage());
            } catch (DataAccessException ex) {
                errors.reject("order.save", "The order could not be saved. Please try again.");
            }
        }
        model.addAttribute("products", productService.getAvailableProducts());
        return "orders/orderCreate";
    }

    @GetMapping
    public String showOrders(Model model, HttpSession session) {
        model.addAttribute("orders", orderService.getOrders(getGuestOrderIds(session)));
        return "orders/orderList";
    }

    @GetMapping("/{id}")
    public String showOrder(@PathVariable Long id, Model model, HttpSession session) {
        model.addAttribute("order", orderService.getOrder(id, getGuestOrderIds(session)));
        return "orders/orderDetails";
    }

    @SuppressWarnings("unchecked")
    private List<Long> getGuestOrderIds(HttpSession session) {
        List<Long> orderIds = (List<Long>) session.getAttribute("guestOrderIds");
        return orderIds == null ? new ArrayList<>() : new ArrayList<>(orderIds);
    }

    @GetMapping("/created")
    public String showConfirmation(Model model) {
        // Confirmation is shown only to the browser that just placed the order.
        if (!model.containsAttribute("orderNumber")) return "redirect:/orders/new";
        return "orders/orderConfirmation";
    }
}
