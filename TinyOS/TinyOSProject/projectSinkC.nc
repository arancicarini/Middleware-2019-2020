#include "project.h"
#include "Timer.h"

module projectSinkC

{
	uses 
	{
	/****** INTERFACES *****/

		interface Boot; 
	
		//interfaces for communication
    	interface SplitControl as AMControl;
    	interface AMSend;
    	interface Packet;
    	interface AMPacket;
    	interface Receive;
    	
		//timer interfaces
		interface Timer<TMilli> as TresholdTimer;
		interface Timer<TMilli> as DataTimer;
		interface Timer<TMilli> as ForwardingTimer;
	
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
  	bool isNewTreshold = FALSE;
  	uint16_t data;
  	uint16_t mote_treshold;

  	void sendData();
  	void readData();
  	void getTreshold();
  	void send_message ( Msg_t* mess);
  

	//***************** Boot event ********************//
  	event void Boot.booted()
  	{
		call AMControl.start();	
  	}

	//***************** SplitControl event********************//
  	event void AMControl.startDone(error_t err)
  	{
  		if(err == SUCCESS)
  		{
  			dbg ("boot", "Booting ... I'm node : %d, Radio started at time: %s \n", TOS_NODE_ID, sim_time_string());
  			if(TOS_NODE_ID ==1){
				call TresholdTimer.startPeriodicAt(0, 1000);
			}
			else{
				mote_treshold = 0;
				call DataTimer.startPeriodicAt(0, 1000);
				call ForwardingTimer.startPeriodicAt(0,100);
			}
			
  		}
  		else
  		{
  			dbgerror("error", "radio didn't start\n");
  			call AMControl.start();
  		}
  
  	}
  
  	event void AMControl.stopDone(error_t err){}


  
  //***************** MilliTimer events ( read a new treshold or a new Data ********************//
  	event void TresholdTimer.fired() 
  	{
	 	dbg_clear("timer", "\n");
		dbg("timer","Treshold timer fired at %s.\n", sim_time_string());
		call Read.read();
	}
  
  	event void DataTimer.fired() 
  	{
	 	dbg_clear("timer", "\n");
		dbg("timer","Data timer fired at %s.\n", sim_time_string());
		call Read.read();
	}
	
	event void ForwardingTimer.fired(){
		dbg_clear("timer", "\n");
		if (isNewTreshold){
			Msg_t* mess = (Msg_t*)(call Packet.getPayload(&packet, sizeof(Msg_t)));   	
			if (mess == NULL) return;
			mess->sender = TOS_NODE_ID;
			mess->value = mote_treshold;
			mess->isData = 0;
			send_message(mess);
			isNewTreshold = FALSE;
			dbg("timer","Treshold has been found changed on mote %hu\n", TOS_NODE_ID );
		}	
	}
	

  
  	


	//***************************** Receive interface *****************//
  	event message_t* Receive.receive(message_t* buf,void* payload, uint8_t len) 
  	{
      	Msg_t* mess = (Msg_t*)payload;
      	//if it's some data sent by the children nodes, we forward it or we print if we are root
		if (mess->sender > TOS_NODE_ID && mess->isData == 1){
			if( TOS_NODE_ID == 1){
				dbg("error", "sink node received data message with data %hu\n", mess->value); 
			}
			else{
				//send_message(mess);
			}
		}
		// if it's the new treshold sent by the parent node, we forward it and we set the new treshold on the current node
		if(mess->sender < TOS_NODE_ID && mess->isData == 0){
			mote_treshold = mess-> value;
      		dbg("radio_rec", "A mote node has received a treshold message with treshold %hu\n", mote_treshold);
      	 	isNewTreshold = TRUE;
      	}
      	
      	  		
    	// in all the other cases the message is simply discarded
    	return buf;
 
   	}

	
	
	  	
  
	//************************* Read interface **********************//
  	event void Read.readDone(error_t result, uint16_t dataRead) 
  	{
		if(TOS_NODE_ID ==1){
  			Msg_t* mess = (Msg_t*)(call Packet.getPayload(&packet, sizeof(Msg_t)));  
			dbg("treshold","Sink just read new treshold %hu\n",dataRead);  	
			if (mess == NULL) return;
			mess->sender = TOS_NODE_ID;
			mess->value = dataRead;
			mess->isData = 0;
			send_message(mess);
		}
		else{		
			if(dataRead > mote_treshold){
  				Msg_t* mess = (Msg_t*)(call Packet.getPayload(&packet, sizeof(Msg_t)));  
				dbg("error","A mote has just read a new value above treshold: %hu\n",dataRead);  	
	
				if (mess == NULL) return;
				mess->sender = TOS_NODE_ID;
				mess->value = dataRead;
				mess->isData = 1;
				send_message(mess);
		
				
	  		}
		}
  	}
  	
  	//*********************Send interface ****************//
  	event void AMSend.sendDone(message_t* buf,error_t err)
  	{
  		if( &packet == buf)
  		{   
  			locked = FALSE;
  			if ( err == SUCCESS)
  			{
				dbg("radio_send", "packet sent at time %s from %hu \n", sim_time_string(), TOS_NODE_ID);
  			}
			else
			{
				dbgerror("error", "error in sending packet, send the request again\n");

			} 
		} 		
  	}
  	
  	//*********************Support functions to avoid repetition of code****************//
  	void send_message ( Msg_t* mess){
  		if(call AMSend.send(AM_BROADCAST_ADDR, &packet,sizeof(Msg_t)) == SUCCESS){
	   		dbg("treshold","Packet sent from mote: %hu, data: %hu, type: %hu\n ", TOS_NODE_ID, mess->value, mess->isData);
	   		locked = TRUE;
		}
  	}
  	
  	
  	
  	

}



