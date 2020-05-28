#include "project.h"
#include "Timer.h"

module projectMoteC

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
    	//interface PacketAcknowledgements as Ack;
	
		//interface used to perform sensor reading (to get the value from a sensor)
		interface Read<uint16_t>;
  	}

} 
implementation
{


  	message_t packet;
  	bool locked = FALSE;
  	

  	void sendTrashold();
  	void getTreshold();
  

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
  			dbg ("boot", "I'm node : %d, Radio started at time: %s \n", TOS_NODE_ID, sim_time_string());
			call MilliTimer.startPeriodicAt(0, 1000);
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
		sendTreshold();
	}
  

  //********************* AMSend interface ****************//
  	event void AMSend.sendDone(message_t* buf,error_t err)
  	{
  		if( &packet == buf && err== SUCCESS)
  		{
			dbg("radio_send", "packet sent at time %s \n", sim_time_string());
  			locked = FALSE;
  			
		}
		else
		{
			dbgerror("error", "error in sending packet, send the request again\n");
			sendTreashold();

		}  		
  	}
  	


	//***************************** Receive interface *****************//
  	event message_t* Receive.receive(message_t* buf,void* payload, uint8_t len) 
  	{
  		if(len!= sizeof(data_msg_t)) return buf;
  		else
  		{
      		data_msg_t* mess = (data_msg_t*)payload;
	  		
  			if(!locked)
  				locked = TRUE;
			else
				return buf;
			
			data = mess-> data;
	  		from = mess-> identifier;
      		dbg("radio_pack", "Packet from: %hu \n", mess->identifier);
  			dbg("radio_rec", "Packet received at time %s\n", sim_time_string());	
      		return buf;
    	}
 
   	}

	//****************** Task send response *****************//
  	void sendTreshold() 
  	{
		call Read.read();
  	}
  
	//************************* Read interface **********************//
  	event void Read.readDone(error_t result, uint16_t data) 
  	{

  		setup_msg_t* mess = (setup_msg_t*)(call Packet.getPayload(&packet, sizeof(setup_msg_t)));  

		treashold= data;
		dbg("treshold","treshold read done %f\n",treashold);  	
	
		if (mess == NULL) return;

		mess -> replyTo = TOS_NODE_ID;
		mess->treshold = treshold;

		
		if(call AMSend.send(BROADCAST, &packet,sizeof(setup_msg_t)) == SUCCESS)
		{
		   	dbg("radio_send", "Packet passed to lower layer successfully!\n");
		   	dbg("radio_pack","Packet from : %hu, treshold: %hu\n", mess->replyTo, mess->treashold);
		}
		else
			locked = FALSE;
  	}

}



