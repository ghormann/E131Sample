#ifndef DDPOUTPUT_H
#define DDPOUTPUT_H

#define DDP_PORT 4048
#define DDP_HEADER_LEN 10

#define DDP_FLAGS1_VER     0xc0   // version mask
#define DDP_FLAGS1_VER1    0x40   // version=1
#define DDP_FLAGS1_PUSH    0x01
#define DDP_FLAGS1_QUERY   0x02
#define DDP_FLAGS1_REPLY   0x04
#define DDP_FLAGS1_STORAGE 0x08
#define DDP_FLAGS1_TIME    0x10

#define DDP_ID_DISPLAY       1
#define DDP_ID_CONFIG      250
#define DDP_ID_STATUS      251

//1440 channels per packet
#define DDP_CHANNELS_PER_PACKET 1440

#define DDP_PACKET_LEN (DDP_HEADER_LEN + DDP_CHANNELS_PER_PACKET)


#include <string>
#include <netinet/in.h>


class DDPOutput
{
private:
    std::string ip_address;
    int maxPixels;
    int maxPackets;
    unsigned char *data;
    struct sockaddr_in myaddr, remoteaddr;
    int fd;
    int lastPacketLength;

public:
    DDPOutput(std::string ip, int numPixels, int startChannel);
    void doUpdate();
    void setPixel(int id, unsigned char r, unsigned char g, unsigned char b);
    ~DDPOutput();
};



#endif