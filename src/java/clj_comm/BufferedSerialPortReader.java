package clj_comm;

import java.util.Vector;
import java.util.Iterator;
import java.io.*;
import gnu.io.*;


public class BufferedSerialPortReader implements SerialPortEventListener
{
  private InputStream is;
  private ByteArrayOutputStream os;
  private int maxBufSize;
  private int bytesInBuffer;
  private Vector<byte[]> triggers;
  private Vector<BufferedSerialPortEventListener> listeners;

  // constructor argument wrong when same name?????
  public BufferedSerialPortReader( InputStream is, int maxBufSize, Vector<byte[]> triggers ) {
    this.is = is;
    this.os = new ByteArrayOutputStream();
    this.maxBufSize = maxBufSize;
    this.bytesInBuffer = 0;
    this.triggers = triggers;
    this.listeners = new Vector<BufferedSerialPortEventListener>();
  }

  public void addEventListener( BufferedSerialPortEventListener l ) {
    listeners.add( l );
  }

  public void removeEventListener( BufferedSerialPortEventListener l ) {
    for( Iterator<BufferedSerialPortEventListener> it = listeners.iterator(); it.hasNext(); ) {
      BufferedSerialPortEventListener n = it.next();
      if( n == l )
        it.remove();
    }
  }

  public void removeEventListener() {
    for( Iterator<BufferedSerialPortEventListener> it = listeners.iterator(); it.hasNext(); ) {
      BufferedSerialPortEventListener n = it.next();
        it.remove();
    }
  }

  private boolean byteHaystackHasNeedle( byte[] hayStack, byte[] needle, int hayStackStartIndex ) {
    int n, h;
    for( h=hayStackStartIndex; h<hayStack.length; ++h ) {
      for( n=0; n<needle.length && n+h < hayStack.length; ++n ) {
        if( hayStack[h+n] != needle[n] )
          break;
      }
      if( n == needle.length )
        return true;
    }
    return false;
  }

  public void serialEvent(SerialPortEvent ev)  {
    try {
      boolean trigger = false;

      if( ev.getEventType() == SerialPortEvent.DATA_AVAILABLE ) {
        // read from is and append result to buffer
        byte[] chunk = new byte[is.available()];
        int bytesRead = is.read( chunk );
        /*
        System.out.println("got "+new Integer(bytesRead).toString()+" bytes: "+new String(chunk)
        +", first byte: " + new Integer(chunk[0]).toString()); */
        os.write( chunk, 0, bytesRead );
        bytesInBuffer += bytesRead;

        if( bytesInBuffer >= maxBufSize )
          trigger = true;
        else {
          for( Iterator<byte[]> it = triggers.iterator(); it.hasNext(); ) {
            byte[] hayStack = os.toByteArray(); byte[] needle = it.next();
            trigger = byteHaystackHasNeedle( hayStack, needle, hayStack.length - needle.length );
            if( trigger )
              break;
          }
        }
      }
      else {
        trigger = true; // any other event forward immediately
      }

      if( trigger ) {
        System.out.println("*** triggered, got: "+os.toString());
        // if condition found then trigger
        for( Iterator<BufferedSerialPortEventListener> it = listeners.iterator(); it.hasNext(); ) {
          BufferedSerialPortEventListener l = it.next();
          l.bufferedSerialEvent( new BufferedSerialPortEvent( ev, os.toByteArray() ) );
        }
        os.reset();
        bytesInBuffer = 0;
      }
    } catch( IOException e) { System.err.println( "IOExcepton occured!" ); }
  }

  public static void main( String[] args ) throws Exception
    {
      if(args.length < 1 ) {
        System.err.println("invocation: java BufferedSerialPortReader <dev-file>");
        return;
      }

      System.out.println("opening device file "+args[0]);
      CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(args[0]);
      CommPort commPort = portIdentifier.open("test",2000);
      SerialPort port = (SerialPort) commPort;
      port.setSerialPortParams(115200,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
      InputStream is = port.getInputStream();
      OutputStream os = port.getOutputStream();
      os.write(new String("Hello, world!\n").getBytes());
      System.out.println("+++ written!\n");

      Vector<byte[]> triggers = new Vector<byte[]>();
      triggers.add(new String("\n").getBytes());
      triggers.add(new String("\r").getBytes());
      triggers.add(new String("z").getBytes());
      triggers.add(new String("eol").getBytes());

      BufferedSerialPortReader bufferedReader = new BufferedSerialPortReader( is, 30, triggers );
      port.addEventListener( bufferedReader );
      port.notifyOnDataAvailable(true);

      System.out.println("wait for 10 seconds ...");
      try {
        Thread.sleep(10000);                 // 10 seconds
      } catch(InterruptedException ex) {
        Thread.currentThread().interrupt();
      }



      System.out.println("now deregister listeners ...");
      port.notifyOnDataAvailable(false);
      port.removeEventListener();
      System.out.println("listeners are successfully deregistered");

      System.out.println("wait another 3 seconds ...");
      try {
        Thread.sleep(3000);                 // 3 seconds
      } catch(InterruptedException ex) {
        Thread.currentThread().interrupt();
      }

      System.out.println("now close port ...");
      port.close();
      System.out.println("port closed");

      System.out.println("done");


/*
      os.close();
      System.out.println("+++ os closed!\n");
      is.close();
      System.out.println("+++ is closed!\n");
      System.exit(0);
*/
    }

}
