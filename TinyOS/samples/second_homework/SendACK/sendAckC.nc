/**
 *  Source file for implementation of module sendAckC in which
 *  the node 1 send a request to node 2 until it receives a response.
 *  The reply message contains a reading from the Fake Sensor.
 *
 *  @author Luca Pietro Borsani
 */

#include "sendAck.h"
#include "Timer.h"

module sendAckC
{
	uses 
	{
	/****** INTERFACES *****/
		interface Boot; 
	
    //interfaces for communication
    	interface SplitControl;
    	interface AMSend;
    	interface Packet;
    	interface AMPacket;
    	interface Receive;
    
	//interface for timer
		interface Timer<TMilli> as MilliTimer;
	
    //other interfaces, if needed
    	interface PacketAcknowledgements as Ack;
	
	//interface used to perform sensor reading (to get the value from a sensor)
		interface Read<uint16_t>;
  	}

} 
implementation
{

	uint8_t counter=0;
  	uint8_t rec_counter;
  	message_t packet;
  	bool locked = FALSE;

  	void sendReq();
  	void sendResp();
  
  
  	//***************** Send request function ********************//
  	void sendReq()
  	{
		my_msg_t* mess = (my_msg_t*)(call Packet.getPayload(&packet, sizeof(my_msg_t)));
		counter ++;
		if (mess == NULL) return;

		//dbg("radio_pack","Preparing the message... \n");	  
		mess->type = REQ;
		mess->counter = counter;

		//dbg("radio_ack","Setting the ack... \n");	  
		call Ack.requestAck(&packet);

		if(call AMSend.send(2, &packet,sizeof(my_msg_t)) == SUCCESS)
		{
   			//dbg("radio_send", "Packet passed to lower layer successfully!\n");
   			dbg("radio_pack", "Packet <>REQUEST<> SENT counter: %hu \n", mess->counter);
   		}
   		else
   			locked = FALSE;
	}        

  //****************** Task send response *****************//
  	void sendResp() 
  	{
		call Read.read();
  	}

  //***************** Boot interface ********************//
  	event void Boot.booted()
  	{
		dbg("boot","Application booted at time %s \n", sim_time_string());
		call SplitControl.start();	
  	}

  //***************** SplitControl interface ********************//
  	event void SplitControl.startDone(error_t err)
  	{
  		if(err == SUCCESS)
  		{
  			dbg ("role", "I'm node : %d, Radio started at time: %s \n", TOS_NODE_ID, sim_time_string());
  			if (TOS_NODE_ID == 1)
  			{
  				call MilliTimer.startPeriodicAt(0, 1000);
  			}
  			if(TOS_NODE_ID == 2)
  			{
  				//call MilliTimer.startPeriodicAt(5, 1000);
  			}
  		}
  		else
  		{
  			dbgerror("error", "radio didn't start\n");
  			call SplitControl.start();
  		}
  
  	}
  
  	event void SplitControl.stopDone(error_t err){}

  //***************** MilliTimer interface ********************//
  	event void MilliTimer.fired() 
  	{
	 	dbg_clear("timer", "\n");
		dbg("timer","timer fired at %s.\n", sim_time_string());
		sendReq();
	}
  

  //********************* AMSend interface ****************//
  	event void AMSend.sendDone(message_t* buf,error_t err)
  	{
  		if( &packet == buf && err== SUCCESS)
  		{
			dbg("radio_send", "packet sent at time %s \n", sim_time_string());
      	
      		if(call Ack.wasAcked(buf))
      		{
      			dbg("radio_ack", "message acked! \n");
      			if(TOS_NODE_ID == 1)
      				call MilliTimer.stop();   
  				if(TOS_NODE_ID == 2)
  					dbg_clear("timer", "Routine finished!\n");
  			}
  			else
  				dbg("radio_ack", "message NOT acked! \n");
  			locked = FALSE;
  			
		}
		else
		{
			dbgerror("error", "error in sending packet, send the request again\n");
			if(TOS_NODE_ID == 1) sendReq();
			if(TOS_NODE_ID == 2) sendResp();
		}  		
  	}
  	


  //***************************** Receive interface *****************//
  	event message_t* Receive.receive(message_t* buf,void* payload, uint8_t len) 
  	{
  		if(len!= sizeof(my_msg_t)) return buf;
  		else
  		{
      		my_msg_t* mess = (my_msg_t*)payload;
	  		
  			if(!locked)
  				locked = TRUE;
			else
				return buf;
	  		
	  		if(mess -> type == REQ && TOS_NODE_ID == 2)
	  		{
	  			rec_counter = mess-> counter;
	  			sendResp();
      			dbg("radio_pack", "Packet <>REQUEST<> RECV counter: %hu \n", mess->counter);
      			dbg("radio_rec", "Packet received at time %s\n", sim_time_string());	
	  		}
	  		if(mess -> type == RESP && TOS_NODE_ID == 1)
	  		{
      			dbg("radio_pack", "Packet <>RESPONSE<> RECV counter: %hu, value: %hu \n", mess->counter, mess->value);
	  			dbg("radio_rec", "Packet received at time %s\n", sim_time_string());	
	  		}
     
      		return buf;
    	}
 
   	}

  
  //************************* Read interface **********************//
  	event void Read.readDone(error_t result, uint16_t data) 
  	{

  		my_msg_t* mess = (my_msg_t*)(call Packet.getPayload(&packet, sizeof(my_msg_t)));  

		double value = ((double)data/65535)*100;
		dbg("value","value read done %f\n",value);  	
	
		if (mess == NULL) return;
		//dbg("radio_pack","Preparing the message... \n");	  
		mess->type = RESP;
		mess -> counter = rec_counter ;
		mess->value = value;

		//dbg("radio_ack","Setting the ack... \n");	  
		call Ack.requestAck(&packet);
		
		if(call AMSend.send(1, &packet,sizeof(my_msg_t)) == SUCCESS)
		{
		   	dbg("radio_send", "Packet passed to lower layer successfully!\n");
		   	dbg("radio_pack","Packet <>RESPONSE<> SENT counter: %hu, value: %hu\n", mess->counter, mess->value);
		}
		else
			locked = FALSE;
  	}

}



