
package helloworld;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import io.vavr.control.Option;
import io.vavr.concurrent.Future;

import com.example.demo.vavr.petstore.definitions.User;
import com.example.demo.vavr.petstore.user.CreateUserResponse;
import com.example.demo.vavr.petstore.user.GetUserByNameResponse;

public class HelloTest {
    @Test
    public void happyPath() throws Exception {
        final String username = randomUsername();
        Future<CreateUserResponse> future = new Hello().createUser(username);
//        GetUserByNameResponse response = future.get(10, TimeUnit.SECONDS);
//        response.fold(this::handleOk, this::handleBadRequest, this::handleNotFound);
    }

    private Object handleOk(User user) {
        assertThat(user.getEmail().isDefined()).isTrue();
        assertThat(user.getUsername().isDefined()).isTrue();
        assertThat(user.equals(user)).isTrue();
        return user;
    }

    private Object handleBadRequest() {
     fail("bad request");
     return null;
    }

    private Object handleNotFound() {
        fail("not found");
        return null;
    }

    private String randomUsername() {
        return "user-" + UUID.randomUUID().toString();
    }
}
