import HelperUtils.{CreateLogger, ObtainConfigReference}
import Simulations.{BasicCloudSimPlusExample, BasicExample, LoggingExample}
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

object Simulation:
  val logger = CreateLogger(classOf[Simulation])

  @main def runSimulation =
    logger.info("Constructing a cloud model...")

//    LoggingExample.Start()
    BasicExample.Start()
//    BasicCloudSimPlusExample.Start()

    logger.info("Finished cloud simulation...")

class Simulation