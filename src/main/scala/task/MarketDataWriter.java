package task;

import net.openhft.chronicle.queue.RollCycles;
import net.openhft.chronicle.queue.impl.StoreFileListener;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.queue.ExcerptAppender;

import java.util.concurrent.atomic.AtomicLong;

import net.openhft.chronicle.queue.ExcerptTailer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MarketDataWriter {
    private static long longSequence = 0;
    private static int intSequence = 0;

    public static void main(String args[]) {
        String path = "D:\\Logs\\ChronicleData\\marketdata";
        writeMarketData(path);
    }

    private static void writeMarketData(String path) {
        ChronicleFactory chronicleFactory = new ChronicleFactory();
        SingleChronicleQueue chronicle = chronicleFactory.createChronicle(path, RollCycles.MINUTELY);

        ExcerptAppender appender = chronicle.acquireAppender();

        while (true) {
            Jvm.pause(100); //NOTE: Slowing down writer to understand file rolling
            appender.writeBytes(b -> {
                b.writeLong(getLongSequence());
                b.writeInt(getIntSequence());
            });
        }
    }

    private static long getLongSequence() {
        return longSequence++;
    }

    private static int getIntSequence() {
        return intSequence++;
    }
}

class ChronicleFactory {
    public SingleChronicleQueue createChronicle(String persistenceDir, RollCycles rollCycles) {
        SingleChronicleQueue chronicle = null;
        try {
            chronicle = SingleChronicleQueueBuilder.binary(persistenceDir)
                    .rollCycle(rollCycles)
                    .storeFileListener(new StoreFileListener() {
                        public void onReleased(int i, File file) {
                            String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
                            System.out.println(currentTime + ": " + Thread.currentThread().getName() + " onReleased called for file: " + file.getAbsolutePath() + " for cycle: " + i);
                        }

                        public void onAcquired(int cycle, File file) {
                            String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
                            System.out.println(currentTime + ": " + Thread.currentThread().getName() + " onAcquired called for file: " + file.getAbsolutePath() + " for cycle: " + cycle);
                        }
                    }).build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return chronicle;
    }

}


class SimpleMarketDataReader {

    public static void main(String args[]) {
        String pathForMarketData = "D:\\Logs\\ChronicleData\\marketdata";
        readMarketData(pathForMarketData);
    }

    public static void readMarketData(String pathForMarketDataFile) {
        ChronicleFactory chronicleFactory = new ChronicleFactory();
        SingleChronicleQueue chronicle = chronicleFactory.createChronicle(pathForMarketDataFile, RollCycles.MINUTELY);

        //Create another thread to read same file
//        SimpleMarketDataReaderNewChronicle simpleMarketDataReaderNewChronicle = new SimpleMarketDataReaderNewChronicle();
//        executor.submit(simpleMarketDataReaderNewChronicle);

        ExcerptTailer tailer = chronicle.createTailer();
        try {
            while (true) {
                tailer.readBytes(b -> {
                    b.readLong();
                    b.readInt();
                    //System.out.println("Long Sequence in SimpleMarketDataReader: " + b.readLong());
                    //System.out.println("User data is: " + userData);
                    //System.out.println("Int Sequence is: " + b.readInt());
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}