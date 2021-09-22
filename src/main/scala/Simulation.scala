import HelperUtils.{CreateLogger, ObtainConfigReference}
import Simulations.{BasicExample, PaaS, SaaS}
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

object Simulation:
  val logger = CreateLogger(classOf[Simulation])

  @main def runSimulation =
    logger.info("Constructing a cloud model...")

    SaaS.StartSimulation()
    PaaS.StartSimulation()
//    BasicExample.Start()

    logger.info("Finished cloud simulation...")

class Simulation