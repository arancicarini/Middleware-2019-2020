#ifndef PROJECT_H
#define PROJECT_H

#define SETUP 1
#define DATA 2 

typedef nx_struct Msg {
	nx_uint16_t type;
	nx_uint16_t sender; 
	nx_uint16_t source; 
	nx_uint16_t value; //if SETUP (1) contains treshold, if DATA (2) contains data
} Msg_t;

enum{
AM_MY_MSG = 6
};

#endif
