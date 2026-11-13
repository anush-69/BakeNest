package com.bakery.bake_nest.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Profile("booking-local")
@RequestMapping("/cake-bookings/live")
public class CakeBookingPageController {
    @GetMapping({"", "/"})
    public String list() {
        return "cake-bookings/live/list";
    }

    @GetMapping("/new")
    public String create(Model model) {
        model.addAttribute("editing", false);
        return "cake-bookings/live/form";
    }

    @GetMapping("/{bookingId}")
    public String details(@PathVariable("bookingId") Long bookingId, Model model) {
        model.addAttribute("bookingId", bookingId);
        return "cake-bookings/live/details";
    }

    @GetMapping("/{bookingId}/edit")
    public String edit(@PathVariable("bookingId") Long bookingId, Model model) {
        model.addAttribute("editing", true);
        model.addAttribute("bookingId", bookingId);
        return "cake-bookings/live/form";
    }
}
