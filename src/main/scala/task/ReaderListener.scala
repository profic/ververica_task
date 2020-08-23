package task

import java.io.File
import java.util.concurrent.atomic.AtomicReference
import javax.annotation.concurrent.NotThreadSafe

import scala.Ordering.comparatorToOrdering

import com.typesafe.scalalogging.Logger
import net.openhft.chronicle.queue.impl.StoreFileListener
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder
import org.apache.commons.io.comparator.LastModifiedFileComparator.LASTMODIFIED_COMPARATOR

//class ReaderListener(q: SingleChronicleQueue, consumer: ExcerptTailer) extends StoreFileListener {
@NotThreadSafe
class ReaderListener(path: String) extends StoreFileListener {

  private val log = Logger(getClass)

  private val curFile = {
    val q        = SingleChronicleQueueBuilder.single(path).build()
    val consumer = q.createTailer("default")

    try {
      val curFile = Option(q.storeForCycle(consumer.cycle(), 0, false, null))
        .map(_.file)
        .map { currentFile =>
          val files            = q.file().listFiles().sorted(comparatorToOrdering(LASTMODIFIED_COMPARATOR))
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
