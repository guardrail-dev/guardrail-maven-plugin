
package helloworld;

import com.example.clients.petstore.definitions.User;
import com.example.clients.petstore.user.GetUserByNameResponse;
import com.example.clients.petstore.user.UserClient;

import io.vavr.control.Option;
import io.vavr.concurrent.Future;

public class Hello {
  private UserClient client = new UserClient.Builder().build();

  public Future<GetUserByNameResponse> createUser(final String username) {
      String email = username + "@example.com";
      User u = new User.Builder()
              .withUsername(username)
              .withEmail(email)
              .withFirstName(Optional.of("Barack"))
              .withLastName(Optional.of("Obama"))
              .build();
      return client.createUser(u);
  }
}
