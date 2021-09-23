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
 * This simulation is used to simulate a Platform-as-a-Service Cloud
 *
 * The total cost is calculated for each option and the cheapest cost is printed to the console
 *
 * The broker decides the following parameters:
 *    i. Cloudlet Scheduler
 *    i. Choice of VM Configurations:
 *      1. Slow: Cost set to $0.1 per unit for all resources
 *        a. mips Capacity  : 1000
 *        b. RAM            : 1024 MB
 *        c. Storage        : 1024 MB
 *        d. Bandwidth      : 1000 MBps
 *        e. PEs            : 4
 *     2. Medium: Cost set to $0.25 per unit for all resources
 *        a. mips Capacity  : 2000
 *        b. RAM            : 2048 MB
 *        c. Storage        : 2048 MB
 *        d. Bandwidth      : 2000 MBps
 *        e. PEs            : 4
 *     3. Fast: Cost set to $0.70 per unit for all resources
 *        a. mips Capacity  : 4000
 *        b. RAM            : 4096 MB
 *        c. Storage        : 4096 MB
 *        d. Bandwidth      : 4000 MBps
 *        e. PEs            : 8
 *
 * The Cloud provider sets the following parameters:
 *    i.  VM Scheduler          : Time Shared
 *    i.  VM Allocation Policy  : Best Fit
 */

class PaaS

object PaaS:

  // Set the file name for the configuration file for this execution
  val config = ConfigFactory.load("Simulations.conf")

  // Create a logger object to set logger properties and show the logs in the output
  val logger = CreateLogger(classOf[PaaS]);
  
  // Value to be toggled for iterations
  var cloudletsch = "CloudletSchedulerTimeShared"

  // Create a result ListBuffer to append the results and find minimum
  val results = ListBuffer.empty[Double]

  // Main function to be executed
  def StartSimulation() : String = {
    System.out.println("Currently executing PaaS")

    // Iterating over the 6 possible options for the broker (Toggling Cloudlet Scheduler along with configs)
    for (i <- 0 to 2){
      Start(i)
    }

    cloudletsch = "CloudletSchedulerSpaceShared"
    for (i <- 0 to 2){
      Start(i)
    }
    return ("\n\nResult for PaaS:\nThe minimum cost required to execute the required cloudlets is $" + results.toList.min)
  }

  def Start(simulation_number : Int) : Unit = {
    
    System.out.println("\n\nCurrent Config:")
    System.out.println("\t Cloudlet Scheduler: " + cloudletsch)
    System.out.println("\t VM Config Code: " + simulation_number)

    // Create the necessary objects
    val simulation : CloudSim         = new CloudSim();
    val broker : DatacenterBroker     = new DatacenterBrokerSimple(simulation);
    val datacenter : Datacenter       = createDatacenter(simulation_number, simulation)
    val vm : Vm                       = createVm(simulation_number)
    val cloudletList : List[Cloudlet] = createCloudlets()

    // Submit the VM and Cloudlet list to the broker for it to process
    broker.submitCloudletList(cloudletList.asJava)
    broker.submitVmList(List(vm).asJava)

    // Call the function which sets the required levels for the log file
    configureLogs();

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
    val datacenterPath : String = "PaaS.CloudProviderProperties." + datacenterName + "."

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
    val hostList : List[Host] = List(createHost())

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
  private def createHost() : Host = {
    // Read and store properties in their respective variables
    val Host_RAM : Int      = config.getInt("PaaS.CloudProviderProperties.host.RAMInMBs")
    val Host_BW : Int       = config.getInt("PaaS.CloudProviderProperties.host.BandwidthInMBps")
    val Host_Storage : Int  = config.getInt("PaaS.CloudProviderProperties.host.StorageInMBs")
    val Host_Pes : Int      = config.getInt("PaaS.CloudProviderProperties.host.Pes")

    // Create required number of processing elements for the host
    val peList : List[Pe] = createPes(Host_Pes)

    // Set the VM Scheduler based on the config parameter
    val vm_sch = config.getString("PaaS.CloudProviderProperties.logic.vmsch")
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
    val Hosts : Int = config.getInt("PaaS.CloudProviderProperties.host.mipsCapacity")
    listbuffer += new PeSimple(Hosts)

    createPes(num - 1, listbuffer)
  }

  /**
   * This function creates a VM object by reading the required parameters from the config file.
   * The parameters are pulled for the specific configuration currently being executed
   * @param simulation_number Current execution
   * @return VM object
   */
  private def createVm(simulation_number : Int) : Vm = {
    // Read and store properties in their respective variables
    val vm_Mips : Int   = config.getInt("PaaS.CloudProviderProperties.vm" + simulation_number + ".mipsCapacity")
    val vm_Pes : Int    = config.getInt("PaaS.CloudProviderProperties.vm" + simulation_number + ".pes")
    val vm_RAM : Int    = config.getInt("PaaS.CloudProviderProperties.vm" + simulation_number + ".RAMInMBs")
    val vm_BW : Int     = config.getInt("PaaS.CloudProviderProperties.vm" + simulation_number + ".BandwidthInMBps")
    val vm_Size : Int   = config.getInt("PaaS.CloudProviderProperties.vm" + simulation_number + ".StorageInMBs")

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
    return vm
  }

  /**
   * This method is used to return a list of created Cloudlets.
   * It creates a ListBuffer object and passes it to the implementing function createCloudlets
   * Once the cloudlets are created and added to the buffer, it converts the buffer to list and returns it
   * @return List of cloudlets
   */
  private def createCloudlets() : List[Cloudlet] = {
    // The utilization model object is created based on the utilization ratio specified in the config
    val utilizationModel : UtilizationModelDynamic = new UtilizationModelDynamic(config.getDouble("PaaS.utilizationRatio")).setMaxResourceUtilization(0.1)
    
    val num_Cloudlets : Int = config.getInt("PaaS.BrokerProperties.cloudlet.number")
    
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
    val cloudlet_Pes : Int = config.getInt("PaaS.BrokerProperties.cloudlet.pes")
    val cloudlet_Size : Int = config.getInt("PaaS.BrokerProperties.cloudlet.size")
    val cloudletFileSize : Int  = config.getInt("PaaS.BrokerProperties.cloudlet.filesize")

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