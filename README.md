net.openhft.chronicle.queue.RollCycles.LARGE_HOURLY
From descritpion: 2 billion entries per hour, indexing every 64th entry
Which means 555.5k RPS for write.

Queue can be shared between multiple clients

https://groups.google.com/g/java-chronicle/c/4-cOUnWQKnE


https://github.com/OpenHFT/Chronicle-Queue/blob/master/docs/FAQ.adoc

By default, Queue writes synchronously to the OS and lets the OS write the data when it can.
If the JVM crashes in the middle of writing a message, the message is truncated.  
If the OS crashes before data has been written, it can be lost.



for each appender, messages are written in the order the appender wrote them

Chronicle has been tested where the consumer was more than the whole of main memory behind the producer.
This reduced the maximum throughput by about half.

The limit is about 1 GB, as of Chronicle 4.x. The practical limit without tuning the configuration is about 16 MB. 
At this point you get significant inefficiencies, unless you increase the data allocation chunk size.


Asynchronous writes can be used to help with very "bursty" writes.
https://github.com/OpenHFT/Chronicle-Queue/blob/master/docs/ring_buffer.adoc#asynchronous-writes


for each appender, messages are written in the order the appender wrote them. Messages by different appenders are interleaved,

for each tailer, it will see every message for a topic in the same order as every other tailer,