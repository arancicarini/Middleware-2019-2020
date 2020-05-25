

#include "First.h"

configuration FirstAppC {}
implementation 
{
	components MainC, FirstC as App, LedsC;
  	components new AMSenderC(AM_RADIO_ID_MSG);
  	components new AMReceiverC(AM_RADIO_ID_MSG);
  	components new TimerMilliC();
  	components ActiveMessageC;
  
  	App.Boot -> MainC.Boot;
  
  	App.Receive -> AMReceiverC;
  	App.AMSend -> AMSenderC;
  	App.AMControl -> ActiveMessageC;
  	App.Leds -> LedsC;
  	App.MilliTimer -> TimerMilliC;
  	App.Packet -> AMSenderC;
}
