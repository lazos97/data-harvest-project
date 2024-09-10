package com.dataharvest.service;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.dataharvest.model.Client;

import java.time.Duration;

@Service
public class WebScrapingService {

    @Autowired
    private ClientService clientService;

    public String scrapeDataUsingSearchBox(String id) {
        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        String result = "";

        try {
            driver.get("https://newregister.bcci.bg/edipub/");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10)); // 10 seconds wait

            WebElement languageMenu = wait.until(ExpectedConditions.elementToBeClickable(By.id("bLang")));
            languageMenu.click();

            WebElement searchBox = wait
                    .until(ExpectedConditions.elementToBeClickable(By.id("RegistrationAreaId_chosen")));
            searchBox.click();

            WebElement searchInput = wait
                    .until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".chosen-search input")));
            searchInput.sendKeys(id);

            WebElement option = wait
                    .until(ExpectedConditions.elementToBeClickable(By.cssSelector(".chosen-results li")));
            option.click();

            WebElement searchButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("bSearch")));
            searchButton.click();

            WebElement firstCheckbox = wait
                    .until(ExpectedConditions.elementToBeClickable(By.cssSelector("input[type='checkbox']")));
            firstCheckbox.click();

            WebElement generateButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("bGenerate")));
            generateButton.click();

            WebElement reportDiv = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("report")));
            String reportText = reportDiv.getText();

            String clientNumber = extractClientNumber(reportText);
            String companyName = extractCompanyName(reportText);
            String registeredOffice = extractRegisteredOffice(reportText);
            String representative = extractRepresentative(reportText);
            String uic = extractUic(reportText);

            var existingClient = clientService.findClientByUIC(uic);
            if (!existingClient.isPresent()) {
                Client newClient = new Client(clientNumber, companyName, registeredOffice, representative, uic);
                clientService.insertClient(newClient);
            }

            result = String.format("Client Number: %s\nCompany Name: %s\nRegistered Office: %s\nRepresentative: %s",
                    clientNumber, companyName, registeredOffice, representative);
        } catch (Exception e) {
            e.printStackTrace();
            result = "Error occurred: Company didn't found!!!";
        } finally {
            driver.quit();
        }

        return result;
    }

    public String scrapeDataById(String id) {
        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        String result = "";

        try {
            driver.get("https://newregister.bcci.bg/edipub/");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10)); // 10 seconds wait

            WebElement languageMenu = wait.until(ExpectedConditions.elementToBeClickable(By.id("bLang")));
            languageMenu.click();

            WebElement idInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("EIK")));
            idInput.sendKeys(id);

            WebElement searchButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("bSearch")));
            searchButton.click();

            WebElement firstCheckbox = wait
                    .until(ExpectedConditions.elementToBeClickable(By.cssSelector("input[type='checkbox']")));
            firstCheckbox.click();

            WebElement generateButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("bGenerate")));
            generateButton.click();

            WebElement reportDiv = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("report")));
            String reportText = reportDiv.getText();

            String clientNumber = extractClientNumber(reportText);
            String companyName = extractCompanyName(reportText);
            String registeredOffice = extractRegisteredOffice(reportText);
            String representative = extractRepresentative(reportText);
            String uic = extractUic(reportText);

            Client newClient = new Client(clientNumber, companyName, registeredOffice, representative, uic);
            clientService.insertClient(newClient);

            result = String.format("Client Number: %s\nCompany Name: %s\nRegistered Office: %s\nRepresentative: %s",
                    clientNumber, companyName, registeredOffice, representative);
        } catch (Exception e) {
            e.printStackTrace();
            result = "Error occurred: Company didn't found!!!";
        } finally {
            driver.quit();
        }

        return result;
    }

    private String extractClientNumber(String text) {
        String prefix = "Client number: ";
        int startIndex = text.indexOf(prefix);

        if (startIndex != -1) {
            startIndex += prefix.length();
            int endIndex = text.indexOf("\n", startIndex);
            if (endIndex == -1) {
                endIndex = text.length();
            }
            return text.substring(startIndex, endIndex).trim();
        }
        return "";
    }

    private String extractCompanyName(String text) {
        // Extract the company name which always follows the client number
        String clientNumber = extractClientNumber(text);
        int clientNumberIndex = text.indexOf(clientNumber);

        if (clientNumberIndex != -1) {
            // Extract the substring starting just after the client number
            String substringAfterClientNumber = text.substring(clientNumberIndex + clientNumber.length());

            // The company name starts with a quote and ends with a dash
            int startIndex = substringAfterClientNumber.indexOf("\"");
            if (startIndex != -1) {
                startIndex += 1; 
                int endIndex = substringAfterClientNumber.indexOf("\"", startIndex);
                if (endIndex != -1) {
                    return substringAfterClientNumber.substring(startIndex, endIndex).trim();
                }
            }
        }
        return "";
    }

    private String extractRegisteredOffice(String text) {
        String[] parts = text.split("\n");
        StringBuilder office = new StringBuilder();
        boolean isOfficeSection = false;
        for (String part : parts) {
            if (part.startsWith("Registered office:")) {
                isOfficeSection = true;
            } else if (isOfficeSection && part.trim().isEmpty()) {
                break;
            }
            if (isOfficeSection) {
                office.append(part).append("\n");
            }
        }
        return office.toString().trim();
    }

    public static String extractRepresentative(String reportText) {
        String regex = "Representative according to registration:[^<]*(<br>[^<]*)*";
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(reportText);

        if (matcher.find()) {
            String representativeSection = matcher.group();
            representativeSection = representativeSection.replaceAll("<[^>]+>", "").trim();
            return representativeSection;
        }
        return null;
    }

    private String extractUic(String text) {
        String[] parts = text.split("\n");
        for (String part : parts) {
            if (part.startsWith("UIC/Bulstat: ")) {
                return part.substring("UIC/Bulstat: ".length()).trim();
            }
        }
        return "";
    }
}
