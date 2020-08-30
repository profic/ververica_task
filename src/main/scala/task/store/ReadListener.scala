package task.store

import java.io.File
import java.util.concurrent.atomic.AtomicReference

import com.typesafe.scalalogging.Logger
import javax.annotation.concurrent.NotThreadSafe
import net.openhft.chronicle.queue.RollCycles
import net.openhft.chronicle.queue.impl.StoreFileListener
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder
import org.apache.commons.io.comparator.LastModifiedFileComparator.LASTMODIFIED_COMPARATOR
import task.Constants

import scala.Ordering.comparatorToOrdering

/**
 * The purpose of this class is to delete useless files produced by Chronicle Queue. File considered
 * as a  useless if the position of the consumer is beyond the index range of the file.
 *
 * Files are scanned and deleted (if any) on object creation, then it starts to listen for a current file release.
 * Due to Chronicle Queue semantics only onAcquired method is called, so we need to remember previous acquired
 * file for being able to delete.
 *
 * @param path -  data folder
 */
@NotThreadSafe
class ReadListener(path: String) extends StoreFileListener {

  private val log = Logger(getClass)

  private val curFile = {
    val q        = SingleChronicleQueueBuilder.single(path).rollCycle(RollCycles.LARGE_HOURLY).build()
    val consumer = q.createTailer(Constants.DefaultTailerName)

    try {
      val curFile = Option(q.storeForCycle(consumer.cycle(), 0, false, null))
        .map(_.file)
        .map { currentFile =>
          val files            = q.file().listFiles().sorted(comparatorToOrdering(LASTMODIFIED_COMPARATOR)) // todo: replace with net.openhft.chronicle.queue.util.FileUtil?
          val toBeDeletedFiles = files.filterNot(_.getName.contains("metadata")).takeWhile(_ != currentFile)
          log.info("{} files will be deleted", toBeDeletedFiles.length)
          toBeDeletedFiles.foreach(deleteFile)
          currentFile
        }.orNull

      new AtomicReference[File](curFile)
    } finally {
      consumer.close()
      q.close()
    }
  }

  override def onReleased(cycle: Int, file: File): Unit = () // noop

  override def onAcquired(cycle: Int, file: File): Unit = Option(curFile.getAndSet(file)).foreach(deleteFile)

  private def deleteFile(prevFile: File): Unit = {
    if (prevFile.delete()) {
      log.info("File {} deleted", prevFile.getAbsolutePath)
    } else {
      log.error("Deleting file {} is unsuccessful", prevFile.getAbsolutePath)
    }
  }
}
