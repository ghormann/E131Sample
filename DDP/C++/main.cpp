#include <iostream>
#include "DDPOutput.h"
#include <chrono>
#include <thread>

#define GRID_ROW 96
#define GRID_HEIGHT 32
#define GRID_PIXELS (GRID_ROW * GRID_HEIGHT)

int main(int argc, char const *argv[])
{
    std::cout << "Booting..." << std::endl;

    DDPOutput output("192.168.1.148", GRID_PIXELS, 1);

    unsigned char r = 0;
    unsigned char g = 0;
    unsigned char b = 0;

    while (true)
    {
        int i = 0;
        for (int x = 0; x < GRID_HEIGHT; x++)
        {
            for (int y = 0; y < GRID_ROW; y++)
            {
                output.setPixel(i, r, g, b);
                i++;
            }
            r += 5;
            if (r > 240)
            {
                r = 0;
                g += 5;
            }
            if (g > 240)
            {
                g = 0;
            }
        }
        b += 5;
        if (b > 240)
        {
            b = 0;
        }
        output.doUpdate();

        std::this_thread::sleep_for(std::chrono::milliseconds(250));
    }

    /* code */
    return 0;
}
