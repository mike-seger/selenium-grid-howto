package com.net128.test.selenium;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class PageTest {
	private static final Logger logger = LoggerFactory.getLogger(PageTest.class.getSimpleName());
	private static final Map<String, RemoteWebDriver> driverMap = new LinkedHashMap<>();
	private static Configuration configuration;
	private static File screenshotDir;

	@BeforeAll
	static void setup() throws Exception {
		configuration = Configuration.load();
		screenshotDir = new File(configuration.screenshotDestination);
		//noinspection ResultOfMethodCallIgnored
		screenshotDir.mkdirs();
		logger.info("Setting up drivers");
		addDriver(new ChromeOptions(), configuration.browsers.get("chrome").dimension);
		addDriver(new FirefoxOptions(), configuration.browsers.get("firefox").dimension);
		logger.info("Done setting up drivers: {}", driverMap.keySet());
	}

	private static void addDriver(Capabilities capabilities, Dimension dimension) {
		RemoteWebDriver driver = new RemoteWebDriver(configuration.hubUrl, capabilities);
		if(dimension!=null) {
			driver.manage().window().setSize(dimension);
		}
		driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(configuration.maxPageLoadDuration.getSeconds()));
		driverMap.put(capabilities.getBrowserName(), driver);
	}

	@AfterAll
	static void teardown() {
		logger.info("Quitting drivers: {}", driverMap.keySet());
		driverMap.values().forEach(RemoteWebDriver::quit);
		logger.info("Done quitting drivers");
	}

	@ParameterizedTest(name = "{index} {1}, {2}, {3}")
	@MethodSource
	public void testPage(RemoteWebDriver driver, String browserName, String pageUrl, String pageTitle) throws IOException {
		driver.get(pageUrl);
		assertThat(driver.getTitle()).matches(pageTitle);
		assertThat(takeScreenshot(driver,
		browserName+"-"+ pageTitle.toLowerCase()
				.replaceAll("[ \\\\/:.-]+", "_")
			)).exists();
	}

	@SuppressWarnings("unused")
	private static Stream<Arguments> testPage() {
		return driverMap.entrySet().stream().flatMap(entry ->
			configuration.pages.stream().map(page ->
				arguments(entry.getValue(), entry.getKey(), page.url, page.title)));
	}

	private String getDateString() {
		SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd-HHmmss.SSS");
		f.setTimeZone(TimeZone.getTimeZone("UTC"));
		return f.format(new Date());
	}

	private File takeScreenshot(RemoteWebDriver driver, String namePrefix) throws IOException {
		File destFile = new File(screenshotDir, namePrefix + "-" + getDateString() + ".png");
		Files.write(destFile.toPath(), driver.getScreenshotAs(OutputType.BYTES));
		return destFile;
	}
}
