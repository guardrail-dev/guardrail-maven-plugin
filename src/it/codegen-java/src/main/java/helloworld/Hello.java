
package helloworld;

import com.example.clients.petstore.definitions.User;
import com.example.clients.petstore.user.GetUserByNameResponse;
import com.example.clients.petstore.user.UserClient;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class Hello {
  private UserClient client = new UserClient.Builder().build();

  public CompletionStage<GetUserByNameResponse> createUser(final String username) {
      String email = username + "@example.com";
      User u = new User.Builder()
              .withUsername(username)
              .withEmail(email)
              .withFirstName(Optional.of("Barack"))
              .withLastName(Optional.of("Obama"))
              .build();
      return client.createUser(u).call().thenCompose(
              createUserResp -> client.getUserByName(username).call());
  }
}
