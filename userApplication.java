import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Scanner;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;


public class userApplication {

	private static Scanner in;
    
    static final String CLIENT_IP = "";
    static final String SERVER_IP = "155.207.18.208";
    static final int CLIENT_PORT = 48023;
    static final int SERVER_PORT = 38023;
    static final String ECHO_CODE = "E7905";
    static final String IMAGE_CODE = "M8081";
    static final String AUDIO_CODE = "A3368";
    static final String COPTER_CODE = "";
    static final String VEHICLE_CODE = "";
    static final long DURATION = 10 * 1 * 1000;


	public static void main(String[] args) throws IOException {
		userApplication.options();
	}
	
	// Creation of menu for choosing the desirable option
	public static void options(){
		while (true) {
            // Print menu
            String[] menu = {"1.Echo Request Code", "2.Echo Request Code No Delay",
                "3.Image Request Code", "4.Sound Request Code(DPCM)",
                "5.Sound Request Code(AQDPCM)", "6.Ithakicopter TCP", "7.Exit"};
            for (String x: menu) System.out.println(x);

            // Get user input
            int choice;
            in = new Scanner(System.in);
			try{
                choice = in.nextInt();
                switch (choice){
                    case 1:
                        echo(ECHO_CODE);
                        break;
                    case 2:
                        echo("E0000");
                        break;
                    case 3:
                        image(IMAGE_CODE);
                        image(IMAGE_CODE + "CAM=PTZ");
                        break;
                    case 4:
						soundDPCM(AUDIO_CODE + 'F');
						soundDPCM(AUDIO_CODE + 'T');
                        break;
                    case 5:
                        soundAQDPCM();
                        break;
                    case 6:
                        ithakicopter();
                    case 7:
                        return;
                    default:
                    System.out.println("Try again");
                }
			}catch(Exception x){
                System.out.println(x);
                return;
			}
	    }
	}

	public static void echo(String code) throws IOException{
        
        String packetInfo = code + "\r";
        ArrayList<Long> responseTimes = new ArrayList<Long>();
        
        // File creation
        String filename = "../log/echo_" + code + ".csv";
        BufferedWriter log = new BufferedWriter(new FileWriter(filename));
        log.write("Packet, Response Time, Time elapsed");
        log.newLine();

        filename = "../log/echo_" + code + "_window.csv";
        BufferedWriter window = new BufferedWriter(new FileWriter(filename));
        window.write("Window, Duration, Packets, Throughput");
        window.newLine();

        filename = "../log/echo_" + code + "_temp.csv";
        BufferedWriter temperature = new BufferedWriter(new FileWriter(filename));


        // Packet spec
        byte[] txbuffer = packetInfo.getBytes();
        DatagramPacket reqPacket = 
            new DatagramPacket(txbuffer, txbuffer.length, InetAddress.getByName(SERVER_IP), SERVER_PORT);
		byte[] rxbuffer = new byte[2048];
        DatagramPacket resPacket = new DatagramPacket(rxbuffer, rxbuffer.length);
        // Handle sockets
        DatagramSocket resSocket = new DatagramSocket(CLIENT_PORT);
        DatagramSocket reqSocket = new DatagramSocket();

        long timeStart, start, end, delta, dt = 0; int packetCount = 0;	

        timeStart = System.currentTimeMillis();
		while (dt < DURATION){
            start = System.currentTimeMillis();
			reqSocket.send(reqPacket);
            resSocket.setSoTimeout(3200);
			while (true) {
				try{
					resSocket.receive(resPacket);
					end = System.currentTimeMillis();
                    delta = end - start; dt = end - timeStart;
                    responseTimes.add(delta);

                    System.out.print(new String(rxbuffer));
                    System.out.printf("\tResponse time: %d ms \tTime elapsed: %d ms\n", delta, dt);

                    log.write(String.format("%d,%d,%d", ++packetCount, delta, dt));
                    log.newLine();
					break;
				}catch (Exception x){
					System.out.println(x);
				}
			}
        }
        // temperature readings
        for (int i = 0; i < 8; i++) {
            packetInfo = String.format("%sT00\r", code);
            txbuffer = packetInfo.getBytes();
            reqPacket = 
                new DatagramPacket(txbuffer, txbuffer.length, InetAddress.getByName(SERVER_IP), SERVER_PORT);
            try {
                reqSocket.send(reqPacket);
                resSocket.setSoTimeout(3200);
                resSocket.receive(resPacket);
                String rcv = new String(rxbuffer);
                temperature.write(rcv);
                temperature.newLine();
                System.out.println(rcv);
            } catch (Exception x) {
                System.out.println(x);
            }
        }
        // Throughput calculation for every 8 sec window
        int windowCount = 0; packetCount = 0;
        long sumInt = 0;
		for (int i = 0; i < responseTimes.size(); i++){
			for (int j = i; j < responseTimes.size(); j++){
                long temp = sumInt + responseTimes.get(j);
                if (temp > 8 * 1000) {
                    break;
                } else {
                    sumInt = temp;
                    packetCount++;
                }
            }
            // R_window (bits/sec)
            double duration = (double) sumInt / 1000;
            double R = (packetCount * 32 * 8) / duration; // (32 bytes)*(8 bits)/(sumInt secs)
            window.write(String.format("%d,%f,%d,%f", ++windowCount, duration, packetCount, R));
            window.newLine();
			packetCount = 0;
            sumInt = 0;

		}

        // close connections
        resSocket.close();
        reqSocket.close();
        // handle file streams
        log.flush();
        window.flush();
        temperature.flush();
        log.close();
        window.close();
        temperature.close();
	
    }
    
    public static void image(String code) throws IOException{
        String packetInfo = code + "\r";
        
        // File creation
        String filename = "../img/" + code + ".jpg";
        OutputStream image = new FileOutputStream(filename);

        // Packet spec
        byte[] txbuffer = packetInfo.getBytes();
        DatagramPacket reqPacket = 
            new DatagramPacket(txbuffer, txbuffer.length, InetAddress.getByName(SERVER_IP), SERVER_PORT);
		byte[] rxbuffer = new byte[2048];
        DatagramPacket resPacket = new DatagramPacket(rxbuffer, rxbuffer.length);
        // Handle sockets
        DatagramSocket resSocket = new DatagramSocket(CLIENT_PORT);
        DatagramSocket reqSocket = new DatagramSocket();

        reqSocket.send(reqPacket);
        resSocket.setSoTimeout(3200);
		
		for(;;){
			try{
                resSocket.receive(resPacket);
				image.write(rxbuffer, 0, 128);
			}catch (IOException ex) {
				System.out.println(ex);
				break;
			}
        }
        // close connections
        resSocket.close();
        reqSocket.close();
        // handle file stream
        image.flush();
		image.close();
    };

    public static void soundDPCM(String code) throws IOException, LineUnavailableException {

        int packetCount = 500, b = 2;
        String packetInfo = code + packetCount;
		System.out.println(packetInfo);
		
        // File creation
        String filename = "../log/soundDPCM_subs_" + code + ".csv";
        BufferedWriter subs = new BufferedWriter(new FileWriter(filename));
        subs.write("sub, value");
		subs.newLine();
		
		filename = "../log/soundDPCM_samples_" + code + ".csv";
        BufferedWriter sampls = new BufferedWriter(new FileWriter(filename));
        sampls.write("sample, value");
        sampls.newLine();

        // Packet spec
        byte[] txbuffer = packetInfo.getBytes();
        DatagramPacket reqPacket = 
            new DatagramPacket(txbuffer, txbuffer.length, InetAddress.getByName(SERVER_IP), SERVER_PORT);
        byte[] rxbuffer = new byte[128];
        DatagramPacket resPacket = new DatagramPacket(rxbuffer, rxbuffer.length);
        // Handle sockets
        DatagramSocket resSocket = new DatagramSocket(CLIENT_PORT);
        DatagramSocket reqSocket = new DatagramSocket();

        byte[] samples = new byte[128 * 2 * packetCount];
        

        reqSocket.send(reqPacket);
        resSocket.setSoTimeout(1000);
        // extract all the samples
        for(int i = 0; i < packetCount; i++){
            try {
                if (i % 100 == 0) System.out.println(i);
				int sub1, sub2;
                resSocket.receive(resPacket);
                for (int j = 0; j < 128; j++){
                    int a = rxbuffer[j];
                    int index = i*256 + 2*j;
                    sub1 = ((a >> 4) & 15) - 8;
                    sub2 = (a & 15) - 8;
                    samples[index] = (index == 0) ? (byte) 0 : (byte) (b * sub1 + samples[index + 1]); 
					samples[index + 1] = (byte) (b * (sub2) + samples[index]);

					subs.write(String.format("%d,%d\n", index, sub1)); 
					subs.write(String.format("%d,%d\n", index + 1, sub2)); 
					sampls.write(String.format("%d,%d\n", index, samples[index])); 
					sampls.write(String.format("%d,%d\n", index + 1, samples[index + 1])); 
				}
            } catch (Exception x){
                System.out.println(x);
            }
        }

        AudioFormat FAudio = new AudioFormat(8000, 8, 1, true, false);
        SourceDataLine dl = AudioSystem.getSourceDataLine(FAudio);
        System.out.println("Playing sound");
        dl.open(FAudio, 32000);
		dl.start();
		dl.write(samples, 0, 256 * packetCount);
		dl.stop();
        dl.close();
        
        // close connections
        resSocket.close();
		reqSocket.close();
		// handle file streams
		subs.flush();
		sampls.flush();
		subs.close();
		sampls.close();
	};
	
    public static void soundAQDPCM() throws IOException, LineUnavailableException {};
    public static void ithakicopter() throws IOException{};
	
}