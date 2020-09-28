# E1.31 and DDP Examples
These are some basic examples of how to send E1.31 data and DDP in different languages to pixel controllers like the [FPP](https://github.com/FalconChristmas/fpp), Falcon F16v3, or AlphaPix4. Please note that: 
1. most controllers expect you send a message at lease once every few seconds
1. some E1.31 controllers have a maximum number of packets that can be buffered before a pause is needed.   (The old Alphapix4 and AlphaPix16s will drop packets if you don't pause for at least 2ms after sending around 20 Universis.) 
