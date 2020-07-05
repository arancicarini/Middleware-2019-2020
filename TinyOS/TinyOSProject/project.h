#ifndef PROJECT_H
#define PROJECT_H

#define SETUP 1
#define DATA 2 

typedef nx_struct Msg {
	nx_uint8_t isData;
	nx_uint16_t sender; 
	nx_uint16_t value; //if isData then it contains some data read by the sensor, otherwise it contains the treshold
} Msg_t;

enum{
AM_MY_MSG = 6
};

#endif
