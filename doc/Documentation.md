# Homework 1
## Installation
### Method: SBT
1. Clone the project.
2. Open terminal/cmd and navigate to the directory of the project
3. type 'sbt'
4. once loaded, type 'run'
### Method: Intellij Idea
1. Clone the project
2. Import it in Intellij Idea

## Description
The simulations are divided in two parts:
1. ConvertedToScala.scala
2. Simulation.scala

### 1. ConvertedToScala
This file contains 3 examples from CloudSimPlus examples repository converted from Java to Scala and merged in the same code.They include:
1. BasicFirstExample
2. LoggingExample
3. VmAllocationPolicyRoundRobinExample

Note: The BasicCloudSimPlusExample.scala corresponds more to the ReducedExample in the CloudSim Plus documentation. This is different from the BasicFirstExample which is converted as a part of this project.

This simulation uses the BasicExample.conf file to set the parameters for various resources used.

Some noteworthy points:
1. The logging values can be micro-managed by changing the parameters in the configureLogs() function.
2. Without loss of generality, parameters like cloudlet schedulers, VM schedulers are not passed, hence the default values are taken. These values can be passed if a better control or performance is needed.

### 2. Simulation.scala
This file is the driver file for the three implementations viz IaaS, PaaS, SaaS. Also, to keep the code streamlined, the ConvertedToScala is executed in this file itself.
The 3 individual files containing the implementation are:
1. IaaS.scala
2. PaaS.scala
3. SaaS.scala

This simulation uses the Simulations.conf file to set the parameters for various resources used in the simulations.

Some noteworthy points:
1. The parameters defining the total processing work (cloudlet size, cloudlet pes, number of cloudlets) is kept constant between the three simulations to get a fair comparison.
2. The individual simulations (SaaS, PaaS, IaaS) find the minimum cost within their simulations and returns it to the driver code (Simulation.scala) which in turn prints the value.
3. SaaS:
    1. The cloud provider gives 3 options to the broker based on the processing speed. The broker (simulation) then executes and finds the cost for all 3 options and concludes the cheapest option.
    2. For simplicity purposes, the VM Scheduler, Cloudlet Scheduler and VM allocation policy are predefined in the config file and are kept constant.
4. PaaS:
    1. The cloud provider gives 3 options to the broker. They include 3 types of VMs (based on resources). The broker executes using two different cloudlet schedulers (TimeShared, SpaceShared) on each of the 3 available VM types equaling to 6 total options and then finds the cheapest one.
    2. For simplicity purposes, the VM Scheduler and VM allocation policy are predefined in the config file and are kept constant.
5. IaaS:
    1. The cloud provider gives 3 options to the broker including 3 type of hosts (again, based on resources). The broker now plays with 2 cloudlet scheduling policies (TimeShared, SpaceShared) and 2 VM scheduling policies (TimeShared, SpaceShared) checking 12 different options and their costs and then picking the cheapest one.
    2. For simplicity purposes, the VM allocation policy is predefined in the config file and is kept constant.

The simulation executes all the options available to the broker to process the specific cloudlets, and then provides the cheapest option available for each type of Service.

## Sample Results
### Values may change based on the configuration.

| Number of Cloudlets | SaaS | PaaS | IaaS |
| :---: | :---: | :---: | :---: |
| 5 | 308.94 | 308.93 | 354.21 |
| 10 | 313.08 | 313.08 | 387.15 |
| 40 | 337.88 | 337.88 | 634.07 |
| 50 | 346.15 | 346.17 | 716.34 |