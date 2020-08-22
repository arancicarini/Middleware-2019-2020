#ifndef PROJECT_H
#define PROJECT_H

#define SETUP 1
#define DATA 2 

typedef nx_struct Msg {
	nx_uint16_t type; // 1 == SETUP MESSAGE; 2 == DATA MESSAGE; all other type values do not have any meaning and the message is discarded
	nx_uint16_t sender; 
	nx_uint16_t source; // the Id of the mote who created this message ( always 1 for SETUP messages)
	nx_uint32_t value; //if type == 1 (SETUP MESSAGE) this field contains the new treshold, if 2( DATA MESSAGE) it contains a new value read by a sensor
	nx_uint32_t time; //to evaluate performances
} Msg_t;

enum{
AM_MY_MSG = 6
};

#endif
