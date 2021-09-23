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
    logger.info("\n\nRunning the converted examples from Java to Scala")

    // Run the converted examples (Java to Scala)
    ConvertedToScala.Start()

    logger.info("Finished execution.")

    logger.info("Now, running the simulations")

    // Run the simulations serially.
    val SaaSResult : String = SaaS.StartSimulation()
    val PaaSResult : String = PaaS.StartSimulation()
    val IaaSResult : String = IaaS.StartSimulation()

    System.out.println(SaaSResult)
    System.out.println(PaaSResult)
    System.out.println(IaaSResult)

    logger.info("Simulations execution done")

class Simulation