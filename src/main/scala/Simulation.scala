import HelperUtils.{CreateLogger, ObtainConfigReference}
import Simulations.{ConvertedToScala, IaaS, PaaS, SaaS}
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

/**
 * The main simulator file. All the simulations are called from here. 
 * Please refer to the further comments for explaination.
 */

object Simulation:
  //Create a logger to display the messages on the console
  val logger = CreateLogger(classOf[Simulation])

  @main def runSimulations =
    logger.info("Run the simulations")

    // Run the simulations serially.
//    SaaS.StartSimulation()
//    PaaS.StartSimulation()
    IaaS.StartSimulation()

    logger.info("Simulations exeution done")

    logger.info("\n\nProceeding to run the converted examples from Java to Scala")

    // Run the converted examples (Java to Scala)
//    ConvertedToScala.Start()

    logger.info("Finished execution.")

class Simulation