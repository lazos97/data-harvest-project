package com.dataharvest.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.dataharvest.service.ClientService;
import com.dataharvest.service.WebScrapingService;

@Controller
@RequestMapping("/")
public class MainController {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private WebScrapingService webScrapingService;

     @Autowired
    private ClientService clientService;

    @GetMapping
    public String home() {
        return "search";
    }

    @GetMapping("/search")
    @Transactional
    public String Search() {
        return "search";
    }
    
    @GetMapping("/searchById")
    public ModelAndView searchById(@RequestParam("id") String id) {
        var existingClient = clientService.findClientByUIC(id);

        if (existingClient.isPresent()) {
            var client = existingClient.get();
            ModelAndView modelAndView = new ModelAndView("search-results");
            modelAndView.addObject("searchType", "ID");
            modelAndView.addObject("searchValue", id);
            modelAndView.addObject("scrapedData", formatClientData(client)); 
            return modelAndView;
        } else {
            String result = webScrapingService.scrapeDataById(id);

            ModelAndView modelAndView = new ModelAndView("search-results");
            modelAndView.addObject("searchType", "ID");
            modelAndView.addObject("searchValue", id);
            modelAndView.addObject("scrapedData", result); 
            return modelAndView;
        }
    }

    @GetMapping("/searchByRegion")
    public ModelAndView searchByCompany(@RequestParam("company") String company) {
  
        String result = webScrapingService.scrapeDataUsingSearchBox(company);

        ModelAndView modelAndView = new ModelAndView("search-results");
        modelAndView.addObject("searchType", "Company");
        modelAndView.addObject("searchValue", company);
        modelAndView.addObject("scrapedData", result); 
        return modelAndView;
    }

    private String formatClientData(Object client) {
        return String.format("Client Number: %s\nCompany Name: %s\nRegistered Office: %s\nRepresentative: %s",
                             ((com.dataharvest.model.Client) client).getClientNumber(), ((com.dataharvest.model.Client) client).getCompanyName(), ((com.dataharvest.model.Client) client).getRegisteredOffice(), ((com.dataharvest.model.Client) client).getRepresentative());
    }
}
