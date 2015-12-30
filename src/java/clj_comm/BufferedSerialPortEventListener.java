/**
 *   Clojure Adapter for the Java Communications API
 *
 *   by Otto Linnemann
 *   (C) 2015, Eclipse Public License
 */
package clj_comm;
import java.util.*;

/** Listener for notification when input buffer is completely filled */
public interface BufferedSerialPortEventListener extends EventListener
{
  /**
   * invoked when the sequence of trigger chaacters has been recieved.
   * all buffered characters which have been red from serial line
   * are returned as event and the buffer chache is emptied afterwards.
   */
	public abstract void bufferedSerialEvent( BufferedSerialPortEvent ev );
}
