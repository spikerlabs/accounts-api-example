import com.spikerlabs.accounts.{Api, Service}
import com.spikerlabs.accounts.storage.InMemoryStorage
import fs2.StreamApp
import monix.eval.Task

import monix.execution.Scheduler.Implicits.global

object App extends StreamApp[Task] {
  def stream(args: List[String], requestShutdown: Task[Unit]): fs2.Stream[Task, StreamApp.ExitCode] =
    Api.httpStream(Service(InMemoryStorage())).serve
}
