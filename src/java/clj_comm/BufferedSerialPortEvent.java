package clj_comm;
import gnu.io.*;

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
