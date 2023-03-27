package lego;

import com.fazecast.jSerialComm.SerialPort;


public class SerialTest {
    private static final String PORT_NAME = "Arduino Leonardo";
    private static final int rate = 9600;


    public static void main(String[] args) throws Exception {
        SerialPort[] commPorts = SerialPort.getCommPorts();
        for (SerialPort sp : commPorts) {
            if (PORT_NAME.equals(sp.getDescriptivePortName())) {
                System.out.println(sp.getDescriptivePortName() + " => " + sp.getSystemPortName());
                open(sp);
                Thread.sleep(4000);
                sp.closePort();
            }
        }
    }

    private static void open(SerialPort comPort) throws InterruptedException {
        comPort.setComPortParameters(rate, 8, 1, SerialPort.NO_PARITY);
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);
        comPort.openPort();
        System.out.println("opened port " + comPort.getDescriptivePortName() + " => " + comPort.getSystemPortName() + " => " + comPort.isOpen());

        if (!comPort.isOpen()) {
            throw new IllegalStateException("Com port is not open");
        }

        Thread.sleep(2000);

        byte[] sendData = new byte[]{(byte) 60};
        int writeBytes = comPort.writeBytes(sendData, 1);
        System.out.println("Written: " + writeBytes);

        Thread.sleep(2000);
       // sendData = new byte[]{(byte) 60};
       // writeBytes = comPort.writeBytes(sendData, 1);
        System.out.println("Written: " + writeBytes);
    }
}