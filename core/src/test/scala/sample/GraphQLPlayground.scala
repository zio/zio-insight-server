package sample

import zio._

import caliban._
import caliban.GraphQL.graphQL
import caliban.interop.tapir.TapirAdapter
import caliban.schema.Annotations.GQLDescription
import sttp.tapir.json.zio._
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zhttp.http._
import zhttp.service.Server

sealed trait Role

object Role {
  case object SoftwareDeveloper       extends Role
  case object SiteReliabilityEngineer extends Role
  case object DevOps                  extends Role
}

case class Employee(
  name: String,
  role: Role)

case class EmployeesArgs(role: Role)
case class EmployeeArgs(name: String)

case class Queries(
  @GQLDescription("Return all employees with specific role")
  employees: EmployeesArgs => List[Employee],
  @GQLDescription("Find an employee by its name")
  employee: EmployeeArgs => Option[Employee])

object GraphQLPlayground extends ZIOAppDefault {

  val employees = List(
    Employee("Alex", Role.DevOps),
    Employee("Maria", Role.SoftwareDeveloper),
    Employee("James", Role.SiteReliabilityEngineer),
    Employee("Peter", Role.SoftwareDeveloper),
    Employee("Julia", Role.SiteReliabilityEngineer),
    Employee("Roberta", Role.DevOps),
  )

  val api = graphQL(
    RootResolver(
      Queries(
        args => employees.filter(_.role == args.role),
        args => employees.find(_.name == args.name),
      ),
    ),
  )

  val route: ZIO[Any, Throwable, Http[Any, Throwable, Request, Response]] =
    for {
      interpreter <- api.interpreter
      endpoint     = TapirAdapter.makeHttpService(interpreter)
      gql          = ZioHttpInterpreter().toHttp(endpoint)
    } yield Http.collectHttp { case _ -> !! / "graphql" => gql }

  val app = Http.collect[Request] { case Method.GET -> !! => Response.text("Hello World!") }

  override def run =
    for {
      gqlRoute <- route
      schema    = api.render
      _        <- ZIO.logInfo(schema)
      svr      <- Server
                    .start(
                      8080,
                      app ++ gqlRoute,
                    )
                    .forkDaemon
      _        <- Console.printLine(s"Started Insight Server ...")
      _        <- svr.join
    } yield ()
}
