package helloworld

import scala.concurrent.Future
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object Hello {
  def main(args: Array[String]) = {
    import com.example.clients.petstore.user.UserClient
    import scala.concurrent.ExecutionContext.Implicits.global

    val server = buildServer()

    implicit val actorSys = ActorSystem()
    implicit val materializer = ActorMaterializer()

    val userClient = UserClient.httpClient(server)
    val result = userClient.getUserByName("billg")

    System.out.println(result)
  }

  private def buildServer(): HttpRequest => Future[HttpResponse] = {
    import com.example.servers.petstore.user._
    import com.example.servers.petstore.{definitions => sdefs}
    import akka.http.scaladsl.server.Route
    import akka.http.scaladsl.settings.RoutingSettings

    implicit val actorSys = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val routingSettings = RoutingSettings(actorSys)

    Route.asyncHandler(
      UserResource.routes(new DummyUserHandler())
    )
  }
}

class DummyUserHandler
  extends com.example.servers.petstore.user.UserHandler {

  import com.example.servers.petstore.user._
  import com.example.servers.petstore.definitions._
  import scala.collection._
  import scala.concurrent.ExecutionContext.Implicits.global

  override def createUser(respond: UserResource.CreateUserResponse.type)(body: User): scala.concurrent.Future[UserResource.CreateUserResponse] = ???
  override def createUsersWithArrayInput(respond: UserResource.CreateUsersWithArrayInputResponse.type)(body: Vector[User]): scala.concurrent.Future[UserResource.CreateUsersWithArrayInputResponse] = ???
  override def createUsersWithListInput(respond: UserResource.CreateUsersWithListInputResponse.type)(body: Vector[User]): scala.concurrent.Future[UserResource.CreateUsersWithListInputResponse] = ???
  override def loginUser(respond: UserResource.LoginUserResponse.type)(username: String, password: String): scala.concurrent.Future[UserResource.LoginUserResponse] = ???
  override def logoutUser(respond: UserResource.LogoutUserResponse.type)(): scala.concurrent.Future[UserResource.LogoutUserResponse] = ???
  override def getUserByName(respond: UserResource.GetUserByNameResponse.type)(username: String): scala.concurrent.Future[UserResource.GetUserByNameResponse] = {
    val user = new User(
      id = Some(1234),
      username = Some(username),
      firstName = Some("First"),
      lastName = Some("Last"),
      email = Some(username + "@example.com"))
    Future { UserResource.GetUserByNameResponseOK(user) }
  }
  override def updateUser(respond: UserResource.UpdateUserResponse.type)(username: String, body: User): scala.concurrent.Future[UserResource.UpdateUserResponse] = ???
  override def deleteUser(respond: UserResource.DeleteUserResponse.type)(username: String): scala.concurrent.Future[UserResource.DeleteUserResponse] = ???
}
