package Simulations

import HelperUtils.{CreateLogger, ObtainConfigReference}
import Simulations.ConvertedToScala.config
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

/**
 * This file contains the converted examples. Their functionality is merged. The examples which are converted are mentioned below
 *  1. BasicFirstExample
 *  2. LoggingExample
 *  3. VmAllocationPolicyRoundRobinExample
 */

class ConvertedToScala

object ConvertedToScala:

  // Set the file name for the configuration file for this execution
  val config = ConfigFactory.load("BasicExample.conf")

  // Crete a logger object to set logger properties and show the logs in the output
  val logger = CreateLogger(classOf[ConvertedToScala]);

  // Create the necessary objects
  private val simulation : CloudSim = new CloudSim();
  private val broker0 : DatacenterBroker = new DatacenterBrokerSimple(simulation);
  private val dclist : List[Datacenter] = createDatacenter()
  private val vmList : List[Vm] = createVms();
  private val cloudletList : List[Cloudlet] = createCloudlets();


  // Main function to be executed
  def Start(): Unit = {

    // Submit the VM and Cloudlet list to the broker for it to process
    broker0.submitCloudletList(cloudletList.asJava);
    broker0.submitVmList(vmList.asJava);

    // Call the function which sets the required levels for the log file
    configureLogs();

    // Start the simulation
    simulation.start();

    /**
     * Generate a list of cloudlets which have finished the execution successfully
     * This is required to produce the outputs in the console
     */

    val finishedCloudlets: List[Cloudlet] = broker0.getCloudletFinishedList.asScala.toList;

    // Sort the cloudlets first by the Cloudlet ID an then by the VM ID
    val vmComparator: Comparator[Cloudlet] = Comparator.comparingLong((c: Cloudlet) => c.getVm.getId)

    // Create a table output in the console displaying the properties of the finished cloudlets
    new CloudletsTableBuilder(finishedCloudlets.asJava).build()

    // Call the function to print the costs associated with running each VMs and then the total cost
    printTotalVmsCost()
  }

  /**
   * This function defines the levels for the resource types in the simulation
   * If needed, please change the values of the levels individually for granular control of what to log
   */
  private def configureLogs() : Unit = {
    Log.setLevel(Level.INFO)
    Log.setLevel(Datacenter.LOGGER, Level.ERROR)
    Log.setLevel(DatacenterBroker.LOGGER, Level.WARN)
    Log.setLevel(VmAllocationPolicy.LOGGER, Level.WARN)
    Log.setLevel(CloudletScheduler.LOGGER, Level.WARN)
  }

  /**
   * This method is used to return a list of created Datacenters.
   * It creates a ListBuffer object and passes it to the implementing function createDatacenter
   * Once the datacenters are created and added to the buffer, it converts the buffer to list and returns it
   * @return List of Datacenters
   */
  private def createDatacenter() : List[Datacenter] ={
    val listbuffer = ListBuffer.empty[Datacenter]
    val dcs : Int = config.getInt("cloudSimulator.setup.dcs")

    //Call the implementation function passing the number of datacenters to be created along with the ListBuffer
    createDatacenter(dcs, listbuffer)
    return listbuffer.toList
  }

  /**
   * This is a recursive implementation of createDatacenter which reads the required parameters from the config and
   * creates the datacenters.
   * This function is recursively called until the number of datacenters remaining to be created (dcs : Int) becomes 0
   * @param dcs number of remaining datacenters to be created
   * @param listbuffer a ListBuffer holding the datacenters created
   */
  private def createDatacenter(dcs: Int, listbuffer : ListBuffer[Datacenter]) : Unit = {
    if (dcs == 0) {
      return
    }

    // Create a string holding a partial path to the datacenter's properties to be read from the config
    val dc_number : Int = config.getInt("cloudSimulator.setup.dcs") - dcs
    val datacenterName : String = "datacenter" + dc_number.toString
    val datacenterPath : String = "cloudSimulator." + datacenterName + "."

    // Read and store properties in their respective variables
    val num_hosts : Int       = config.getInt(datacenterPath + "hosts")
    val arch                  = config.getString(datacenterPath + "arch")
    val os                    = config.getString(datacenterPath + "os")
    val vmm                   = config.getString(datacenterPath + "vmm")
    val time_zone             = config.getDouble(datacenterPath + "time_zone")
    val cost                  = config.getDouble(datacenterPath + "cost")
    val costPerMem            = config.getDouble(datacenterPath + "cpm")
    val costPerStorage        = config.getDouble(datacenterPath + "cps")
    val costPerBw             = config.getDouble(datacenterPath + "cpb")

    //Create the hosts required for the datacenter
    val hostList : List[Host] = createHosts(num_hosts)

    // Create a simple datacenter
    val dc = new DatacenterSimple(simulation, hostList.asJava, new VmAllocationPolicyRoundRobin)

    // Set the previously read characteristics to dc
    dc.getCharacteristics
      .setVmm(vmm).setOs(os)
      .setArchitecture(arch)
      .setCostPerBw(costPerBw)
      .setCostPerMem(costPerMem)
      .setCostPerStorage(costPerStorage)
      .setCostPerSecond(cost)
    dc.setName("datacenter" + dc_number.toString)

    // Add the datacenter to ListBuffer
    listbuffer += dc

    // Recursive call
    createDatacenter(dcs - 1, listbuffer)
  }

  /**
   * This method is used to return a list of created Hosts to the datacenter.
   * It creates a ListBuffer object and passes it to the implementing function createHosts
   * Once the hosts are created and added to the buffer, it converts the buffer to list and returns it
   * @param num_hosts: Number of hosts to be created
   * @return List of hosts
   */
  private def createHosts(num_hosts: Int) : List[Host] = {
    val listbuffer = ListBuffer.empty[Host]

    // Call the implementation function passing the number of hosts to be created along with the ListBuffer
    createHosts(num_hosts, listbuffer)
    return listbuffer.toList
  }

  /**
   * This is a recursive implementation of createHosts which calls createHosts() and appends the created host to the
   * ListBuffer
   * This function is recursively called until the number of hosts remaining to be created (num_hosts : Int) becomes 0
   * @param num_hosts number of remaining hosts to be created
   * @param listbuffer ListBuffer holding the hosts created
   */
  private def createHosts(num_hosts: Int, listbuffer: ListBuffer[Host]) : Unit = {
    if (num_hosts == 0) {
      return
    }

    listbuffer += createHosts()

    // Recursive call
    createHosts(num_hosts - 1, listbuffer)
  }

  /**
   * This function reads the required parameters from the config a creates a host.
   * @return Host object
   */
  private def createHosts() : Host = {
    // Read and store properties in their respective variables
    val Host_RAM : Int      = config.getInt("cloudSimulator.host.RAMInMBs")
    val Host_BW : Int       = config.getInt("cloudSimulator.host.BandwidthInMBps")
    val Host_Storage : Int  = config.getInt("cloudSimulator.host.StorageInMBs")
    val Host_Pes : Int      = config.getInt("cloudSimulator.host.Pes")

    // Create required number of processing elements for the host
    val peList : List[Pe]   = createPes(Host_Pes)

    // Create an object of the required VmScheduler and set it to the created simple host and return it
    return new HostSimple(Host_RAM, Host_BW, Host_Storage, peList.asJava).setVmScheduler(new VmSchedulerTimeShared);
  }

  /**
   * This method is used to return a list of created processing elements to the host.
   * It creates a ListBuffer object and passes it to the implementing function createPes
   * Once the processing elements are created and added to the buffer, it converts the buffer to list and returns it
   * @param num Number of processing elements to be created
   * @return List of processing elements
   */
  private def createPes(num: Int): List[Pe] ={
    val pes = ListBuffer.empty[Pe]

    // Call the implementation function passing the number of processing elements to be created along with the ListBuffer
    createPes(num, pes)
    return pes.toList
  }

  /**
   * This function recursively creates processing elements with the specified value of mipsCapacity in the config
   * @param num Number of processing elements to be created
   * @param listbuffer ListBuffer holding the created processing elements
   */
  private def createPes(num: Int, listbuffer: ListBuffer[Pe]) : Unit = {
    if (num == 0){
      return
    }
    val Hosts : Int = config.getInt("cloudSimulator.host.mipsCapacity")
    listbuffer += new PeSimple(Hosts)

    createPes(num - 1, listbuffer)
  }

  /**
   * This method is used to return a list of created VMs.
   * It creates a ListBuffer object and passes it to the implementing function createVms
   * Once the VMs are created and added to the buffer, it converts the buffer to list and returns it
   * @return List of VMs
   */
  private def createVms() : List[Vm] = {

    val num_VMs : Int = config.getInt("cloudSimulator.setup.Vms")
    val vmList = ListBuffer.empty[Vm]

    // Call the implementation function passing the number of hosts to be created along with the ListBuffer
    createVms(num_VMs, vmList)
    return vmList.toList
  }

  /**
   * This is a recursive implementation of createVms which calls createVms() and appends the created VM to the
   * ListBuffer
   * This function is recursively called until the number of VMs remaining to be created (num_VMs : Int) becomes 0
   * @param num_VMs number of remaining VMs to be created
   * @param listbuffer holding the VMs created
   */
  private def createVms(num_VMs: Int, listbuffer: ListBuffer[Vm]) : Unit = {
    if (num_VMs == 0) {
      return
    }

    // Read and store properties in their respective variables
    val vm_Mips : Int = config.getInt("cloudSimulator.vm.mipsCapacity")
    val vm_Pes : Int  = config.getInt("cloudSimulator.vm.Pes")
    val vm_RAM : Int  = config.getInt("cloudSimulator.vm.RAMInMBs")
    val vm_BW : Int   = config.getInt("cloudSimulator.vm.StorageInMBs")
    val vm_Size : Int = config.getInt("cloudSimulator.vm.BandwidthInMBps")

    // Create a Simple VM object and set its properties
    val vm : Vm = new VmSimple(vm_Mips, vm_Pes).setRam(vm_RAM).setSize(vm_Size).setBw(vm_BW)

    // Add the datacenter to ListBuffer
    listbuffer += vm

    //Recursive Call
    createVms(num_VMs - 1, listbuffer)
  }

  /**
   * This method is used to return a list of created Cloudlets.
   * It creates a ListBuffer object and passes it to the implementing function createCloudlets
   * Once the cloudlets are created and added to the buffer, it converts the buffer to list and returns it
   * @return List of cloudlets
   */
  private def createCloudlets() : List[Cloudlet] = {
    // The utilization model object is created based on the utilization ratio specified in the config
    val utilizationModel : UtilizationModelDynamic = new UtilizationModelDynamic(config.getDouble("cloudSimulator.utilizationRatio"))

    val num_Cloudlets : Int = config.getInt("cloudSimulator.setup.Cloudlets")

    val cloudletList = ListBuffer.empty [Cloudlet]

    createCloudlets(num_Cloudlets, utilizationModel, cloudletList)
    return cloudletList.toList
  }

  /**
   * This is a recursive implementation of createCloudlets which calls createCloudlets() and appends the created cloudlet
   * to the ListBuffer
   * This function is recursively called until the number of cloudlets remaining to be created (num_Cloudlets : Int) becomes 0
   * @param num_Cloudlets number of remaining VMs to be created
   * @param model Utilization Model Object
   * @param listbuffer holding the VMs created
   */
  private def createCloudlets(num_Cloudlets: Int, model: UtilizationModelDynamic, listbuffer: ListBuffer[Cloudlet]) : Unit = {
    if (num_Cloudlets == 0) {
      return
    }

    // Read and store properties in their respective variables
    val cloudlet_Pes : Int      = config.getInt("cloudSimulator.cloudlet.PEs")
    val cloudlet_Size : Int     = config.getInt("cloudSimulator.cloudlet.size")
    val cloudletFileSize : Int  = config.getInt("cloudSimulator.cloudlet.ioSizes")

    // Create a Simple Cloudlet object and set its properties
    val cloudlet : Cloudlet = new CloudletSimple(cloudlet_Size, cloudlet_Pes, model).setSizes(cloudletFileSize)

    // Add the datacenter to ListBuffer
    listbuffer += cloudlet

    //Recursive Call
    createCloudlets(num_Cloudlets - 1, model, listbuffer)
  }

  /**
   * This function calculates the total cost of running the VMs for each VM and then prints the costs accordingly
   */
  private def printTotalVmsCost() : Unit = {

    // Initialize variables
    var totalCost: Double           = 0
    var totalNonIdleVms: Int        = 0
    var processingTotalCost: Double = 0
    var memoryTotalCost: Double     = 0
    var storageTotalCost: Double    = 0
    var bwTotalCost: Double         = 0

    // For each VM object, create a VmCost object and extract the costs and prints it
    for (vm <- broker0.getVmCreatedList.asScala) {
      val cost: VmCost      = new VmCost(vm)
      processingTotalCost   += cost.getProcessingCost
      memoryTotalCost       += cost.getMemoryCost
      storageTotalCost      += cost.getStorageCost
      bwTotalCost           += cost.getBwCost
      totalCost             += cost.getTotalCost
      System.out.println(cost)
    }
  }