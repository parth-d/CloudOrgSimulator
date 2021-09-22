package Simulations

import HelperUtils.{CreateLogger, ObtainConfigReference}
import Simulations.BasicExample.config
import ch.qos.logback.classic.Level
import com.typesafe.config.ConfigFactory
import org.cloudbus.cloudsim.allocationpolicies.{VmAllocationPolicy, VmAllocationPolicyAbstract, VmAllocationPolicyRoundRobin, VmAllocationPolicySimple}
import org.cloudbus.cloudsim.brokers.{DatacenterBroker, DatacenterBrokerSimple}
import org.cloudbus.cloudsim.cloudlets.{Cloudlet, CloudletSimple}
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.datacenters.{Datacenter, DatacenterCharacteristics, DatacenterSimple}
import org.cloudbus.cloudsim.hosts.{Host, HostSimple}
import org.cloudbus.cloudsim.resources.{Pe, PeSimple}
import org.cloudbus.cloudsim.schedulers.cloudlet.{CloudletScheduler, CloudletSchedulerTimeShared}
import org.cloudbus.cloudsim.schedulers.vm.{VmScheduler, VmSchedulerSpaceShared, VmSchedulerTimeShared}
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic
import org.cloudbus.cloudsim.vms.{Vm, VmCost, VmSimple}
import org.cloudsimplus.builders.tables.CloudletsTableBuilder
import org.cloudsimplus.util.Log

import java.util.Comparator
import collection.JavaConverters.*
import scala.collection.mutable.ListBuffer
import scala.jdk.javaapi.CollectionConverters.asJava

class BasicExample

object BasicExample:
  val config = ConfigFactory.load("BasicExample.conf")

//  val config = ObtainConfigReference("BasicExample") match {
//    case Some(value) => value
//    case None => throw new RuntimeException("Cannot obtain a reference to the config data.")
//  }


  val logger = CreateLogger(classOf[BasicExample]);
  private val simulation : CloudSim = new CloudSim();
  private val broker0 : DatacenterBroker = new DatacenterBrokerSimple(simulation);
  private val dclist : List[Datacenter] = createDatacenter()
//  private val datacenter0 : Datacenter = createDatacenter();
  private val vmList : List[Vm] = createVms();
  private val cloudletList : List[Cloudlet] = createCloudlets();


  def Start(): Unit = {

    broker0.submitCloudletList(cloudletList.asJava);
    broker0.submitVmList(vmList.asJava);

    configureLogs();
    simulation.start();

    val finishedCloudlets: List[Cloudlet] = broker0.getCloudletFinishedList.asScala.toList;

    //Sorts cloudlets by VM id then Cloudlet id
    val vmComparator: Comparator[Cloudlet] = Comparator.comparingLong((c: Cloudlet) => c.getVm.getId)

    new CloudletsTableBuilder(finishedCloudlets.asJava).build()

    printTotalVmsCost()
  }

  private def configureLogs() : Unit = {
    Log.setLevel(Level.INFO)

    Log.setLevel(Datacenter.LOGGER, Level.ERROR)
    Log.setLevel(DatacenterBroker.LOGGER, Level.WARN)
    Log.setLevel(VmAllocationPolicy.LOGGER, Level.WARN)
    Log.setLevel(CloudletScheduler.LOGGER, Level.WARN)
  }

  private def createDatacenter() : List[Datacenter] ={
    val listbuffer = ListBuffer.empty[Datacenter]
    val dcs : Int = config.getInt("cloudSimulator.setup.dcs")
    createDatacenter(dcs, listbuffer)

    return listbuffer.toList
  }

  private def createDatacenter(dcs: Int, listbuffer : ListBuffer[Datacenter]) : Unit = {
    if (dcs == 0) {
      return
    }

    val dc_number : Int = config.getInt("cloudSimulator.setup.dcs") - dcs
    val datacenterName : String = "datacenter" + dc_number.toString
    val datacenterPath : String = "cloudSimulator." + datacenterName + "."

    val num_hosts : Int = config.getInt(datacenterPath + "hosts")
    val hostList : List[Host] = createHosts(num_hosts)
//    val dc : Datacenter = new DatacenterSimple(simulation, hostList.asJava);
    val arch            = config.getString(datacenterPath + "arch")
    val os              = config.getString(datacenterPath + "os")
    val vmm             = config.getString(datacenterPath + "vmm")
    val time_zone       = config.getDouble(datacenterPath + "time_zone")
    val cost            = config.getDouble(datacenterPath + "cost")
    val costPerMem      = config.getDouble(datacenterPath + "cpm")
    val costPerStorage  = config.getDouble(datacenterPath + "cps")
    val costPerBw       = config.getDouble(datacenterPath + "cpb")

//    val chars = new DatacenterCharacteristics(arch, os, vmm, asJava[Host](hostList), time_zone, cost, costPerMem, costPerStorage, costPerBw)
    val dc = new DatacenterSimple(simulation, hostList.asJava)
    dc.getCharacteristics
      .setVmm(vmm).setOs(os)
      .setArchitecture(arch)
      .setCostPerBw(costPerBw)
      .setCostPerMem(costPerMem)
      .setCostPerStorage(costPerStorage)
      .setCostPerSecond(cost)
    dc.setName("datacenter" + dc_number.toString)
    listbuffer += dc
    System.out.println("Created Datacenter: " + dc.getName)

    createDatacenter(dcs - 1, listbuffer)
  }

//  private def createDatacenter(): Datacenter = {
//    val num_hosts : Int = config.getInt("cloudSimulator.setup.Hosts")
//    val hostList : List[Host] = createHosts(num_hosts)
//    val dc : Datacenter = new DatacenterSimple(simulation, hostList.asJava);
//    dc.setSchedulingInterval(1)
//    dc.getCharacteristics()
//      .setCostPerSecond(0.01).setCostPerBw(0.01).setCostPerMem(0.01).setCostPerStorage(0.1)
//    return dc
//  }

  private def createHosts(num_hosts: Int) : List[Host] = {
    val listbuffer = ListBuffer.empty[Host]

    createHosts(num_hosts, listbuffer)

    return listbuffer.toList
  }

  private def createHosts(num_hosts: Int, listbuffer: ListBuffer[Host]) : Unit = {
    if (num_hosts == 0) {
      return
    }

    listbuffer += createHosts()

    createHosts(num_hosts - 1, listbuffer)
  }

  private def createHosts() : Host = {
    val Host_RAM : Int = config.getInt("cloudSimulator.host.RAMInMBs")
    val Host_BW : Int = config.getInt("cloudSimulator.host.BandwidthInMBps")
    val Host_Storage : Int = config.getInt("cloudSimulator.host.StorageInMBs")
    val Host_Pes : Int = config.getInt("cloudSimulator.host.Pes")
    val peList : List[Pe] = createPes(Host_Pes)
    val sch = new VmSchedulerTimeShared()
    return new HostSimple(Host_RAM, Host_BW, Host_Storage, peList.asJava);
  }

  private def createPes(num: Int): List[Pe] ={
    val pes = ListBuffer.empty[Pe]

    createPes(num, pes)

    return pes.toList
  }

  private def createPes(num: Int, listbuffer: ListBuffer[Pe]) : Unit = {
    if (num == 0){
      return
    }
    val Hosts : Int = config.getInt("cloudSimulator.host.mipsCapacity")
    listbuffer += new PeSimple(Hosts)

    createPes(num - 1, listbuffer)
  }

  private def createVms() : List[Vm] = {
    val num_VMs : Int = config.getInt("cloudSimulator.setup.Vms")

    val vmList = ListBuffer.empty[Vm]
    createVms(num_VMs, vmList)
//    val vmList : List[Vm] = List(new VmSimple(vm_Mips, vm_Pes).setRam(vm_RAM).setSize(vm_Size).setBw(vm_BW))
    return vmList.toList
  }

  private def createVms(num_VMs: Int, listbuffer: ListBuffer[Vm]) : Unit = {
    if (num_VMs == 0) {
      return
    }

    val vm_Mips : Int = config.getInt("cloudSimulator.vm.mipsCapacity")
    val vm_Pes : Int = config.getInt("cloudSimulator.vm.Pes")
    val vm_RAM : Int = config.getInt("cloudSimulator.vm.RAMInMBs")
    val vm_BW : Int = config.getInt("cloudSimulator.vm.StorageInMBs")
    val vm_Size : Int = config.getInt("cloudSimulator.vm.BandwidthInMBps")

    val vm : Vm = new VmSimple(vm_Mips, vm_Pes).setRam(vm_RAM).setSize(vm_Size).setBw(vm_BW)

    listbuffer += vm

    createVms(num_VMs - 1, listbuffer)
  }

  private def createCloudlets() : List[Cloudlet] = {
    val utilizationModel : UtilizationModelDynamic = new UtilizationModelDynamic(config.getDouble("cloudSimulator.utilizationRatio"))
    val num_Cloudlets : Int = config.getInt("cloudSimulator.setup.Cloudlets")
    val cloudletList = ListBuffer.empty [Cloudlet]
    createCloudlets(num_Cloudlets, utilizationModel, cloudletList)
//    val cloudletList : List [Cloudlet] = List(new CloudletSimple(cloudlet_Size, cloudlet_Pes, utilizationModel).setSizes(config.getInt("cloudSimulator.cloudlet.ioSizes")))
    return cloudletList.toList
  }

  private def createCloudlets(num_Cloudlets: Int, model: UtilizationModelDynamic, listbuffer: ListBuffer[Cloudlet]) : Unit = {
    if (num_Cloudlets == 0) {
      return
    }
    val cloudlet_Pes : Int = config.getInt("cloudSimulator.cloudlet.PEs")
    val cloudlet_Size : Int = config.getInt("cloudSimulator.cloudlet.size")
    val cloudlet : Cloudlet = new CloudletSimple(cloudlet_Size, cloudlet_Pes, model).setSizes(config.getInt("cloudSimulator.cloudlet.ioSizes"))
    listbuffer += cloudlet
    createCloudlets(num_Cloudlets - 1, model, listbuffer)
  }

  private def printTotalVmsCost() : Unit = {
    var totalCost: Double = 0.0
    var totalNonIdleVms: Int = 0
    var processingTotalCost: Double = 0
    var memoryTotalCost: Double = 0
    var storageTotalCost: Double = 0
    var bwTotalCost: Double = 0
    for (vm <- broker0.getVmCreatedList.asScala) {
      //      System.out.println("Debug: " + vm)
      val cost: VmCost = new VmCost(vm)
      processingTotalCost += cost.getProcessingCost
      memoryTotalCost += cost.getMemoryCost
      storageTotalCost += cost.getStorageCost
      bwTotalCost += cost.getBwCost
      totalCost += cost.getTotalCost
      System.out.println(cost)
    }


//        System.out.printf("Total cost ($) for %3d created VMs from %3d in DC %d: %8.2f$ %13.2f$ %17.2f$ %12.2f$ %15.2f$%n",
//          broker0.getVmCreatedList.size(), broker0.getVmsNumber, datacenter0.getId, processingTotalCost, memoryTotalCost, storageTotalCost, bwTotalCost, totalCost)
  }