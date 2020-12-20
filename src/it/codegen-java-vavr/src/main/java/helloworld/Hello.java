
package helloworld;

import com.example.demo.vavr.petstore.definitions.User;
import com.example.demo.vavr.petstore.user.CreateUserResponse;
import com.example.demo.vavr.petstore.user.UserClient;

import io.vavr.control.Option;
import io.vavr.concurrent.Future;

public class Hello {
  private UserClient client = new UserClient.Builder().build();

  public Future<CreateUserResponse> createUser(final String username) {
      String email = username + "@example.com";
      User u = new User.Builder()
              .withUsername(username)
              .withEmail(email)
              .withFirstName(Option.of("Barack"))
              .withLastName(Option.of("Obama"))
              .build();
      return client.createUser(u).call();
  }
}
