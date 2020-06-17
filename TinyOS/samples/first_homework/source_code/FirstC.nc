
#include "Timer.h"
#include "First.h"


module FirstC @safe() 
{
	uses 
	{
    	interface Leds;
    	interface Boot;
    	interface Receive;
    	interface AMSend;
    	interface Timer<TMilli> as MilliTimer;
    	interface SplitControl as AMControl;
    	interface Packet;
  	}
}
implementation 
{
	message_t packet;
	uint16_t id;

  	bool locked = FALSE;
  	uint16_t counter = 0;
  	message_t packet;
  
	event void Boot.booted() 
	{
		call AMControl.start();
	}
	
	event void AMControl.startDone(error_t err) 
	{
    	if (err == SUCCESS)
    	{
    		uint16_t interval=0;
    		
    		id = TOS_NODE_ID;
    		
    		switch(id){
    			case 1:
    				interval=1000;
    				break;
    			case 2:
    				interval=333;
    				break;
    			default:
    				interval=200;
    				break;
    		}
      		call MilliTimer.startPeriodic(interval);
		}
    	else
        	call AMControl.start();
    }
    
    event void AMControl.stopDone(error_t err) 
    {
    	// do nothing
  	}
  	
  	event void MilliTimer.fired() 
  	{
    	if (locked) return;
    	else 
    	{
      		radio_id_msg_t* rm = (radio_id_msg_t*)call Packet.getPayload(&packet, sizeof(radio_id_msg_t));
      		if (rm == NULL) return;

      		rm->sender_id = id;
      		rm->counter = counter;
      		if (call AMSend.send(AM_BROADCAST_ADDR, &packet, sizeof(radio_id_msg_t)) == SUCCESS)
				locked = TRUE;
    	}
  	}
  	
  	event message_t* Receive.receive(message_t* bufPtr, void* payload, uint8_t len) 
  	{
  		counter++;

    	if (len != sizeof(radio_id_msg_t))
    		return bufPtr;
    	else
    	{    		
      		radio_id_msg_t* rm = (radio_id_msg_t*)payload;
      		if (rm->counter % 10 == 0)
      		{
      			call Leds.led0Off();
      			call Leds.led1Off();
      			call Leds.led2Off();
      		}
      		switch(rm->sender_id){
      			case 1:
      				call Leds.led0Toggle();
      				break;
      			case 2:
      				call Leds.led1Toggle();
      				break;
      			case 3:
      				call Leds.led2Toggle();
      				break;
      		}
			return bufPtr;
			
		}
	}
      
	event void AMSend.sendDone(message_t* bufPtr, error_t error)
	{
    	if (&packet == bufPtr)
      		locked = FALSE;
  	}
}
  	
  	
