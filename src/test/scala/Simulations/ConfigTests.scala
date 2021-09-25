package Simulations

import Simulations.SaaS.config
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ConfigTests extends AnyFlatSpec with Matchers{
  behavior of "configuration parameters module in SaaS"
  it should "obtain the utilization ratio" in {config.getDouble("SaaS.utilizationRatio") should be < 1.0}
  it should "obtain the MIPS capacity" in {config.getLong("SaaS.CloudProviderProperties.host.mipsCapacity") shouldBe 10000}

  behavior of "configuration parameters module in PaaS"
  it should "obtain the utilization ratio" in {config.getDouble("PaaS.utilizationRatio") should be < 1.0}
  it should "obtain the VM Scheduler" in {config.getString("PaaS.CloudProviderProperties.logic.vmsch") shouldBe ("VmSchedulerTimeShared")}

  behavior of "configuration parameters module in IaaS"
  it should "obtain the utilization ratio" in {config.getDouble("IaaS.utilizationRatio") should be < 1.0}
  it should "Cloudlet PEs required should be less than Host PEs" in {config.getInt("IaaS.BrokerProperties.cloudlet.pes") should be <= config.getInt("IaaS.BrokerProperties.cloudlet.pes")}


}