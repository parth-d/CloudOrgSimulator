package Simulations

import HelperUtils.{CreateLogger, ObtainConfigReference}
import Simulations.BasicExample.config
import org.cloudbus.cloudsim.brokers.{DatacenterBroker, DatacenterBrokerSimple}
import org.cloudbus.cloudsim.cloudlets.{Cloudlet, CloudletSimple}
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.datacenters.{Datacenter, DatacenterSimple}
import org.cloudbus.cloudsim.hosts.{Host, HostSimple}
import org.cloudbus.cloudsim.resources.{Pe, PeSimple}
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic
import org.cloudbus.cloudsim.vms.{Vm, VmSimple}
import org.cloudsimplus.builders.tables.CloudletsTableBuilder

import collection.JavaConverters.*
import scala.collection.mutable.ListBuffer

class BasicExample

object BasicExample:

  val config = ObtainConfigReference("cloudSimulator") match {
    case Some(value) => value
    case None => throw new RuntimeException("Cannot obtain a reference to the config data.")
  }

  val logger = CreateLogger(classOf[BasicCloudSimPlusExample]);

  def Start(): Unit = {
    val simulation : CloudSim = new CloudSim();
    val datacenter0 : Datacenter = createDatacenter(simulation);

    val broker0 : DatacenterBroker = new DatacenterBrokerSimple(simulation);

    val vmList : List[Vm] = createVms();
    val cloudletList : List[Cloudlet] = createCloudlets();
    broker0.submitCloudletList(cloudletList.asJava);
    broker0.submitVmList(vmList.asJava);

    simulation.start();

    new CloudletsTableBuilder(broker0.getCloudletFinishedList).build();
  }

  private def createDatacenter(simulation: CloudSim): Datacenter = {
    val hostList : List[Host] = List(createHost());
    return new DatacenterSimple(simulation, hostList.asJava);
  }

  private def createHost() : Host = {
    val Hosts : Int = config.getInt("cloudSimulator.host.mipsCapacity")
    val Host_RAM : Int = config.getInt("cloudSimulator.host.RAMInMBs")
    val Host_BW : Int = config.getInt("cloudSimulator.host.BandwidthInMBps")
    val Host_Storage : Int = config.getInt("cloudSimulator.host.StorageInMBs")
    val peList : List[Pe] = List(new PeSimple(Hosts))
    return new HostSimple(Host_RAM, Host_BW, Host_Storage, peList.asJava);
  }

  private def createVms() : List[Vm] = {
    val num_VMs : Int = config.getInt("cloudSimulator.setup.Vms")

    val vmList = ListBuffer.empty[Vm]
    createVmsImpl(num_VMs, vmList)
//    val vmList : List[Vm] = List(new VmSimple(vm_Mips, vm_Pes).setRam(vm_RAM).setSize(vm_Size).setBw(vm_BW))
    return vmList.toList
  }

  private def createVmsImpl(num_VMs: Int, listbuffer: ListBuffer[Vm]) : Unit = {
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

    createVmsImpl(num_VMs - 1, listbuffer)
  }

  private def createCloudlets() : List[Cloudlet] = {
    val utilizationModel : UtilizationModelDynamic = new UtilizationModelDynamic(config.getDouble("cloudSimulator.utilizationRatio"))
    val num_Cloudlets : Int = config.getInt("cloudSimulator.setup.Cloudlets")
    val cloudletList = ListBuffer.empty [Cloudlet]
    createCloudletsImpl(num_Cloudlets, utilizationModel, cloudletList)
//    val cloudletList : List [Cloudlet] = List(new CloudletSimple(cloudlet_Size, cloudlet_Pes, utilizationModel).setSizes(config.getInt("cloudSimulator.cloudlet.ioSizes")))
    return cloudletList.toList
  }

  private def createCloudletsImpl(num_Cloudlets: Int, model: UtilizationModelDynamic, listbuffer: ListBuffer[Cloudlet]) : Unit = {
    if (num_Cloudlets == 0) {
      return
    }
    val cloudlet_Pes : Int = config.getInt("cloudSimulator.cloudlet.PEs")
    val cloudlet_Size : Int = config.getInt("cloudSimulator.cloudlet.size")
    val cloudlet : Cloudlet = new CloudletSimple(cloudlet_Size, cloudlet_Pes, model).setSizes(config.getInt("cloudSimulator.cloudlet.ioSizes"))
    listbuffer += cloudlet
    createCloudletsImpl(num_Cloudlets - 1, model, listbuffer)
  }