package cloud.autotests.tests.demowebshop;

import cloud.autotests.config.demowebshop.App;
import cloud.autotests.helpers.AllureRestAssuredFilter;
import cloud.autotests.tests.TestBase;
import cloud.autotests.utils.TestDataGenerationUtils;
import com.codeborne.selenide.Configuration;
import com.github.javafaker.Faker;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.openqa.selenium.Cookie;

import java.util.Locale;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static io.qameta.allure.Allure.step;
import static io.restassured.RestAssured.given;

public class RegistrationTests extends TestBase {
    Faker faker = new Faker(new Locale("en"));

    String firstName,
            lastName,
            email,
            gender,
            password;

    final String tokenName = "__RequestVerificationToken";
    final String authCookieName = "NOPCOMMERCE.AUTH";


    @BeforeAll
    static void configureBaseUrl() {
        RestAssured.baseURI = App.config.apiUrl();
        Configuration.baseUrl = App.config.webUrl();
    }

    @BeforeEach
    void testDataGeneration() {
        testDataGenerate();
    }

    @Test
    @Tag("demowebshop")
    @DisplayName("Successful registration to demowebshop (UI)")
    void registrationByUITest() {
        step("Open registration page", () ->
                open("/register"));

        step("Fill registration form", () -> {
            $(".gender #gender-" + gender).click();
            $("#FirstName").setValue(firstName);
            $("#LastName").setValue(lastName);
            $("#Email").setValue(email);
            $("#Password").setValue(password);
            $("#ConfirmPassword").setValue(password);
            $("#register-button").click();
        });

        step("Verify successful authorization", () ->
                $(".registration-result-page").shouldHave(text("Your registration completed")));
    }

    @Test
    @Tag("demowebshop")
    @DisplayName("Successful authorization to demowebshop (API + UI)")
    void registrationByAPITest() {
        userRegistrationAndSetCookieStep();

        step("Open main page", () ->
                open(""));


        step("Verify successful authorization", () ->
                $(".account").shouldHave(text(email)));
    }

    @Test
    @Tag("demowebshop")
    @DisplayName("Changing user info (API + UI)")
    void userInfoChangeTest() {
        userRegistrationAndSetCookieStep();

        step("Open user profile page", () ->
                open("/customer/info"));

        step("Change user info", () -> {
            testDataGenerate();
            $(".gender #gender-" + gender).click();
            $("#FirstName").setValue(firstName);
            $("#LastName").setValue(lastName);
            $("#Email").setValue(email);
            $(".save-customer-info-button").click();
        });

        checkUserInfoStep();
    }

    private String userRegistration(String authCookieName) {

        Response register = given()
                .filter(AllureRestAssuredFilter.withCustomTemplates())
                .contentType(ContentType.JSON)
                .log().all()
                .when()
                .get("/register")
                .then()
                .log().all()
                .extract()
                .response();

        String token = register.htmlPath().getString("**.find{it.@name == '__RequestVerificationToken'}.@value");

        String authorizationCookie =
                given()
                        .filter(AllureRestAssuredFilter.withCustomTemplates())
                        .contentType("application/x-www-form-urlencoded; charset=UTF-8")
                        .formParam("Gender", gender.toUpperCase(Locale.ROOT).charAt(0))
                        .formParam("FirstName", firstName)
                        .formParam("LastName", lastName)
                        .formParam("Email", email)
                        .formParam("Password", password)
                        .formParam("ConfirmPassword", password)
                        .formParam(tokenName, token)
                        .cookies(register.cookies())
                        .when()
                        .post("/register")
                        .then()
                        .statusCode(302)
                        .extract()
                        .cookie(authCookieName);

        return authorizationCookie;
    }

    private void userRegistrationAndSetCookieStep() {
        step("New user registration", () -> {
            String authorizationCookie = userRegistration(authCookieName);

            step("Open minimal content, because cookie can be set when site is opened", () ->
                    open("/Themes/DefaultClean/Content/images/logo.png"));

            step("Set cookie to to browser", () ->
                    getWebDriver().manage().addCookie(
                            new Cookie(authCookieName, authorizationCookie)));
        });
    }

    private void checkUserInfoStep() {
        step("Check user info in profile", () -> {
            open("/customer/info");

            $(".account").shouldHave(text(email));

            $(".gender #gender-" + gender).shouldBe(selected);
            $("#FirstName").shouldHave(value(firstName));
            $("#LastName").shouldHave(value(lastName));
            $("#Email").shouldHave(value(email));
        });
    }

    private void testDataGenerate() {
        firstName = faker.name().firstName();
        lastName = faker.name().lastName();
        email = faker.internet().emailAddress();
        gender = TestDataGenerationUtils.genderGeneration();
        password = faker.internet().password();
    }
}
