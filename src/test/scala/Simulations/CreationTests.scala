package Simulations

import HelperUtils.CreateLogger
import ch.qos.logback.classic.Level
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudsimplus.util.Log
import org.scalatest.funsuite.AnyFunSuite

class CreationTests extends AnyFunSuite {
  val logger = CreateLogger(classOf[SaaS])
  Log.setLevel(Level.DEBUG)

  val simulation = new CloudSim()

  test("SaaS.createDatacenter") {
    val name = "datacenter0"

    val datacenter0 = SaaS.createDatacenter(0, simulation)

    logger.debug("Testing Datacenter is not null..")
    assert(datacenter0!=null)
  }

  test("PaaS Results number"){
    val numberofPES = 1000
    val pes = PaaS.createPes(numberofPES)
    logger.debug("Testing if createPes can handle creating a large number of PEs")
    assert(pes.length == numberofPES)
  }

  test("IaaS VM config check"){
    logger.debug("Testing if number of VMs created matches")
    val vmlist = IaaS.createVm(0)
    assert(vmlist.length == SaaS.config.getInt("IaaS.BrokerProperties.logic.num_vms"))
  }
}