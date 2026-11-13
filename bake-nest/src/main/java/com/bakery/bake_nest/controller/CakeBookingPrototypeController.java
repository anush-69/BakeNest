package com.bakery.bake_nest.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

/** Displays the prototype only; no booking is submitted or persisted. */
@Controller
@RequestMapping("/cake-bookings/preview")
public class CakeBookingPrototypeController {

    // Both GET addresses display the same form without accessing the database.
    @GetMapping({"", "/", "/new"})
    public String showForm(Model model) {
        model.addAttribute("activePage", "form");
        return "cake-bookings/form";
    }

    // Immutable fictional fixtures are presentation data, unrelated to database entities.
    private static final List<DemoBooking> DEMO_BOOKINGS = List.of(
            new DemoBooking(1001, "Birthday cake (example)", "Medium (example)",
                    "Chocolate (example)", "Cream icing with cocoa swirls and small stars.",
                    "Happy Birthday, Alex!", "2026-12-12", "Not provided", "REQUESTED (example)"),
            new DemoBooking(1002, "Celebration cake (example)", "Small (example)",
                    "Vanilla (example)", "Soft brown ribbon detail with simple cream flowers.",
                    "Congratulations!", "2026-12-20", "https://example.invalid/demo-cake.jpg", "IN REVIEW (example)")
    );

    // The query parameter demonstrates an empty list without changing any data.
    @GetMapping("/bookings")
    public String showBookings(@RequestParam(defaultValue = "false") boolean empty, Model model) {
        model.addAttribute("activePage", "bookings");
        model.addAttribute("emptyDemo", empty);
        model.addAttribute("bookings", empty ? List.of() : DEMO_BOOKINGS);
        return "cake-bookings/list";
    }

    @GetMapping("/bookings/{id}")
    public String showDetails(@PathVariable long id, Model model) {
        DemoBooking booking = DEMO_BOOKINGS.stream().filter(item -> item.id() == id).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demo booking not found"));
        model.addAttribute("activePage", "bookings");
        model.addAttribute("booking", booking);
        return "cake-bookings/details";
    }

    // Admin preview reuses the same fictional fixtures, without adding authorization rules or writes.
    @GetMapping("/admin")
    public String showAdmin(Model model) {
        model.addAttribute("activePage", "admin");
        model.addAttribute("bookings", DEMO_BOOKINGS);
        return "cake-bookings/admin";
    }

    // A view-only record keeps the prototype independent of persistence and authentication.
    public record DemoBooking(long id, String cakeType, String cakeSize, String flavour,
                              String designDescription, String cakeMessage, String requiredDate,
                              String referenceImageUrl, String status) { }
}
