# broker-adapter
Adapter to allow out-of-process broker implementation.
## R Example
See the example in the broker-adapter-fs repo.

## Python Example

The Python Example connects to the adapter on two channels (send and receive) and sends one order to the server. It's just a raw xml string and nothing fancy. All received xml is output to the console.
2 things that happen already:

- two async threads and 2 blocking queues for the in/outs
- handling of `broker-accept` message.

## General idea

You can create any kind of adapter by implementing the `IpcAdapter` interface and specifying the CLI paramter `--ipc-adapter`.
See the included GRPC adapter for details.

There are some caveats. Some things need to be reimplemented that were previously handled by the already existing Java code. One such example is the handling of the

```
<broker-accept prefix="2" key="dlop5b" serverTime="1521046957413"/>
``` 

message which holds the info about key generation and prefix. The broker core takes care of prepending the key to outgoing messages, but the implementation needs to use the prefix multiplied by 100000000 to calculate ID values for domain types sent to the server, such as TariffSpecifications and Orders. This is essential; otherwise the server will discard your messages. The serverTime is the time in milliseconds UTC at which the first timeslot starts. This time advances by one timeslot-duration each time the server sends out the TimeslotUpdate message.
