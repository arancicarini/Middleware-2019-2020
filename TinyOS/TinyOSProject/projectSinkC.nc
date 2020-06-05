#include "project.h"
#include "Timer.h"

module projectSinkC

{
	uses 
	{
	/****** INTERFACES *****/

		interface Boot; 
	
		//interfaces for communication
    	interface SplitControl;
    	//interface AMSend;
    	//interface Packet;
    	//interface AMPacket;
    	interface Receive;
    	interface StdControl as DisseminationControl;
  		interface DisseminationValue<uint16_t> as Value;
  		interface DisseminationUpdate<uint16_t> as Update;
  		interface StdControl as RoutingControl;
    	interface RootControl;
    	interface Send;
    	
		//interface for timer
		interface Timer<TMilli> as TresholdTimer;
		interface Timer<TMilli> as DataTimer;
	
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
  	uint16_t data;
  	uint16_t from;
  	uint16_t sink_treshold;
  	uint16_t mote_treshold;
  	uint16_t replyTo;

  	void sendData();
  	void readData();
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
  			call RoutingControl.start();
  			call DisseminationControl.start();
  			if(TOS_NODE_ID ==1){
  				sink_treshold = 0;
				call TresholdTimer.startPeriodicAt(0, 1000);
				call RootControl.setRoot();
			}
			else{
				mote_treshold = 0;
				call DataTimer.startPeriodicAt(0, 1000);
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
  	event void TresholdTimer.fired() 
  	{
	 	dbg_clear("timer", "\n");
		dbg("timer","timer fired at %s.\n", sim_time_string());
		call Read.read();
	}
  
  	event void DataTimer.fired() 
  	{
	 	dbg_clear("timer", "\n");
		dbg("timer","timer fired at %s.\n", sim_time_string());
		call Read.read();
	}
	

  //*********************Send interface ****************//
  	event void Send.sendDone(message_t* buf,error_t err)
  	{
  		if( &packet == buf && err== SUCCESS)
  		{
			dbg("radio_send", "packet sent at time %s \n", sim_time_string());
  			locked = FALSE;
  			
		}
		else
		{
			dbgerror("error", "error in sending packet, send the request again\n");

		}  		
  	}
  	


	//***************************** Receive interface *****************//
  	event message_t* Receive.receive(message_t* buf,void* payload, uint8_t len) 
  	{
    	dbg("error", "Stiamo ricevendo\n");
  		if(TOS_NODE_ID ==1){
  		    dbg("error", "il Sink riceveo\n");
  			//if(len!= sizeof(data_msg_t)) return buf;
  			//else
  			//{
      		data_msg_t* mess = (data_msg_t*)payload;
  				/*if(!locked)
  					locked = TRUE;
				else
					return buf;*/
			data = mess-> value;
		  	//from = buf-> identifier;
    	  	dbg("radio_pack", "Packet with data %hu\n", data);
  				//dbg("radio_rec", "Packet received at time %s\n", sim_time_string());	
    	  		return buf;
    		//}
    		
    	}
    	else{
    		//if(len!= sizeof(setup_msg_t)) return buf;
  			//else
  			{
      			setup_msg_t* mess = (setup_msg_t*)payload;
	  			
  				if(!locked)
  					locked = TRUE;
				else
					return buf;
	  			
	  			mote_treshold = mess-> treshold;
	  			replyTo = mess -> sender;
	  			dbg("radio_pack", "Packet with new treshold: %hu \n", mess->treshold);
      			//dbg("radio_rec", "Packet received at time %s\n", sim_time_string());
      			return buf;
    		} 
    	}
 
   	}

	//****************** Dissemination*****************//
	
	  	
  	event void Value.changed() {
    	const uint16_t* newVal = call Value.get();
    	mote_treshold = *newVal;
    	dbg("error", "Mote %d, received new treshold %hu\n",TOS_NODE_ID ,mote_treshold);
  	}
  
	//************************* Read interface **********************//
  	event void Read.readDone(error_t result, uint16_t data) 
  	{
		if(TOS_NODE_ID ==1){
			
			sink_treshold=data;
			call Update.change(&sink_treshold);
			dbg("error", "Mote %d, read new treshold %hu\n",TOS_NODE_ID ,sink_treshold);

  			/*setup_msg_t* mess = (setup_msg_t*)(call Packet.getPayload(&packet, sizeof(setup_msg_t)));  
	
			sink_treshold=data;
			dbg("treshold","treshold read done %f\n",sink_treshold);  	
	
			if (mess == NULL) return;

			mess->sender = TOS_NODE_ID;
			mess->treshold = sink_treshold;

		
			if(call AMSend.send(AM_BROADCAST_ADDR, &packet,sizeof(setup_msg_t)) == SUCCESS)
			{
			   	dbg("radio_send", "Packet passed to lower layer successfully!\n");
			   	dbg("radio_pack","Packet from : %hu, treshold: %hu\n", mess->sender, mess->treshold);
			}
			else
				locked = FALSE;*/
		}
		else{
			  		
			//if(data > mote_treshold){
  				data_msg_t* mess = (data_msg_t*)(call Send.getPayload(&packet, sizeof(data_msg_t)));  
				dbg("data","data read done %f\n",data);  	
	
				if (mess == NULL) return;
				mess->identifier = TOS_NODE_ID;
				mess->value = data;
		
				if(call Send.send(&packet,sizeof(data_msg_t)) == SUCCESS)
				{
			   		dbg("radio_send", "Packet passed to lower layer successfully!\n");
			   		dbg("radio_pack","Packet sent from identifier: %hu, data: %hu\n", mess->identifier, mess->value);
				}
				else
					locked = FALSE;
	  		//}
		}
  	}
  	
  	
  	

}



