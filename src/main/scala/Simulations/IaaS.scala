package Simulations

import HelperUtils.CreateLogger
import ch.qos.logback.classic.Level
import com.typesafe.config.ConfigFactory
import org.cloudbus.cloudsim.allocationpolicies.{VmAllocationPolicy, VmAllocationPolicyBestFit}
import org.cloudbus.cloudsim.brokers.{DatacenterBroker, DatacenterBrokerSimple}
import org.cloudbus.cloudsim.cloudlets.{Cloudlet, CloudletSimple}
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.datacenters.{Datacenter, DatacenterSimple}
import org.cloudbus.cloudsim.hosts.{Host, HostSimple}
import org.cloudbus.cloudsim.resources.{Pe, PeSimple}
import org.cloudbus.cloudsim.schedulers.cloudlet.{CloudletScheduler, CloudletSchedulerSpaceShared, CloudletSchedulerTimeShared}
import org.cloudbus.cloudsim.schedulers.vm.{VmSchedulerSpaceShared, VmSchedulerTimeShared}
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic
import org.cloudbus.cloudsim.vms.{Vm, VmCost, VmSimple}
import org.cloudsimplus.builders.tables.CloudletsTableBuilder
import org.cloudsimplus.util.Log

import collection.JavaConverters.*
import java.util.Comparator
import scala.collection.mutable.ListBuffer

class IaaS

object IaaS:
  val config = ConfigFactory.load("Simulations.conf")
  val logger = CreateLogger(classOf[IaaS]);
  val results = ListBuffer.empty[Double]
  var vmsch = "VmSchedulerTimeShared"
  var cloudletsch = "CloudletSchedulerTimeShared"

  def StartSimulation() : Unit = {
    System.out.println("Currently executing IaaS")
    for (i <- 0 to 2){
      Start(i)
    }

    cloudletsch = "CloudletSchedulerSpaceShared"
    for (i <- 0 to 2){
      Start(i)
    }

    vmsch = "VmSchedulerSpaceShared"
    cloudletsch = "CloudletSchedulerTimeShared"
    for (i <- 0 to 2){
      Start(i)
    }

    cloudletsch = "CloudletSchedulerTimeShared"
    for (i <- 0 to 2){
      Start(i)
    }
    println("\n\nResult:\nThe minimum cost (IaaS) required to execute the required cloudlets is " + results.toList.min)
  }

  def Start(simulation_number : Int) : Unit = {
//    System.out.println("\n\nCurrent Config:")
//    System.out.println("\t Cloudlet Scheduler: " + cloudletsch)
//    System.out.println("\t VM Scheduler: " + vmsch)
//    System.out.println("\t VM Config Code: " + simulation_number)
    val simulation : CloudSim = new CloudSim();
    val broker : DatacenterBroker = new DatacenterBrokerSimple(simulation);
    val datacenter : Datacenter = createDatacenter(simulation_number, simulation)
    val vmList : List[Vm] = createVm(simulation_number)
    val cloudletList : List[Cloudlet] = createCloudlets()
    broker.submitCloudletList(cloudletList.asJava)
    broker.submitVmList(vmList.asJava)
    configureLogs();
    simulation.start();
    val finishedCloudlets: List[Cloudlet] = broker.getCloudletFinishedList.asScala.toList;
    val vmComparator: Comparator[Cloudlet] = Comparator.comparingLong((c: Cloudlet) => c.getVm.getId)

//        new CloudletsTableBuilder(finishedCloudlets.asJava).build()

//        printTotalVmsCost(broker)
    var vm_cost_sum : Double = 0
    for (vm <- broker.getVmCreatedList.asScala){
      val vm_cost = new VmCost(vm)
      vm_cost_sum = vm_cost_sum + vm_cost.getTotalCost
    }
    results.addOne(vm_cost_sum)
  }

  private def createDatacenter(simulation_number: Int, simulation: CloudSim) : Datacenter = {
    val datacenterName : String = "datacenter" + simulation_number.toString
    val datacenterPath : String = "IaaS.CloudProviderProperties." + datacenterName + "."

    val num_hosts : Int = config.getInt(datacenterPath + "hosts")
    val hostList : List[Host] = List(createHost(simulation_number))
    val arch            = config.getString(datacenterPath + "arch")
    val os              = config.getString(datacenterPath + "os")
    val vmm             = config.getString(datacenterPath + "vmm")
    val time_zone       = config.getDouble(datacenterPath + "time_zone")
    val cost            = config.getDouble(datacenterPath + "cost")
    val costPerMem      = config.getDouble(datacenterPath + "cpm")
    val costPerStorage  = config.getDouble(datacenterPath + "cps")
    val costPerBw       = config.getDouble(datacenterPath + "cpb")

    val dc = new DatacenterSimple(simulation, hostList.asJava)
    dc.getCharacteristics
      .setVmm(vmm).setOs(os)
      .setArchitecture(arch)
      .setCostPerBw(costPerBw)
      .setCostPerMem(costPerMem)
      .setCostPerStorage(costPerStorage)
      .setCostPerSecond(cost)
    dc.setVmAllocationPolicy(new VmAllocationPolicyBestFit)
    dc.setName("datacenter" + simulation_number.toString)
    return dc
  }

  private def createHost(simulation_number : Int) : Host = {
    val Host_RAM : Int      = config.getInt("IaaS.CloudProviderProperties.host" + simulation_number + ".RAMInMBs")
    val Host_BW : Int       = config.getInt("IaaS.CloudProviderProperties.host" + simulation_number + ".BandwidthInMBps")
    val Host_Storage : Int  = config.getInt("IaaS.CloudProviderProperties.host" + simulation_number + ".StorageInMBs")
    val Host_Pes : Int      = config.getInt("IaaS.CloudProviderProperties.host" + simulation_number + ".Pes")
    val peList : List[Pe]   = createPes(Host_Pes, simulation_number)
    val vm_sch = vmsch
    if (vm_sch == "VmSchedulerTimeShared"){
      return new HostSimple(Host_RAM, Host_BW, Host_Storage, peList.asJava).setVmScheduler(new VmSchedulerTimeShared());
    }
    else {
      return new HostSimple(Host_RAM, Host_BW, Host_Storage, peList.asJava).setVmScheduler(new VmSchedulerSpaceShared())
    }
  }

  private def createPes(num: Int, simulation_number : Int): List[Pe] ={
    val pes = ListBuffer.empty[Pe]

    createPes(num, pes, simulation_number)

    return pes.toList
  }

  private def createPes(num: Int, listbuffer: ListBuffer[Pe], simulation_number : Int) : Unit = {
    if (num == 0){
      return
    }
    val Hosts : Int = config.getInt("IaaS.CloudProviderProperties.host" + simulation_number + ".mipsCapacity")
    listbuffer += new PeSimple(Hosts)

    createPes(num - 1, listbuffer, simulation_number)
  }
  private def createVm(simulation_number : Int) : List[Vm] = {
    val num_VMs : Int = config.getInt("IaaS.BrokerProperties.logic.num_vms")

    val vmList = ListBuffer.empty[Vm]
    createVm(simulation_number, num_VMs, vmList)
    //    val vmList : List[Vm] = List(new VmSimple(vm_Mips, vm_Pes).setRam(vm_RAM).setSize(vm_Size).setBw(vm_BW))
    return vmList.toList
  }

  private def createVm(simulation_number : Int, num_vms : Int, vmList : ListBuffer[Vm]) : Unit = {
    if (num_vms == 0){
      return
    }
    val reduction_factor : Int = (config.getInt("IaaS.BrokerProperties.logic.num_vms")/config.getDouble("IaaS.utilizationRatio")).toInt
    val vm_Pes : Int    = config.getInt("IaaS.CloudProviderProperties.host" + simulation_number + ".Pes")/config.getInt("IaaS.BrokerProperties.logic.num_vms")
    val vm_Mips : Int   = config.getInt("IaaS.CloudProviderProperties.host" + simulation_number + ".mipsCapacity")/reduction_factor
    val vm_RAM : Int    = config.getInt("IaaS.CloudProviderProperties.host" + simulation_number + ".RAMInMBs")/reduction_factor
    val vm_BW : Int     = config.getInt("IaaS.CloudProviderProperties.host" + simulation_number + ".BandwidthInMBps")/reduction_factor
    val vm_Size : Int   = config.getInt("IaaS.CloudProviderProperties.host" + simulation_number + ".StorageInMBs")/reduction_factor

    val vm : Vm = new VmSimple(vm_Mips, vm_Pes).setRam(vm_RAM).setSize(vm_Size).setBw(vm_BW)
    val cl_sch = cloudletsch
    if (cl_sch == "CloudletSchedulerTimeShared"){
      vm.setCloudletScheduler(new CloudletSchedulerTimeShared)
    }
    else {
      vm.setCloudletScheduler(new CloudletSchedulerSpaceShared)
    }

    vmList += vm

    createVm(simulation_number, num_vms - 1, vmList)

  }

  private def createCloudlets() : List[Cloudlet] = {
    val utilizationModel : UtilizationModelDynamic = new UtilizationModelDynamic(config.getDouble("IaaS.utilizationRatio"))
    val num_Cloudlets : Int = config.getInt("IaaS.BrokerProperties.cloudlet.number")
    val cloudletList = ListBuffer.empty [Cloudlet]
    createCloudlets(num_Cloudlets, utilizationModel, cloudletList)
    return cloudletList.toList
  }

  private def createCloudlets(num_Cloudlets: Int, model: UtilizationModelDynamic, listbuffer: ListBuffer[Cloudlet]) : Unit = {
    if (num_Cloudlets == 0) {
      return
    }
    val cloudlet_Pes : Int = config.getInt("IaaS.BrokerProperties.cloudlet.pes")
    val cloudlet_Size : Int = config.getInt("IaaS.BrokerProperties.cloudlet.size")
    val cloudlet : Cloudlet = new CloudletSimple(cloudlet_Size, cloudlet_Pes, model).setSizes(config.getInt("IaaS.BrokerProperties.cloudlet.filesize"))
    listbuffer += cloudlet
    createCloudlets(num_Cloudlets - 1, model, listbuffer)
  }


  private def configureLogs() : Unit = {
    Log.setLevel(Level.OFF)

    Log.setLevel(Datacenter.LOGGER, Level.OFF)
    Log.setLevel(DatacenterBroker.LOGGER, Level.OFF)
    Log.setLevel(VmAllocationPolicy.LOGGER, Level.OFF)
    Log.setLevel(CloudletScheduler.LOGGER, Level.OFF)
  }

  private def printTotalVmsCost(broker : DatacenterBroker) : Unit = {
    var totalCost: Double = 0.0
    var totalNonIdleVms: Int = 0
    var processingTotalCost: Double = 0
    var memoryTotalCost: Double = 0
    var storageTotalCost: Double = 0
    var bwTotalCost: Double = 0
    for (vm <- broker.getVmCreatedList.asScala) {
      //      System.out.println("Debug: " + vm)
      val cost: VmCost = new VmCost(vm)
      processingTotalCost += cost.getProcessingCost
      memoryTotalCost += cost.getMemoryCost
      storageTotalCost += cost.getStorageCost
      bwTotalCost += cost.getBwCost
      totalCost += cost.getTotalCost
      System.out.println(cost)
    }
  }