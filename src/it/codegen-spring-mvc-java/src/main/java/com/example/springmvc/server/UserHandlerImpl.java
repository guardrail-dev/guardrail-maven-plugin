package com.example.springmvc.server;

import com.example.springmvc.server.petstore.definitions.User;
import com.example.springmvc.server.petstore.user.UserHandler;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Service
public class UserHandlerImpl implements UserHandler {
    @Override
    public CompletionStage<CreateUserResponse> createUser(User body) {
        return CompletableFuture.completedFuture(CreateUserResponse.Ok);
    }

    @Override
    public CompletionStage<CreateUsersWithArrayInputResponse> createUsersWithArrayInput(List<User> body) {
        return null;
    }

    @Override
    public CompletionStage<CreateUsersWithListInputResponse> createUsersWithListInput(List<User> body) {
        return null;
    }

    @Override
    public CompletionStage<LoginUserResponse> loginUser(String username, String password) {
        return CompletableFuture.completedFuture(LoginUserResponse.Ok("This login is fine."));
    }

    @Override
    public CompletionStage<LogoutUserResponse> logoutUser() {
        return CompletableFuture.completedFuture(LogoutUserResponse.Ok);
    }

    @Override
    public CompletionStage<GetUserByNameResponse> getUserByName(String username) {
        final User user = new User.Builder()
                        .withFirstName("Barack")
                        .withLastName("Obama")
                        .withEmail("barack.obama@example.com")
                        .build();
        return CompletableFuture.completedFuture(GetUserByNameResponse.Ok(user));
    }

    @Override
    public CompletionStage<UpdateUserResponse> updateUser(String username, User body) {
        return null;
    }

    @Override
    public CompletionStage<DeleteUserResponse> deleteUser(String username) {
        return null;
    }
}
