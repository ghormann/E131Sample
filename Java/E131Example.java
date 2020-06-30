import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * This is an example program that sends E131 packets to multiple universes on
 * a single IP Address.   In Order to test with it, you'll need to 
 * 1) Update the value on "ipAddr" to the ipAddress of your AlphaPix
 * 2) Change numUniverseToSend (line 48) to reduce the number of Universes updated at once (anything > 5 causes problems on my 2 AlphaPix controllers)
 * 
 * @author Greg Hormann (ghormann@gmail.com)
 *
 */
public class E131Example {
	public static final int packetSize = 638;
	public static final int port = 5568;
	public static final int numchannels = 512;
	public static final String UUID = "c0de0080c69b11e095720800200c9a66";
	public static final int numUniverse = 19;
	public static final String ipAddr1 = "192.168.1.148";
	//public static final int universeNumbers[] = new int[] {100,11,12,13, 20,21,22,23, 30,31,32,32, 40,41,42,43};
	public static final int universeNumbers[] = new int[] {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
	//public static final int universeNumbers[] = new int[] {1,2};

	private DatagramSocket clientSocket = new DatagramSocket();

	public Universe universes[] = new Universe[numUniverse];

	public E131Example() throws Exception {
	        InetAddress IPAddress;
		IPAddress = InetAddress.getByName(ipAddr1);
		for (int i = 0; i < universeNumbers.length; i++) {
			universes[i] = new Universe(universeNumbers[i], IPAddress);
		}
	}

	private void sleepMs(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
        private int getRandom()
        {
           return (int)(Math.random() * 256);
        }
	public void loopColors() {
	   int numUniverseToSend = numUniverse;
           int r=0;
           int g=0;
           int b=0;
           int rDir = 1;
           int bDir = 1;
           int gDir = 1;
           while (true) {
              r += rDir;
              g += gDir;
              b += bDir;
              if (r > 255 || r < 0) { r = getRandom(); rDir = (rDir * -1); }
              if (g > 255 || g < 0) { g = getRandom(); bDir = (bDir * -1); }
              if (b > 255 || b < 0) { b = getRandom(); gDir = (gDir * -1); }
	      System.out.println("r=" + r + ", g=" + g + ", b=" + b);
	      for (int i = 0; i < numUniverseToSend; i++) {
	         universes[i].setAll(r, g, b);
		 universes[i].sendData();
                 //sleepMs(2);
              }
	      sleepMs(100) ;
           }
        }

	public void sendData() {

		//int numUniverseToSend = 32;
		int numUniverseToSend = numUniverse;

		while (true) {
			System.out.println("Sending Red");
			for (int i = 0; i < numUniverseToSend; i++) {
				universes[i].setAll(255, 0, 0);
				universes[i].sendData();
			}
			sleepMs(1000);

			System.out.println("Sending Green");
			for (int i = 0; i < numUniverseToSend; i++) {
				universes[i].setAll(0, 255, 0);
				universes[i].sendData();
			}
			sleepMs(1000);
			System.out.println("Sending Blue");
			for (int i = 0; i < numUniverseToSend; i++) {
				universes[i].setAll(0, 0, 255);
				universes[i].sendData();
			}
			sleepMs(1000);
			System.out.println("Sending White");
			for (int i = 0; i < numUniverseToSend; i++) {
				universes[i].setAll(255, 255, 255);
				universes[i].sendData();
			}
			sleepMs(1000);
		}
	}


	private class Universe {
		public byte[] data = new byte[packetSize];
		private InetAddress ip;
		private int universe;
		private int sequenceNum = 0;
		
		/*
		 * Note: Base E1.31 Implementation based on XLights 3 Implementation of E1.31.  released under GNU GENERAL PUBLIC LICENSE
		 * Converted to Java by Greg Hormann
		 * 
		 */

		public void sendData() {
			// Increment Sequence number before sending per E1.31 Spec
			if (++sequenceNum > 254) {
				sequenceNum = 0;
			}
			data[111] = (byte)sequenceNum;
			DatagramPacket sendPacket = new DatagramPacket(data, data.length, ip, port);
			//System.out.println(ip.toString + ": " + universe + ": send" );
			try {
				clientSocket.send(sendPacket);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

		}

		public void setAll(int r, int g, int b) {

			for (int i = 126; i < 512 + 126 - 3; i += 3) {
				data[i] = (byte) r;
				data[i + 1] = (byte) g;
				data[i + 2] = (byte) b;
			}
		}

		public Universe(int universe, InetAddress ip) {
			this.universe = universe;
			this.ip = ip;
			byte univHi = (byte) (universe >> 8); // Universe Number (high)
			byte univLo = (byte) (universe & 0xff); // Universe Number (low)
			data[0] = 0x00; // RLP preamble size (high)
			data[1] = 0x10; // RLP preamble size (low)
			data[2] = 0x00; // RLP postamble size (high)
			data[3] = 0x00; // RLP postamble size (low)
			data[4] = 0x41; // ACN Packet Identifier (12 bytes)
			data[5] = 0x53;
			data[6] = 0x43;
			data[7] = 0x2d;
			data[8] = 0x45;
			data[9] = 0x31;
			data[10] = 0x2e;
			data[11] = 0x31;
			data[12] = 0x37;
			data[13] = 0x00;
			data[14] = 0x00;
			data[15] = 0x00;
			data[16] = 0x72; // RLP Protocol flags and length (high)
			data[17] = 0x6e; // 0x26e = 638 - 16
			data[18] = 0x00; // RLP Vector (Identifies RLP Data as 1.31 Protocol
								// PDU)
			data[19] = 0x00;
			data[20] = 0x00;
			data[21] = 0x04;

			// CID/UUID

			char msb, lsb;
			for (int i = 0, j = 22; i < 32; i += 2) {
				msb = UUID.charAt(i);
				lsb = UUID.charAt(i + 1);
				msb -= Character.isDigit(msb) ? 0x30 : 0x57;
				lsb -= Character.isDigit(lsb) ? 0x30 : 0x57;
				data[j++] = (byte) ((((byte) msb) << 4) | ((byte) lsb));
			}

			data[38] = 0x72; // Framing Protocol flags and length (high)
			data[39] = 0x58; // 0x258 = 638 - 38
			data[40] = 0x00; // Framing Vector (indicates that the E1.31 framing
								// layer is wrapping a DMP PDU)
			data[41] = 0x00;
			data[42] = 0x00;
			data[43] = 0x02;
			data[44] = 'G'; // Source Name (64 bytes)
			data[45] = 'r';
			data[46] = 'e';
			data[47] = 'g';
			data[48] = 'L';
			data[49] = 'i';
			data[50] = 'g';
			data[51] = 'h';
			data[52] = 't';
			data[53] = 's';
			data[54] = 0x00;
			data[55] = 0x00;
			data[56] = 0x00;
			data[57] = 0x00;
			data[58] = 0x00;
			data[59] = 0x00;
			data[60] = 0x00;
			data[61] = 0x00;
			data[61] = 0x00;
			data[62] = 0x00;
			data[63] = 0x00;
			data[64] = 0x00;
			data[65] = 0x00;
			data[66] = 0x00;
			data[67] = 0x00;
			data[68] = 0x00;
			data[69] = 0x00;
			data[70] = 0x00;
			data[71] = 0x00;
			data[71] = 0x00;
			data[72] = 0x00;
			data[73] = 0x00;
			data[74] = 0x00;
			data[75] = 0x00;
			data[76] = 0x00;
			data[77] = 0x00;
			data[78] = 0x00;
			data[79] = 0x00;
			data[80] = 0x00;
			data[81] = 0x00;
			data[81] = 0x00;
			data[82] = 0x00;
			data[83] = 0x00;
			data[84] = 0x00;
			data[85] = 0x00;
			data[86] = 0x00;
			data[87] = 0x00;
			data[88] = 0x00;
			data[89] = 0x00;
			data[90] = 0x00;
			data[91] = 0x00;
			data[91] = 0x00;
			data[92] = 0x00;
			data[93] = 0x00;
			data[94] = 0x00;
			data[95] = 0x00;
			data[96] = 0x00;
			data[97] = 0x00;
			data[98] = 0x00;
			data[99] = 0x00;
			data[100] = 0x00;
			data[101] = 0x00;
			data[101] = 0x00;
			data[102] = 0x00;
			data[103] = 0x00;
			data[104] = 0x00;
			data[105] = 0x00;
			data[106] = 0x00;
			data[107] = 0x00;
			data[108] = 100; // Priority
			data[109] = 0x00; // Reserved
			data[110] = 0x00; // Reserved
			data[111] = 0x00; // Sequence Number
			data[112] = 0x00; // Framing Options Flags
			data[113] = univHi; // Universe Number (high)
			data[114] = univLo; // Universe Number (low)

			data[115] = 0x72; // DMP Protocol flags and length (high)
			data[116] = 0x0b; // 0x20b = 638 - 115
			data[117] = 0x02; // DMP Vector (Identifies DMP Set Property Message
								// PDU)
			data[118] = (byte) 0xa1; // DMP Address Type & Data Type
			data[119] = 0x00; // First Property Address (high)
			data[120] = 0x00; // First Property Address (low)
			data[121] = 0x00; // Address Increment (high)
			data[122] = 0x01; // Address Increment (low)
			data[123] = 0x02; // Property value count (high)
			data[124] = 0x01; // Property value count (low)
			data[125] = 0x00; // DMX512-A START Code

			int i = E131Example.numchannels + 1;
			byte NumHi = (byte) (i >> 8); // Channels (high)
			byte NumLo = (byte) (i & 0xff); // Channels (low)

			data[123] = NumHi; // Property value count (high)
			data[124] = NumLo; // Property value count (low)

			i = 638 - 16 - (512 - E131Example.numchannels);
			byte hi = (byte) (i >> 8); // (high)
			byte lo = (byte) (i & 0xff); // (low)

			data[16] = (byte) (hi + 0x70); // RLP Protocol flags and length
											// (high)
			data[17] = lo; // 0x26e = 638 - 16

			i = 638 - 38 - (512 - E131Example.numchannels);
			hi = (byte) (i >> 8); // (high)
			lo = (byte) (i & 0xff); // (low)
			data[38] = (byte) (hi + 0x70); // Framing Protocol flags and length
											// (high)
			data[39] = lo; // 0x258 = 638 - 38

			i = 638 - 115 - (512 - E131Example.numchannels);
			hi = (byte) (i >> 8); // (high)
			lo = (byte) (i & 0xff); // (low)
			data[115] = (byte) (hi + 0x70); // DMP Protocol flags and length
											// (high)
			data[116] = lo; // 0x20b = 638 - 115
		}

	}
	
	public static void main(String[] args) throws Exception {
		//new E131Example().sendData();
		new E131Example().loopColors();
	}

}
