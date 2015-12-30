/**
 *   Clojure Adapter for the Java Communications API
 *
 *   by Otto Linnemann
 *   (C) 2015, Eclipse Public License
 */
package clj_comm;
import gnu.io.*;


/**
 *   Event class for serial port buffer completion
 */
public class BufferedSerialPortEvent extends SerialPortEvent
{
  private byte[] buffer;

	public BufferedSerialPortEvent( SerialPortEvent serialPortEvent, byte[] buffer ) {
		super( (SerialPort) serialPortEvent.getSource(),
           serialPortEvent.getEventType(),
           serialPortEvent.getOldValue(),
           serialPortEvent.getNewValue() );
    this.buffer = buffer;
	}

	public byte[] getBuffer() {
		return(buffer);
	}
}
