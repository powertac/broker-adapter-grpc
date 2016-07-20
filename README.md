# broker-adapter
Adapter to allow out-of-process broker implementation.

Example: The r-broker directory contains a brief working demo called forkDemo.R. It works within RStudio. Here's the process for running it:

* Create two fifos in the broker-adapter directory. The rest of this assumes you name them broker-input and broker-output
* In RStudio, source forkDemo.R
* If you don't have a boot record, start the Power TAC server and generate a boot record.
* In the Power TAC server, set up a game using the boot record, and authorize the broker Rbroker to log in. Start the sim.
* In RStudio, run the command runBroker("broker-output", "broker-input")

You should see all the incoming xml messages streamed to the R console.
