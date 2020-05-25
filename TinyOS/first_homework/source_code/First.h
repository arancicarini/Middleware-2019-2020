#ifndef FIRST_H
#define FIRST_H

typedef nx_struct radio_id_msg 
{
 	nx_uint16_t sender_id;
 	nx_uint16_t counter;
} radio_id_msg_t;

enum 
{
	AM_RADIO_ID_MSG = 6,
};

#endif
