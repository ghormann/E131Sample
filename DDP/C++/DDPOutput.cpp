#include "DDPOutput.h"
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <string.h>
#include <iostream>

DDPOutput::DDPOutput(std::string ip, int numPixels, int startChannel)
{
    this->ip_address = ip;
    this->maxPixels = numPixels;
    this->lastPacketLength = DDP_PACKET_LEN;
    int chan = startChannel - 1;
    this->fd = 0;

    // Canculate max number of Packets
    this->maxPackets = (numPixels * 3) / DDP_CHANNELS_PER_PACKET;
    if ((numPixels * 3) % DDP_CHANNELS_PER_PACKET)
    {
        this->maxPackets++;
    }

    // Allocate data for each packet
    this->data = (unsigned char *)calloc(this->maxPackets * DDP_PACKET_LEN, sizeof(unsigned char));

    // Set header
    for (int i = 0; i < this->maxPackets; i++)
    {
        int pos = i * DDP_PACKET_LEN;

        // byte 0
        /*
        byte 0:     Flags: V V x T S R Q P
 *               V V:    2-bits for protocol version number (01 for this file)
 *               x:      reserved
 *               T:      timecode field added to end of header
 *                       if T & P are set, Push at specified time
 *               S:      Storage.  If set, data from Storage, not data-field.
 *               R:      Reply flag, marks reply to Query packet.
 *                       always set when any packet is sent by a Display.
 *                       if Reply, Q flag is ignored.
 *               Q:      Query flag, requests len data from ID at offset
 *                       (no data sent). if clear, is a Write buffer packet
 *               P:      Push flag, for display synchronization, or marks
 *                       last packet of Reply
        */
        this->data[pos] = DDP_FLAGS1_VER1;

        // byte 1: Sequence number -- Ignore
        this->data[pos + 1] = 0;

        /*
        *   byte  2:    data type: S x t t t b b b
        *         S:      0 = standard types, 1 = custom
        *         x:      reserved for future use (set to zero)
        *       ttt:    type, 0 = greyscale, 1 = rgb, 2 = hsl?
        *       bbb:    bits per pixel:
        *               0=1, 1=4, 2=8, 3=16, 4=24, 5=32, 6=48, 7=64
        */

        this->data[pos + 2] = 1;

        /*
        *   byte  3:    Source or Destination ID
 *               0 = reserved
 *               1 = default output device
 *               2=249 custom IDs, (possibly defined via JSON config) ?????
 *               246 = JSON control (read/write)
 *               250 = JSON config  (read/write)
 *               251 = JSON status  (read only)
 *               254 = DMX transit
 *               255 = all devices
 * */
        this->data[pos + 3] = DDP_ID_DISPLAY;

        int pktSize = DDP_CHANNELS_PER_PACKET;

        /* 
         * Handle Last packet different
         */
        if (i == (maxPackets - 1))
        {
            this->data[pos] = DDP_FLAGS1_VER1 | DDP_FLAGS1_PUSH;
            //last packet
            if ((maxPixels * 3) % DDP_CHANNELS_PER_PACKET)
            {
                pktSize = (maxPixels * 3) % DDP_CHANNELS_PER_PACKET;
                this->lastPacketLength = pktSize + DDP_HEADER_LEN;
            }
        }

        /*
         *   byte  4-7:  data offset in bytes (in units based on data-type.
         *               ie: RGB=3 bytes=1 unit) or bytes??  32-bit number, MSB first
         */
        this->data[pos + 4] = (chan & 0xFF000000) >> 24;
        this->data[pos + 5] = (chan & 0xFF0000) >> 16;
        this->data[pos + 6] = (chan & 0xFF00) >> 8;
        this->data[pos + 7] = (chan & 0xFF);

        /*
         *   byte  8-9:  data length in bytes (size of data field when writing)
         *               16-bit number, MSB first
         *               for Queries, this specifies size of data to read,
         *                   no data field follows header.
         * 
         */
        this->data[pos + 8] = (pktSize & 0xFF00) >> 8;
        this->data[pos + 9] = pktSize & 0xFF;
        chan += pktSize;
    }
    // End setting header

    /*
     * Setup networking
     */

    memset(&myaddr, 0, sizeof(myaddr));
    myaddr.sin_family = AF_INET;
    myaddr.sin_addr.s_addr = htonl(INADDR_ANY);
    myaddr.sin_port = htons(0);

    //Setup reomote Address
    memset((char *)&remoteaddr, 0, sizeof(remoteaddr));
    remoteaddr.sin_family = AF_INET;
    remoteaddr.sin_port = htons(DDP_PORT);
    if (inet_aton(ip_address.c_str(), &remoteaddr.sin_addr) == 0)
    {
        std::cout << "DDPOutput: inet_aton() failed for remote addr" << std::endl;
        throw "inet_aton() failed for remote addr\n";
    }

    if (fd == 0)
    {

        // Setup Socket
        if ((fd = socket(AF_INET, SOCK_DGRAM, 0)) < 0)
        {
            std::cout << "DDPOutput: cannot create socket" << std::endl;
            throw "cannot create socket";
        }

        if (bind(fd, (struct sockaddr *)&myaddr, sizeof(myaddr)) < 0)
        {
            std::cout << "DDPOutput: Bind to Socked failed" << std::endl;
            throw "Bind to Socked failed";
        }
    }
}

void DDPOutput::setPixel(int pixelId, unsigned char r, unsigned char g, unsigned char b) {
    if (pixelId <0 || pixelId >=this->maxPixels) {
        std::cout << "Illegale Pixel ID: " << pixelId << std::endl;
        throw "Illegal Pixel ID";
    }

    /*
     * This is not very efficent, but is good for demo code
     */
    int packetId = pixelId / (DDP_CHANNELS_PER_PACKET/3);
    int id = pixelId % (DDP_CHANNELS_PER_PACKET/3);
    int pos = packetId * DDP_PACKET_LEN + DDP_HEADER_LEN + (id*3);
    data[pos] = r;
    data[pos+1] = g;
    data[pos+2] = b;
}

void DDPOutput::doUpdate() {
    int pkLen = DDP_PACKET_LEN;
    int slen=sizeof(remoteaddr);

    for (int i = 0; i < this->maxPackets; i++)
    {
        if (i == (this->maxPackets -1)) {
            pkLen = this->lastPacketLength;
        }

         if (sendto(fd, (data + (i * DDP_PACKET_LEN)), pkLen, 0, (struct sockaddr *)&remoteaddr, slen) ==-1) {
            std::cout << "DDPOutput: Error during sentdto in DDPOutput" << std::endl;
            throw "Error during sentdto DDPOutput";
		}
    }
}

DDPOutput::~DDPOutput()
{
}
