#ifndef PROJECT_H
#define PROJECT_H

#define SETUP 1
#define DATA 2 

typedef nx_struct setup_msg{
	nx_uint16_t sender;
	nx_uint16_t treshold;
} setup_msg_t;

typedef nx_struct data_msg{
	nx_uint16_t identifier;
	nx_uint16_t value;
} data_msg_t;

enum{
AM_MY_MSG = 6
};

#endif

//oppure una typedef sola??
/*typedef nx_struct my_msg {
	nx_uint8_t type;
	nx_uint16_t identifier; #in un caso contiente l'id (data) in un caso il from (setup, per settare la strada)
	nx_uint16_t value; 
} my_msg_t;
*/
