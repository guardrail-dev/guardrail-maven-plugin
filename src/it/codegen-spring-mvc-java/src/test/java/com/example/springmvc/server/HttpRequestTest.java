package com.example.springmvc.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
                classes = HelloApplication.class)
public class HttpRequestTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void userEndpointReturnsStatus200() throws Exception {
        given()
                .baseUri("http://localhost:" + port)
        .when()
                .get("/v2/user/obama")
        .then()
                .statusCode(200)
                .contentType("application/json")
                .body("firstName", equalTo("Barack"),
                        "lastName", equalTo("Obama"),
                        "email", equalTo("barack.obama@example.com"));
    }
}
