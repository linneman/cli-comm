package clj_comm;
import java.util.*;

public interface BufferedSerialPortEventListener extends EventListener
{
	public abstract void bufferedSerialEvent( BufferedSerialPortEvent ev );
}
