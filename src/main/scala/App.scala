import com.spikerlabs.accounts.Api
import com.spikerlabs.accounts.storage.InMemoryStorage
import fs2.StreamApp
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.http4s.server.blaze.BlazeBuilder

object App extends StreamApp[Task] {

  val api = Api(InMemoryStorage())

  def stream(args: List[String], requestShutdown: monix.eval.Task[Unit]) =
    BlazeBuilder[monix.eval.Task]
      .bindHttp(8080, "0.0.0.0")
      .mountService(api.httpService, "/")
      .serve
}
