
package helloworld;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.example.clients.petstore.definitions.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.example.clients.petstore.user.GetUserByNameResponse;

public class HelloTest {
    @Test
    public void happyPath() throws Exception {
        final String username = randomUsername();
        CompletableFuture<GetUserByNameResponse> future = new Hello().createUser(username).toCompletableFuture();
        GetUserByNameResponse response = future.get(10, TimeUnit.SECONDS);
        response.fold(this::handleOk, this::handleBadRequest, this::handleNotFound);
    }
    
    private Object handleOk(User user) {
        assertThat(user.getEmail().isPresent()).isTrue();
        assertThat(user.getUsername().isPresent()).isTrue();
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
