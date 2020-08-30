###1. How to build?
SBT is used for managing dependencies and make a package. Sbt-native-packager plugin is used for project assembly. You can simply run `build.sh` scripts in the package root folder, which will do all required steps for a further run.
###2. How to run?
Make sure you had run build.sh script and then execute `run.sh`.

###3. How does your system work? (if not addressed in comments in source)

All optional features has been implemented:
- restoring state after SHUTDOWN request or restarting system;
- multiple simultaneous clients are allowed.

General modifications:
- for every distinct request from client it is required to append new line character `\n` in the end of every command, e.g. `QUIT\n`, `PUT line\n` etc.
- for every response server appends `\r\n` as a sign of response end. For every line for GET request, new-line character `\n` is appended to every line, e.g. single-line response will look like `this is response\n\r\n`.
- for every successfully processed request server replies with `OK\n`;
- below is a description for unsuccessful reasons and server responses in such cases:
	- any request:
		- `INVALID_REQUEST\r\n` - when the requests is not parseable;
	- GET: 
		- `INVALID_REQUEST\r\n` when 'number' in `GET number` is not a valid integer or integer is `<= 0`;
		- `ERR\r\n` when there is no enough lines to read;
	- PUT:
		- `INVALID_REQUEST\r\n` if line does not match required pattern;
    This modification were done because of TCP nature: data is received/sent as a stream, so we need a way to distinguish requests from each other when they are consist from multiple packets.

System semantics:
- write requests are stored in the order they handled by the system. Eventual order is defined by order in which data is written to disk by Chronicle Queue. By default, Queue writes synchronously to the OS and lets the OS write the data when it can. If the JVM crashes in the middle of writing a message, the message is truncated. If the OS crashes before data has been written, it can be lost;
- read requests are always served in the order they stored;
- at-most once, e.g. fire-and-forget semantics for reads. If client is disconnected while transferring data or writing to client's socket is failed message is still marked as consumed

The system consists from the following layers:

1) Storage layer. Chronicle Queue has been chosen as storage. It is an append-only, low-latency, GC-optimized persistent storage.
Note: Read requests are sequential and synchronous, which means for reading N lines Chronicle Queue will read data message by message until N messages are read. If any exception occur `M: 1 <= M <= N`  messages are considered as consumed.
Chronicle Queue stores data in file chunks, which are rolled every hour regardless of data that has been written or read. A mechanism for cleaning up these files is handled by `task.store.ReadListener` - see scaladoc.
Currently roll cycle policy is `net.openhft.chronicle.queue.RollCycles.LARGE_HOURLY`, which means that up to `555.5k/sec` entries can be written. We can handle even more by applying larger roll policy e.g. `net.openhft.chronicle.queue.RollCycles.LARGE_HOURLY_SPARSE`
2) Network layer. Netty has been chosen as a network framework for handling TCP requests and producing responses. 
Nothing special here: requests are handled and processed in the same handler. There is a room for improvement for a request processing: now the heaviest operations such as read and write are executed
in a synchronous mode, but Netty is asynchronous in its nature so some kind of async modifications for interactions with storage layer is required.

###3. How will your system perform as the number of requests per second increases?

Network layer:
Normally, until the network bandwidth and connections count are in allowed bounds, the system will not suffer from increased RSP.

Storage layer:
The system itself uses memory-mapped files, which means that increasing RPS eventually bounded to RAM size and disk throughput. It is expected that system throughput will decrease and latency increase.

Large N value for reading will force other readers to wait until the original request is completed. It means to achieve the best overall performance reasonable small N for GET requests should be used.

###4. How will your system perform with various queue sizes?
Queue size does not have a dramatic impact on the system itself.

###5. What documentation, websites, papers, etc did you consult in doing this assignment

https://en.wikipedia.org/wiki/Write-ahead_logging <br>
https://en.wikipedia.org/wiki/Sync_(Unix) <br>

I also considered multiple other alternatives to Chronicle-Queue: <br>

https://github.com/mrwilson/java-dirt <br>
https://github.com/bulldog2011/bigqueue <br>
https://github.com/jankotek/mapdb <br>
https://github.com/xxlabaza/log-file <br>

###6. What third-party libraries or other tools does the system use?

[Chronicle-queue](https://github.com/OpenHFT/Chronicle-Queue) is heavily used for storing data.
Core features from [Netty](https://github.com/netty/netty) are used for managing all network-related stuff.

###7. How long did you spend on this exercise?
I can't say the exact number of hours, I think 40-60 hours is a correct estimation.
