# broker-adapter
Adapter to allow out-of-process broker implementation.

## R Example
Example: The r-broker directory contains a brief working demo called forkDemo.R. It works within RStudio. Here's the process for running it:

* Create two fifos in the broker-adapter directory. The rest of this assumes you name them broker-input and broker-output
* In RStudio, source forkDemo.R
* In RStudio, change your working directory with setwd() to the broker-adapter directory.
* If you don't have a boot record, start the Power TAC server and generate a boot record.
* In the Power TAC server, set up a game using the boot record, and authorize the broker Rbroker to log in. Specify `pause.props` as the Server-config. Start the sim.
* In RStudio, run the command runBroker("broker-output", "broker-input")

You should see all the incoming xml messages streamed to the R console.

## Python Example

The Python Example connects to the adapter on two channels (send and receive) and sends one order to the server. It's just a raw xml string and nothing fancy. All received xml is output to the console.
2 things that happen already:

- two async threads and 2 blocking queues for the in/outs
- handling of `broker-accept` message.

## General idea

You can create any kind of adapter by implementing the `IpcAdapter` interface and passing the Class name of the implementing bean via CLI paramter `--ipc-adapter-name`.
See the two included adapters (GRPC and CharStream) for details.

There are some caveats though. Some things need to be reimplemented that were previously handled by the already existing Java code. One such example is the handling of the

```
<broker-accept prefix="2" key="dlop5b" serverTime="1521046957413"/>
``` 

message which holds the infos about key generation and prefix stuff. The broker core takes care of prepending the key to outgoing messages, but the implementation needs to use the prefix multiplied by 100000000 to calculate ID values for domain types sent to the server, such as TariffSpecifications and Orders. This is essential; otherwise the server will discard your messages. The serverTime is the time in milliseconds UTC at which the first timeslot starts. This time advances by one timeslot-duration each time the server sends out the TimeslotUpdate message.
