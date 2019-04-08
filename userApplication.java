import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
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
    static final int CLIENT_PORT = 48013;
    static final int SERVER_PORT = 38013;
    static final String ECHO_CODE = "E8058";
    static final String IMAGE_CODE = "M9969";
    static final String AUDIO_CODE = "A1205";
    static final String COPTER_CODE = "Q3759";
    static final String VEHICLE_CODE = "V8721";
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
				"5.Sound Request Code(AQDPCM)", "6.Ithakicopter TCP", "7.Vehicle",
				"8.Exit"};
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
                        soundAQDPCM(AUDIO_CODE + "AQF");
                        break;
                    case 6:
						ithakicopterTCP(COPTER_CODE);
						break;
					case 7:
						vehicle(VEHICLE_CODE);
                    case 8:
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
        String filename = "../log/echo/" + code + ".csv";
        BufferedWriter log = new BufferedWriter(new FileWriter(filename));
        log.write("Packet, Response Time, Time elapsed");
        log.newLine();

        filename = "../log/echo/" + code + "_window.csv";
        BufferedWriter window = new BufferedWriter(new FileWriter(filename));
        window.write("Window, Duration, Packets, Throughput");
        window.newLine();

        filename = "../log/echo/" + code + "_temp.csv";
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
        String filename = "../log/DPCM/" + code + "_subs.csv";
        BufferedWriter subs = new BufferedWriter(new FileWriter(filename));
        subs.write("sub, value");
		subs.newLine();
		
		filename = "../log/DPCM/" + code + "_freqs.csv";
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

        byte[] freqs = new byte[128 * 2 * packetCount];
        

        reqSocket.send(reqPacket);
        resSocket.setSoTimeout(1000);
        // extract all the freqs
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
                    freqs[index] = (index == 0) ? (byte) 0 : (byte) (b * sub1 + freqs[index + 1]); 
					freqs[index + 1] = (byte) (b * (sub2) + freqs[index]);

					subs.write(String.format("%d,%d\n", index, sub1)); 
					subs.write(String.format("%d,%d\n", index + 1, sub2)); 
					sampls.write(String.format("%d,%d\n", index, freqs[index])); 
					sampls.write(String.format("%d,%d\n", index + 1, freqs[index + 1])); 
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
		dl.write(freqs, 0, 256 * packetCount);
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
	
    public static void soundAQDPCM(String code) throws IOException, LineUnavailableException {
        int packetCount = 500, b = 2;
        String packetInfo = code + packetCount;
		System.out.println(packetInfo);
		
        // File creation
        String filename = "../log/AQDPCM/" + code + "_subs.csv";
        BufferedWriter subtr = new BufferedWriter(new FileWriter(filename));
        subtr.write("sub, value");
		subtr.newLine();
		
		filename = "../log/AQDPCM/" + code + "_freqs.csv";
        BufferedWriter sampls = new BufferedWriter(new FileWriter(filename));
        sampls.write("sample, value");
		sampls.newLine();
		
		filename = "../log/AQDPCM/" + code + "_means.csv";
        BufferedWriter means = new BufferedWriter(new FileWriter(filename));
        means.write("mean, value");
		means.newLine();
		
		filename = "../log/AQDPCM/" + code + "_betas.csv";
        BufferedWriter betas = new BufferedWriter(new FileWriter(filename));
        betas.write("beta, value");
        betas.newLine();

        // Packet spec
        byte[] txbuffer = packetInfo.getBytes();
        DatagramPacket reqPacket = 
            new DatagramPacket(txbuffer, txbuffer.length, InetAddress.getByName(SERVER_IP), SERVER_PORT);
        byte[] rxbuffer = new byte[132];
        DatagramPacket resPacket = new DatagramPacket(rxbuffer, rxbuffer.length);
        // Handle sockets
        DatagramSocket resSocket = new DatagramSocket(CLIENT_PORT);
		DatagramSocket reqSocket = new DatagramSocket();
		
		byte[] meanB = new byte[4];
		byte[] betta = new byte[4];
		byte sign;
		byte[] freqs = new byte[256 * 2 * packetCount];
		int rx, sub1, sub2, sample1 = 0, sample2 = 0, counter = 4, mean, beta, hint = 0, sumplCount = 0;

		reqSocket.send(reqPacket);
        resSocket.setSoTimeout(1000);
		
		for(int i = 1; i < packetCount; i++){
			if (i % 100 == 0) System.out.println(i);
			try{
				resSocket.receive(resPacket);
				sign = (byte)( ( rxbuffer[1] & 0x80) !=0 ? 0xff : 0x00); //converting byte[2] to integer
				meanB[3] = sign; 
				meanB[2] = sign;
				meanB[1] = rxbuffer[1];
				meanB[0] = rxbuffer[0];
				mean = ByteBuffer.wrap(meanB).order(ByteOrder.LITTLE_ENDIAN).getInt();
				means.write(String.format("%d,%d\n", i, mean));
				sign = (byte)( ( rxbuffer[3] & 0x80) !=0 ? 0xff : 0x00);
				betta[3] = sign;
				betta[2] = sign;
				betta[1] = rxbuffer[3];
				betta[0] = rxbuffer[2];
				beta = ByteBuffer.wrap(betta).order(ByteOrder.LITTLE_ENDIAN).getInt();
				betas.write(String.format("%d,%d\n", i, beta));
				for (int j = 4;j <= 131; j++){
					rx = rxbuffer[j];
					sub1 = (int)(rx & 0x0000000F)-8;
					sub2 = (int)((rxbuffer[j] & 0x000000F0)>>4)-8;
					subtr.write(String.format("%d,%d\n%d,%d\n", ++sumplCount, sub1, ++sumplCount, sub2));
					sub1 = sub1*beta;
					sub2 = sub2*beta;
					sample1 = hint + sub1 + mean;
					sample2 = sub1 + sub2 + mean;
					hint = sub2;
					counter += 4;
					freqs[counter] = (byte)(sample1 & 0x000000FF);
					freqs[counter + 1] = (byte)((sample1 & 0x0000FF00)>>8);
					freqs[counter + 2] = (byte)(sample2 & 0x000000FF);
					freqs[counter + 3] = (byte)((sample2 & 0x0000FF00)>>8);
					sampls.write(String.format("%d,%d\n%d,%d\n%d,%d\n%d,%d\n", 
						counter, freqs[counter], counter + 1, freqs[counter + 1], 
						counter + 2, freqs[counter + 2], counter + 3, freqs[counter + 3]));
				}
			}catch(Exception x){
				System.out.println(x);
			}
		}
		
		AudioFormat FAudio = new AudioFormat(8000, 16, 1, true, false);
		SourceDataLine dl = AudioSystem.getSourceDataLine(FAudio);
		dl.open(FAudio,32000);
		dl.start();
		dl.write(freqs, 0, 256*2*packetCount);
		dl.stop();
		dl.close();

		// close connections
		resSocket.close();
		reqSocket.close();
		//handle file streams
		subtr.flush();
		sampls.flush();
		subtr.close();
		sampls.close();
		means.flush();
		means.close();
		betas.flush();
		betas.close();
	};

    public static void ithakicopter(String code) throws IOException{
		 // File creation
        String filename = "../log/copter/" + code + ".csv";
        BufferedWriter log = new BufferedWriter(new FileWriter(filename));

        // Packet spec
        byte[] rxbuffer = new byte[5000];
        DatagramPacket resPacket = new DatagramPacket(rxbuffer, rxbuffer.length);
        // Handle sockets
        DatagramSocket resSocket = new DatagramSocket(48038);
		DatagramSocket reqSocket = new DatagramSocket();

		for (int i = 0; i < 10; i++){
			try {
				resSocket.receive(resPacket);
				String message = new String(rxbuffer, 0, resPacket.getLength());
				log.write(message);
				log.newLine();
				System.out.println(new String(rxbuffer));
			} catch (Exception x) {
				System.out.println(x);
			}

		}
		// close connections
		resSocket.close();
		reqSocket.close();
		//handle file streams
		log.flush();
		log.close();
	};

	public static void ithakicopterTCP(String code) throws IOException{
		Socket s = new Socket(SERVER_IP, 38048);
		InputStream in = s.getInputStream();
		OutputStream out = s.getOutputStream();

		String packet = "AUTO FLIGHTLEVEL=100 LMOTOR=154 RMOTOR=002 PILOT \r\n";
		out.write(packet.getBytes());
		String res = "";
		int x = in.read();
		while (x != -1){
			res += (char) x;
			x = in.read();
		}
		System.out.println(res);

		in.close();
		out.close();
		s.close();
   };

	public static void vehicle(String code) throws IOException{
        String filename = "../log/vehicle/" + code + ".csv";
        BufferedWriter log = new BufferedWriter(new FileWriter(filename));
        log.write("Run Time,Air temp,Throttle pos,Engine RPM,Speed,Coolant temp");
        log.newLine();

        code = code + "OBD=01 ";
        String[] packetInfo = {code + "1F", code + "0F", code + "11", code + "0C", code + "0D", code + "05"};
        // Packet spec
		byte[] rxbuffer = new byte[2048];
        DatagramPacket resPacket = new DatagramPacket(rxbuffer, rxbuffer.length);
        // Handle sockets
        DatagramSocket resSocket = new DatagramSocket(CLIENT_PORT);
        DatagramSocket reqSocket = new DatagramSocket();

        long timeStart, end, dt = 0;	
        timeStart = System.currentTimeMillis();
        System.out.println("Run Time\tAir temp\tThrottle pos\tEngine RPM\tSpeed\tCoolant temp");
		while (dt < DURATION){
            for (int i = 0; i < 6; i ++){
                byte[] txbuffer = packetInfo[i].getBytes();
                DatagramPacket reqPacket = 
                    new DatagramPacket(txbuffer, txbuffer.length, InetAddress.getByName(SERVER_IP), SERVER_PORT);
                try {
                    reqSocket.send(reqPacket);
                    resSocket.setSoTimeout(3200);
                    resSocket.receive(resPacket);
                    double result = formula(new String(rxbuffer), i);
                    System.out.printf("%.02f\t\t", result);
                    log.write(String.format("%.03f,", result));
                } catch (Exception x){
                    System.out.println(x);
                    return;
				}
            }
            log.newLine();
            System.out.println(); 
            end = System.currentTimeMillis();
            dt = end - timeStart;
        }		
        // close connections
        resSocket.close();
        reqSocket.close();
        // handle file strem
        log.flush();
        log.close();
	};
    
    private static float formula(String rx, int index){
        String[] p = rx.split(" ");
        float XX = Integer.parseInt(p[2], 16);
        float YY = Integer.parseInt(p[3].substring(0, 2), 16); // \r causes problems on the end of p[3]

        switch(index){
            case 0:
                return 256 * XX + YY;
            case 1:
                return XX - 40;
            case 2:
                return XX * 100 / 255;
            case 3:
                return ((XX * 256) + YY) / 4;
            case 4:
                return XX;
            case 5:
                return XX - 40;
        }
        return 1;
    }
}