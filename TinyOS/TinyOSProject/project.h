#ifndef PROJECT_H
#define PROJECT_H

#define SETUP 1
#define DATA 2 

typedef nx_struct Setup_msg{
	nx_uint16_t sender;
	nx_uint16_t treshold;
} Setup_msg;

typedef nx_struct Data_msg{
	nx_uint16_t sender;
	nx_uint16_t value;
} Data_msg;

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
