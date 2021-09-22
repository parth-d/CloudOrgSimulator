import HelperUtils.{CreateLogger, ObtainConfigReference}
import Simulations.{BasicExample, IaaS, PaaS, SaaS}
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

object Simulation:
  val logger = CreateLogger(classOf[Simulation])

  @main def runSimulation =
    logger.info("Constructing a cloud model...")

    SaaS.StartSimulation()
    PaaS.StartSimulation()
    IaaS.StartSimulation()
//    BasicExample.Start()

    logger.info("Finished cloud simulation...")

class Simulation