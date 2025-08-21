package uk.gov.hmcts.appregister.test;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * TODO - REMOVE
 */
@RestController
public class TestController {

    @GetMapping("/test")
    public String loginSuccess() {
        return "You successfully logged in!";
    }
}
