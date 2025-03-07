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

/**
 * This simulation is used to simulate a Infrastructure-as-a-Service Cloud
 *
 * The total cost is calculated for each option and the cheapest cost is printed to the console
 *
 * The broker decides the following parameters:
 *    i. Cloudlet Scheduler
 *    i. VM Scheduler
 *    i. Choice of host configurations:
 *      1. Slow: Cost set to $0.1 per unit for all resources
 *        a. mips Capacity  : 1000
 *        b. RAM            : 1024 MB
 *        c. Storage        : 1024 MB
 *        d. Bandwidth      : 1000 MBps
 *        e. PEs            : 8
 *     2. Medium: Cost set to $0.25 per unit for all resources
 *        a. mips Capacity  : 2000
 *        b. RAM            : 2048 MB
 *        c. Storage        : 2048 MB
 *        d. Bandwidth      : 2000 MBps
 *        e. PEs            : 8
 *     3. Fast: Cost set to $0.70 per unit for all resources
 *        a. mips Capacity  : 4000
 *        b. RAM            : 4096 MB
 *        c. Storage        : 4096 MB
 *        d. Bandwidth      : 4000 MBps
 *        e. PEs            : 16
 *
 * The Cloud provider sets the following parameters:
 *    i. VM Allocation Policy  : Best Fit
 */

class IaaS

object IaaS:
  // Set the file name for the configuration file for this execution
  val config = ConfigFactory.load("Simulations.conf")

  // Set the file name for the configuration file for this execution
  val logger = CreateLogger(classOf[IaaS]);

  // Create a result ListBuffer to append the results and find minimum
  val results = ListBuffer.empty[Double]

  // Values to be toggled for iterations
  var vmsch = "VmSchedulerTimeShared"
  var cloudletsch = "CloudletSchedulerTimeShared"

  // Main function to be executed
  def StartSimulation() : String = {
    System.out.println("Currently executing IaaS")

    // Iterating over the 12 possible options for the broker to choose from:
    // {Slow, Medium, Fast} X {CloudletSchedulerTimeShared, CloudletSchedulerSpaceShared} X {VmSchedulerTimeShared, VmSchedulerSpaceShared}
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
    return ("\n\nResult for IaaS:\nThe minimum cost required to execute the required cloudlets is $" + results.toList.min)
  }


  def Start(simulation_number : Int) : Unit = {

    System.out.println("\n\nCurrent Config:")
    System.out.println("\t Cloudlet Scheduler: " + cloudletsch)
    System.out.println("\t VM Scheduler: " + vmsch)
    System.out.println("\t VM Config Code: " + simulation_number)

    // Call the function which sets the required levels for the log file
    configureLogs();

    // Create the necessary objects
    val simulation : CloudSim         = new CloudSim();
    val broker : DatacenterBroker     = new DatacenterBrokerSimple(simulation);
    val datacenter : Datacenter       = createDatacenter(simulation_number, simulation)
    val vmList : List[Vm]             = createVm(simulation_number)
    val cloudletList : List[Cloudlet] = createCloudlets()

    logger.info("Created all objects successfully")

    // Submit the VM and Cloudlet list to the broker for it to process
    broker.submitCloudletList(cloudletList.asJava)
    broker.submitVmList(vmList.asJava)

    logger.info("Submitted cloudlets and VM to the broker")

    // Start the simulation
    simulation.start();

    /**
     * Generate a list of cloudlets which have finished the execution successfully
     * This is required to produce the outputs in the console
     */
    val finishedCloudlets: List[Cloudlet] = broker.getCloudletFinishedList.asScala.toList;

    // Sort the cloudlets first by the Cloudlet ID an then by the VM ID
    val vmComparator: Comparator[Cloudlet] = Comparator.comparingLong((c: Cloudlet) => c.getVm.getId)

    // Create a table output in the console displaying the properties of the finished cloudlets
    new CloudletsTableBuilder(finishedCloudlets.asJava).build()

    // Call the function to print the costs associated with running each VMs and then the total cost
    printTotalVmsCost(broker)

    // Logic to calculate the total cost for this implementation
    var vm_cost_sum : Double = 0
    for (vm <- broker.getVmCreatedList.asScala){
      val vm_cost = new VmCost(vm)
      vm_cost_sum = vm_cost_sum + vm_cost.getTotalCost
    }
    results.addOne(vm_cost_sum)
  }

  /**
   * This function creates datacenter objects by reading the required parameters from the config file.
   * The parameters are pulled for the specific configuration currently being executed
   * @param simulation_number Current execution
   * @param simulation Current Cloudsim object
   * @return Datacenter object
   */
  private def createDatacenter(simulation_number: Int, simulation: CloudSim) : Datacenter = {
    // Create a string holding a partial path to the datacenter's properties to be read from the config
    val datacenterName : String = "datacenter" + simulation_number.toString
    val datacenterPath : String = "IaaS.CloudProviderProperties." + datacenterName + "."

    // Read and store properties in their respective variables
    val num_hosts : Int = config.getInt(datacenterPath + "hosts")
    val arch            = config.getString(datacenterPath + "arch")
    val os              = config.getString(datacenterPath + "os")
    val vmm             = config.getString(datacenterPath + "vmm")
    val time_zone       = config.getDouble(datacenterPath + "time_zone")
    val cost            = config.getDouble(datacenterPath + "cost")
    val costPerMem      = config.getDouble(datacenterPath + "cpm")
    val costPerStorage  = config.getDouble(datacenterPath + "cps")
    val costPerBw       = config.getDouble(datacenterPath + "cpb")

    //Create the hosts required for the datacenter
    val hostList : List[Host] = List(createHost(simulation_number))

    // Create a simple datacenter
    val dc = new DatacenterSimple(simulation, hostList.asJava)

    // Set the previously read characteristics to dc
    dc.getCharacteristics
      .setVmm(vmm).setOs(os)
      .setArchitecture(arch)
      .setCostPerBw(costPerBw)
      .setCostPerMem(costPerMem)
      .setCostPerStorage(costPerStorage)
      .setCostPerSecond(cost)
    dc.setName("datacenter" + simulation_number.toString)

    // Set the allocation policy to best fit
    dc.setVmAllocationPolicy(new VmAllocationPolicyBestFit)
    return dc
  }

  /**
   * This function creates a host object by reading the required parameters from the config file.
   * The parameters are pulled for the specific configuration currently being executed
   * @return
   */
  private def createHost(simulation_number : Int) : Host = {
    // Read and store properties in their respective variables
    val Host_RAM : Int      = config.getInt("IaaS.CloudProviderProperties.host" + simulation_number + ".RAMInMBs")
    val Host_BW : Int       = config.getInt("IaaS.CloudProviderProperties.host" + simulation_number + ".BandwidthInMBps")
    val Host_Storage : Int  = config.getInt("IaaS.CloudProviderProperties.host" + simulation_number + ".StorageInMBs")
    val Host_Pes : Int      = config.getInt("IaaS.CloudProviderProperties.host" + simulation_number + ".Pes")

    // Create required number of processing elements for the host
    val peList : List[Pe]   = createPes(Host_Pes, simulation_number)

    // Set the VM Scheduler based on the current iteration
    val vm_sch = vmsch
    if (vm_sch == "VmSchedulerTimeShared"){
      return new HostSimple(Host_RAM, Host_BW, Host_Storage, peList.asJava).setVmScheduler(new VmSchedulerTimeShared());
    }
    else {
      return new HostSimple(Host_RAM, Host_BW, Host_Storage, peList.asJava).setVmScheduler(new VmSchedulerSpaceShared())
    }
  }

  /**
   * This method is used to return a list of created processing elements to the host.
   * It creates a ListBuffer object and passes it to the implementing function createPes
   * Once the processing elements are created and added to the buffer, it converts the buffer to list and returns it
   * @param num Number of processing elements to be created
   * @return List of processing elements
   */
  private def createPes(num: Int, simulation_number : Int): List[Pe] ={
    val pes = ListBuffer.empty[Pe]

    // Call the implementation function passing the number of processing elements to be created along with the ListBuffer
    createPes(num, pes, simulation_number)
    return pes.toList
  }

  /**
   * This function recursively creates processing elements with the specified value of mipsCapacity in the config
   * @param num Number of processing elements to be created
   * @param listbuffer ListBuffer holding the created processing elements
   */
  private def createPes(num: Int, listbuffer: ListBuffer[Pe], simulation_number : Int) : Unit = {
    if (num == 0){
      return
    }
    val Hosts : Int = config.getInt("IaaS.CloudProviderProperties.host" + simulation_number + ".mipsCapacity")
    listbuffer += new PeSimple(Hosts)

    createPes(num - 1, listbuffer, simulation_number)
  }

  /**
   * This method is used to return a list of created processing elements to the host.
   * It creates a ListBuffer object and passes it to the implementing function createPes
   * Once the processing elements are created and added to the buffer, it converts the buffer to list and returns it
   * @param simulation_number Current execution
   * @return List of VM objects
   */
  def createVm(simulation_number : Int) : List[Vm] = {

    val num_VMs : Int = config.getInt("IaaS.BrokerProperties.logic.num_vms")
    val vmList = ListBuffer.empty[Vm]

    // Call the implementation function passing the number of hosts to be created along with the ListBuffer
    createVm(simulation_number, num_VMs, vmList)
    return vmList.toList
  }

  /**
   * This function creates a VM object by reading the required parameters from the config file.
   * The parameters are pulled for the specific configuration currently being executed
   * @param simulation_number Current execution
   * @param num_vms Number of remaining VMs
   * @param vmList ListBuffer containing created VMs
   */
  private def createVm(simulation_number : Int, num_vms : Int, vmList : ListBuffer[Vm]) : Unit = {
    if (num_vms == 0){
      return
    }

    // Equally dividing the available resources considering the number of VMs and utilization ratio
    val reduction_factor : Int = (config.getInt("IaaS.BrokerProperties.logic.num_vms")/config.getDouble("IaaS.utilizationRatio")).toInt

    // Read and store properties in their respective variables
    val vm_Pes : Int    = config.getInt("IaaS.CloudProviderProperties.host" + simulation_number + ".Pes")/config.getInt("IaaS.BrokerProperties.logic.num_vms")
    val vm_Mips : Int   = config.getInt("IaaS.CloudProviderProperties.host" + simulation_number + ".mipsCapacity")/reduction_factor
    val vm_RAM : Int    = config.getInt("IaaS.CloudProviderProperties.host" + simulation_number + ".RAMInMBs")/reduction_factor
    val vm_BW : Int     = config.getInt("IaaS.CloudProviderProperties.host" + simulation_number + ".BandwidthInMBps")/reduction_factor
    val vm_Size : Int   = config.getInt("IaaS.CloudProviderProperties.host" + simulation_number + ".StorageInMBs")/reduction_factor

    // Create a Simple VM object and set its properties
    val vm : Vm = new VmSimple(vm_Mips, vm_Pes).setRam(vm_RAM).setSize(vm_Size).setBw(vm_BW)

    // Set the Cloudlet Scheduler based on the config parameter
    val cl_sch = cloudletsch
    if (cl_sch == "CloudletSchedulerTimeShared"){
      vm.setCloudletScheduler(new CloudletSchedulerTimeShared)
    }
    else {
      vm.setCloudletScheduler(new CloudletSchedulerSpaceShared)
    }

    // Append created VM to the ListBuffer
    vmList += vm

    // Recursive call
    createVm(simulation_number, num_vms - 1, vmList)

  }

  /**
   * This method is used to return a list of created Cloudlets.
   * It creates a ListBuffer object and passes it to the implementing function createCloudlets
   * Once the cloudlets are created and added to the buffer, it converts the buffer to list and returns it
   * @return List of cloudlets
   */
  private def createCloudlets() : List[Cloudlet] = {
    // The utilization model object is created based on the utilization ratio specified in the config
    val utilizationModel : UtilizationModelDynamic = new UtilizationModelDynamic(config.getDouble("IaaS.utilizationRatio")).setMaxResourceUtilization(0.5)

    val num_Cloudlets : Int = config.getInt("IaaS.BrokerProperties.cloudlet.number")

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
    val cloudlet_Pes : Int = config.getInt("IaaS.BrokerProperties.cloudlet.pes")
    val cloudlet_Size : Int = config.getInt("IaaS.BrokerProperties.cloudlet.size")
    val cloudletFileSize : Int  = config.getInt("IaaS.BrokerProperties.cloudlet.filesize")

    // Create a Simple Cloudlet object and set its properties
    val cloudlet : Cloudlet = new CloudletSimple(cloudlet_Size, cloudlet_Pes, model).setSizes(cloudletFileSize)

    // Add the datacenter to ListBuffer
    listbuffer += cloudlet

    //Recursive Call
    createCloudlets(num_Cloudlets - 1, model, listbuffer)
  }

  /**
   * This function defines the levels for the resource types in the simulation
   * If needed, please change the values of the levels individually for granular control of what to log
   */
  private def configureLogs() : Unit = {
    Log.setLevel(Level.INFO)
    Log.setLevel(Datacenter.LOGGER, Level.ERROR)
    Log.setLevel(DatacenterBroker.LOGGER, Level.WARN)
    Log.setLevel(VmAllocationPolicy.LOGGER, Level.OFF)
    Log.setLevel(CloudletScheduler.LOGGER, Level.WARN)
  }

  /**
   * This function calculates the total cost of running the VMs for each VM and then prints the costs accordingly
   */
  private def printTotalVmsCost(broker : DatacenterBroker) : Unit = {

    // Initialize variables
    var totalCost: Double           = 0
    var totalNonIdleVms: Int        = 0
    var processingTotalCost: Double = 0
    var memoryTotalCost: Double     = 0
    var storageTotalCost: Double    = 0
    var bwTotalCost: Double         = 0

    // For each VM object, create a VmCost object and extract the costs and prints it
    for (vm <- broker.getVmCreatedList.asScala) {
      val cost: VmCost      = new VmCost(vm)
      processingTotalCost   += cost.getProcessingCost
      memoryTotalCost       += cost.getMemoryCost
      storageTotalCost      += cost.getStorageCost
      bwTotalCost           += cost.getBwCost
      totalCost             += cost.getTotalCost
      System.out.println(cost)
    }
  }