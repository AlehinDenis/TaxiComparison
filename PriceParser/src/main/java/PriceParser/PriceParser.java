package main.java.PriceParser;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.net.PortProber;


import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;

public class PriceParser {
    private String addressFrom;
    private String addressTo;
    private int timeOut;
    private WebDriver browser;
    private WebDriverWait wait;

    public PriceParser(String city, String streetFrom, String streetTo) {
        this.addressFrom = city + " " + streetFrom;
        this.addressTo = city + " " + streetTo;
    }

    public List<Pair<String, String>> run(int timeOut) {
        this.timeOut = timeOut;
        setUpEnvironment();
        List<Pair<String, String>> result = parseWebsite(timeOut);

        browser.close();
        return result;
    }


    private void setUpEnvironment() {
        /*System.setProperty("webdriver.chrome.driver", "chromedriver.exe");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");*/
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setBrowserName("chrome");
        capabilities.setVersion("80.0");
        capabilities.setCapability("enableVNC", true);
        capabilities.setCapability("enableVideo", false);

        try {
            browser = new RemoteWebDriver(
                    new URL("http://35.245.43.143:4444/wd/hub"),
                    capabilities);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        browser.get("https://taxinf.ru/");
        //browser.navigate().to("https://taxinf.ru/");
        //browser.manage().window().maximize();
        wait = new WebDriverWait(browser, 30);
    }


    private List<Pair<String, String>> parseWebsite(int timeOut) {
        enterData(timeOut);
        return getData();
    }

    private void enterData(int timeOut) {
        try {
            WebElement addressFromElement = wait.until(presenceOfElementLocated(By.xpath("//input[@placeholder='Адрес откуда']")));
            WebElement addressToElement = wait.until(presenceOfElementLocated(By.xpath("//input[@placeholder='Адрес куда']")));
            Thread.sleep(timeOut);
            addressFromElement.sendKeys(addressFrom);
            Thread.sleep(timeOut);
            wait.until(presenceOfElementLocated(By.xpath("//div[@class='section-search_form_input_option']"))).click();
            Thread.sleep(timeOut);
            addressToElement.sendKeys(addressTo);
            Thread.sleep(timeOut);
            wait.until(presenceOfElementLocated(By.xpath("//div[@class='section-search_form_input_option']"))).click();
            Thread.sleep(timeOut);
            wait.until(presenceOfElementLocated(By.xpath("//button[@class='section-search_form_button base-button --green']"))).click();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private List<Pair<String, String>> getData() {
        wait.until(presenceOfElementLocated(By.xpath("//div[@class='section-search_result_items']")));

        try {
            Thread.sleep(timeOut);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        List<WebElement> taxi =
                browser.findElements(By.xpath("//div[@class='section-search_result_item_servis']/a[1]/h3"));
        List<WebElement> price =
                browser.findElements(By.xpath("//div[@class='section-search_result_item_servis']/a[2]"));

        List<Pair<String, String>> result = new ArrayList<>();
        for(int i = 0; i < taxi.size(); i++) {
            result.add(new Pair<String, String>(taxi.get(i).getText(), price.get(i).getText()));
        }

        return result;
    }
}
