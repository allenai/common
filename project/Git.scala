import sbt._
import Keys._

object Git {
  protected def executableName(command: String) = {
    val maybeOsName = sys.props.get("os.name").map(_.toLowerCase)
    val maybeIsWindows = maybeOsName.filter(_.contains("windows"))
    maybeIsWindows.map(_ => command+".exe").getOrElse(command)
  }

  val commandName = "git"
  private lazy val exec = executableName(commandName)
  def cmd(args: Any*): ProcessBuilder = Process(exec +: args.map(_.toString))

  def describe() = cmd("describe") !!
}
