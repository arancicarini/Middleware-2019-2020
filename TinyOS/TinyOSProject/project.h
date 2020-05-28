#ifndef PROJECT_H
#define PROJECT_H

//payload of the msg
typedef nx_struct my_msg {
	nx_uint8_t type;
	nx_uint16_t identifier; #in un caso contiente l'id (data) in un caso il from (setup, per settare la strada)
	nx_uint16_t value; 
} my_msg_t;

#define SETUP 1
#define DATA 2 

enum{
AM_MY_MSG = 6
};

#endif

#oppure due typedef diverse?

typedef nx_struct_setup_msg{
	nx_uint16_t sender;
	nx_uint16_t teshold;
}

typedef nx_struct_data_msg{
	nx_uint16_t identifier;
	nx_uint16_t value;
}

